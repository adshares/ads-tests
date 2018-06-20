package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NodeStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Private key generated from "a" pass-phrase
     */
    private static final String PRIVATE_KEY = "CA978112CA1BBDCAFAC231B39A23DC4DA786EFF8147C4E72B9807785AFEE48BB";
    /**
     * Public key generated from "a" pass-phrase
     */
    private static final String PUBLIC_KEY = "EAE1C8793B5597C4B3F490E76AC31172C439690F8EE14142BB851A61F9A49F0E";

    private UserData userData;
    private UserData createdUserData;
    private LogEventTimestamp lastEventTimestamp;

    @Given("^user, who wants to create node$")
    public void user_who_wants_to_create_account() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
        lastEventTimestamp = FunctionCaller.getInstance().getLastEventTimestamp(userData).incrementEventNum();
    }

    @When("^user creates node$")
    public void user_creates_account() {
        final FunctionCaller fc = FunctionCaller.getInstance();
        String resp = fc.createNode(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String reason;
        // check, if accepted
        reason = new AssertReason.Builder().msg("Create node transaction was not accepted by node.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(o));


        //check fee
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        reason = new AssertReason.Builder().msg("Invalid fee computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, fee, comparesEqualTo(EscConst.CREATE_BANK_FEE));

        // check deduct
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        BigDecimal expectedDeduct = EscConst.CREATE_BANK_FEE.add(EscConst.BANK_MIN_UMASS);
        reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, deduct, comparesEqualTo(expectedDeduct));

        int node = getCreatedNodeOrdinalNumberFromLog();
        assertThat("Incorrect node = " + node, node > 0);
        // convert dec node id to hex
        String address = String.format("%04X", node) + "-00000000-XXXX";
        log.debug("Address in created node{}: {}", node, address);

        assertThat("Created address is not valid: " + address, EscUtils.isValidAccountAddress(address));
        createdUserData = UserDataProvider.getInstance().cloneUser(userData, address);

        fc.addNodePrivateKey(node, userData.getSecret());
        fc.startNode(node);
    }

    @Then("^node is created$")
    public void node_is_created() {
        log.debug("node_is_created: start");

        EscUtils.waitForNextBlock();
        String nodeId = createdUserData.getNodeId();

        // Node needs to synchronize with network, therefore there are few attempts.

        int msid = 0;
        int attempt = 0;
        int attemptMax = 100;
        while (attempt++ < attemptMax) {
            String resp = FunctionCaller.getInstance().getBlock(UserDataProvider.getInstance().getUserDataList(1).get(0));
            JsonObject o = Utils.convertStringToJsonObject(resp);
            JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
            for (JsonElement je : arr) {
                JsonObject nodeEntry = je.getAsJsonObject();
                if (nodeId.equals(nodeEntry.get("id").getAsString())) {
                    msid = nodeEntry.get("msid").getAsInt();
                    log.debug("msid: {}", msid);
                    log.debug("attempts: {}", attempt);
                    break;
                }
            }

            if (msid != 0) {
                break;
            }

            String reason = new AssertReason.Builder().msg("Node didn't start.")
                    .req(FunctionCaller.getInstance().getLastRequest()).res(resp).build();
            assertThat(reason, attempt < attemptMax);
            EscUtils.waitForNextBlock();
        }
        log.debug("node_is_created: end");

    }

    private int getCreatedNodeOrdinalNumberFromLog() {
        final FunctionCaller fc = FunctionCaller.getInstance();
        int nodeId = -1;

        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "create_node");
        lf.addFilter("type_no", "32775");

        int attempt = 0;
        int attemptMax = 3;
        while (attempt++ < attemptMax) {
            EscUtils.waitForNextBlock();

            String resp = fc.getLog(userData, lastEventTimestamp);
            LogChecker lc = new LogChecker(resp);
            JsonArray arr = lc.getFilteredLogArray(lf);
            if (arr.size() > 0) {
                JsonObject accCrObj = arr.get(arr.size() - 1).getAsJsonObject();
                final String requestStatus = accCrObj.get("request").getAsString();
                assertThat("Create node request was not accepted.", requestStatus, equalTo("accepted"));
                nodeId = accCrObj.get("node").getAsInt();
                break;
            }

            String reason = new AssertReason.Builder().msg("Cannot get node ordinal after delay.")
                    .req(fc.getLastRequest()).res(resp)
                    .msg("Full log:").msg(fc.getLog(userData)).build();
            assertThat(reason, attempt < attemptMax);
        }

        return nodeId;
    }

    @Given("^main user, who wants to change own node key$")
    public void user_who_change_own_node_key() {
        userData = null;
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();
        for (UserData u : userDataList) {
            if (u.isMainAccount()) {
                userData = u;
                break;
            }
        }
        assertThat("Cannot find user, who will change node key.", userData, notNullValue());
    }

    @When("^user changes own node key$")
    public void user_changes_own_node_key() {
        FunctionCaller fc = FunctionCaller.getInstance();

        // add private key to key.txt file, if it is not present
        int node = Integer.valueOf(userData.getNodeId(), 16);
        fc.addNodePrivateKey(node, PRIVATE_KEY);

        // change node key
        String resp = fc.changeNodeKey(userData, PUBLIC_KEY);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String reason;
        // check result
        String result = o.has("result") ? o.get("result").getAsString() : "null";
        reason = new AssertReason.Builder().msg("Node key was not changed.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, result, equalTo(EscConst.Result.NODE_KEY_CHANGED));
        //check fee
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        reason = new AssertReason.Builder().msg("Invalid fee computed.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, fee, comparesEqualTo(EscConst.CHANGE_BANK_KEY_FEE));
        // check deduct - expected deduct is equal to expected fee
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, deduct, comparesEqualTo(EscConst.CHANGE_BANK_KEY_FEE));
    }

    @Then("^node key is changed$")
    public void node_key_is_changed() {
        EscUtils.waitForNextBlock();
        EscUtils.waitForNextBlock();
        BigDecimal balance = FunctionCaller.getInstance().getUserAccountBalance(userData);
        log.debug("Balance {}", balance.toPlainString());
    }
}
