package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

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
        String resp = FunctionCaller.getInstance().createNode(userData);

        Assert.assertTrue("Create node transaction was not accepted by node",
                EscUtils.isTransactionAcceptedByNode(resp));

        JsonObject o = Utils.convertStringToJsonObject(resp);
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        Assert.assertEquals("Invalid fee computed", 0, EscConst.CREATE_BANK_FEE.compareTo(fee));
        // expected deduct is equal to expected fee
        BigDecimal expectedDeduct = EscConst.CREATE_BANK_FEE.add(EscConst.BANK_MIN_UMASS);
        Assert.assertEquals("Invalid deduct computed", 0, expectedDeduct.compareTo(deduct));


        int nodeId = getCreatedNodeIdFromLog();
        Assert.assertTrue("Incorrect node id", nodeId > 0);
        // convert dec node id to hex
        String address = String.format("%04X", nodeId) + "-00000000-XXXX";
        log.info("Address in created node{}: {}", nodeId, address);

        Assert.assertTrue("Created address is not valid", EscUtils.isValidAccountAddress(address));
        createdUserData = UserDataProvider.getInstance().cloneUser(userData, address);
    }

    @Then("^node is created$")
    public void node_is_created() {
        //TODO rewrite function, below is code copied from step defs for account
        // Sometimes node is created with delay,
        // therefore there are next attempts.
//        int attempt = 0;
//        int attemptMax = 3;
//        while (attempt++ < attemptMax) {
//            String resp = FunctionCaller.getInstance().getMe(createdUserData);
//            JsonObject o = Utils.convertStringToJsonObject(resp);
//
//            if (o.has("error")) {
//                String errorDesc = o.get("error").getAsString();
//                log.info("Error occurred: {}", errorDesc);
//                Assert.assertEquals("Unexpected error after account creation.",
//                        EscConst.Error.GET_GLOBAL_USER_FAILED, errorDesc);
//            } else {
//                BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
//                log.info("Balance {}", balance.toPlainString());
//                break;
//            }
//
//            Assert.assertTrue("Cannot get account info after delay", attempt < attemptMax);
//            EscUtils.waitForNextBlock();
//        }
        throw new PendingException();
    }

    private int getCreatedNodeIdFromLog() {
        int nodeId = -1;

        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "create_node");
        lf.addFilter("type_no", "32775");

        int attempt = 0;
        int attemptMax = 3;
        while (attempt++ < attemptMax) {
            EscUtils.waitForNextBlock();

            LogChecker lc = new LogChecker(FunctionCaller.getInstance().getLog(userData, lastEventTimestamp));
            JsonArray arr = lc.getFilteredLogArray(lf);
            if (arr.size() > 0) {
                JsonObject accCrObj = arr.get(arr.size() - 1).getAsJsonObject();
                Assert.assertEquals("Request was not accepted.", "accepted", accCrObj.get("request").getAsString());
                nodeId = accCrObj.get("node").getAsInt();
                break;
            }

            Assert.assertTrue("Cannot get node id after delay", attempt < attemptMax);
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
        Assert.assertNotNull("Cannot find user, who will change node key", userData);
    }

    @When("^user changes own node key$")
    public void user_changes_own_node_key() {
        FunctionCaller fc = FunctionCaller.getInstance();

        // add private key to key.txt file, if it is not present
        int nodeId = Integer.valueOf(userData.getAddress().substring(0, 4), 16);
        String keyFileName = String.format("/ads-data/node%d/key/key.txt", nodeId);
        String keyFileContent = fc.callFunction("docker exec -i adshares_ads_1 cat " + keyFileName);
        if (!keyFileContent.contains(PRIVATE_KEY)) {
            fc.callFunction("docker exec -i adshares_ads_1 sh -c \"echo '" + PRIVATE_KEY + "' >> " + keyFileName + "\"");
        }

        // change node key
        String resp = fc.changeNodeKey(userData, PUBLIC_KEY);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        Assert.assertEquals("Node key was not changed",
                EscConst.Result.NODE_KEY_CHANGED, o.get("result").getAsString());

        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        Assert.assertEquals("Invalid fee computed", 0, EscConst.CHANGE_BANK_KEY_FEE.compareTo(fee));
        // expected deduct is equal to expected fee
        Assert.assertEquals("Invalid deduct computed", 0, EscConst.CHANGE_BANK_KEY_FEE.compareTo(deduct));

    }

    @Then("^node key is changed$")
    public void node_key_is_changed() {
        EscUtils.waitForNextBlock();
        BigDecimal balance = FunctionCaller.getInstance().getUserAccountBalance(userData);
        log.info("Balance {}", balance.toPlainString());
    }
}
