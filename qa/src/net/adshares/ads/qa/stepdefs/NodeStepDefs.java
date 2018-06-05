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

public class NodeStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

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
        Assert.assertEquals("Invalid deduct computed", 0, EscConst.CREATE_BANK_FEE.compareTo(deduct));


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
                Assert.assertEquals("Request was not accepted.","accepted", accCrObj.get("request").getAsString());
                nodeId = accCrObj.get("node").getAsInt();
                break;
            }

            Assert.assertTrue("Cannot get node id after delay", attempt < attemptMax);
        }

        return nodeId;
    }
}
