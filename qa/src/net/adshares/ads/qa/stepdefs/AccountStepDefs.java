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
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class AccountStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Private key generated from "a" pass-phrase
     */
    private static final String PRIVATE_KEY = "CA978112CA1BBDCAFAC231B39A23DC4DA786EFF8147C4E72B9807785AFEE48BB";
    /**
     * Public key generated from "a" pass-phrase
     */
    private static final String PUBLIC_KEY = "EAE1C8793B5597C4B3F490E76AC31172C439690F8EE14142BB851A61F9A49F0E";
    /**
     * Empty string signed with private key generated from "a" pass-phrase
     */
    private static final String SIGNATURE = "1F0571D30661FB1D50BE0D61A0A0E97BAEFF8C030CD0269ADE49438A4AD4CF897367E21B100C694F220D922200B3AB852A377D8857A64C36CB1569311760F303";

    private UserData userData;
    private UserData createdUserData;
    private LogEventTimestamp lastEventTimestamp;

    @Given("^user, who wants to change key$")
    public void user_who_wants_to_change_key() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
        lastEventTimestamp = FunctionCaller.getInstance().getLastEventTimestamp(userData).incrementEventNum();
    }

    @Given("^user, who wants to create account$")
    public void user_who_wants_to_create_account() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
        lastEventTimestamp = FunctionCaller.getInstance().getLastEventTimestamp(userData).incrementEventNum();
    }

    @When("^user changes key$")
    public void user_changes_key() {
        String resp = FunctionCaller.getInstance().changeAccountKey(userData, PUBLIC_KEY, SIGNATURE);

        Assert.assertTrue("Change key transaction was not accepted by node",
                EscUtils.isTransactionAcceptedByNode(resp));

        JsonObject o = Utils.convertStringToJsonObject(resp);
        Assert.assertTrue("Incorrect 'result' field",
                o.has("result") && "PKEY changed".equals(o.get("result").getAsString()));

        // in case of change key: deduct == fee
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        Assert.assertEquals("Invalid deduct computed", 0, deduct.compareTo(fee));
        Assert.assertEquals("Invalid fee computed", 0, EscConst.CHANGE_ACCOUNT_KEY_FEE.compareTo(fee));
    }

    @Then("^he cannot use old key$")
    public void he_cannot_use_old_key() {
        String resp = FunctionCaller.getInstance().getMe(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        Assert.assertTrue(o.has("error")
                && "Wrong signature".equals(o.get("error").getAsString()));
    }

    @Then("^transaction can be authorised with new key$")
    public void transaction_can_be_authorised_with_new_key() {
        userData.setSecret(PRIVATE_KEY);
        String resp = FunctionCaller.getInstance().getMe(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        Assert.assertFalse(o.has("error"));
        BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
        log.info("Balance {}", balance.toPlainString());
    }

    @Then("^account change key transaction is present in log$")
    public void account_change_key_transaction_is_present_in_log() {
        LogChecker lc = new LogChecker(FunctionCaller.getInstance().getLog(userData, lastEventTimestamp));
        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "change_account_key");
        BigDecimal feeFromLog = lc.getBalanceFromLogArray(lf);
        // fee in log is negative, but in doc is positive
        // change sign of fee in log
        feeFromLog = BigDecimal.ZERO.subtract(feeFromLog);
        log.info("fees in log: {}", feeFromLog.toPlainString());
        log.info("fees in doc: {}", EscConst.CHANGE_ACCOUNT_KEY_FEE.toPlainString());
        log.info("diff:        {}", feeFromLog.subtract(EscConst.CHANGE_ACCOUNT_KEY_FEE).toPlainString());

        Assert.assertEquals("Invalid deduct computed",
                0, EscConst.CHANGE_ACCOUNT_KEY_FEE.compareTo(feeFromLog));
    }

    @When("^user creates( remote)? account$")
    public void user_creates_account(String accountType) {
        boolean isRemote = " remote".equals(accountType);
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        if (isRemote) {
            String userNode = userData.getAddress().substring(0, 4);
            String node = getDifferentNodeId(userData, userNode);
            Assert.assertNotNull("Not able to find different node", node);
            // Function create_account takes node parameter in decimal format
            node = Integer.valueOf(node, 16).toString();
            resp = requestRemoteAccountCreation(userData, node);
        } else {
            resp = fc.createAccount(userData);
        }

        Assert.assertTrue("Create account transaction was not accepted by node",
                EscUtils.isTransactionAcceptedByNode(resp));

        JsonObject o = Utils.convertStringToJsonObject(resp);
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        BigDecimal expectedFee = EscConst.CREATE_ACCOUNT_LOCAL_FEE;
        if (isRemote) {
            // remote transaction has additional fee for remote node
            expectedFee = expectedFee.add(EscConst.CREATE_ACCOUNT_REMOTE_FEE);
        }
        Assert.assertEquals("Invalid fee computed", 0, expectedFee.compareTo(fee));
        BigDecimal expectedDeduct = expectedFee.add(EscConst.USER_MIN_MASS);
        Assert.assertEquals("Invalid deduct computed", 0, expectedDeduct.compareTo(deduct));


        String address;
        if (isRemote) {
            address = getCreatedAccountAddressFromLog();
        } else {
            Assert.assertTrue("Missing \"new_account\" object in response.", o.has("new_account"));
            address = o.getAsJsonObject("new_account").get("address").getAsString();
        }
        Assert.assertTrue("Created address is not valid", EscUtils.isValidAccountAddress(address));
        createdUserData = UserDataProvider.getInstance().cloneUser(userData, address);
    }

    @Then("^account is created$")
    public void account_is_created() {

        // Sometimes account is created with delay,
        // therefore there are next attempts.
        int attempt = 0;
        int attemptMax = 3;
        while (attempt++ < attemptMax) {
            String resp = FunctionCaller.getInstance().getMe(createdUserData);
            JsonObject o = Utils.convertStringToJsonObject(resp);

            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.info("Error occurred: {}", errorDesc);
                Assert.assertEquals("Unexpected error after account creation.",
                        EscConst.Error.GET_GLOBAL_USER_FAILED, errorDesc);
            } else {
                BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
                log.info("Balance {}", balance.toPlainString());
                break;
            }

            Assert.assertTrue("Cannot get account info after delay", attempt < attemptMax);
            EscUtils.waitForNextBlock();
        }
    }

    /**
     * Requests account creation few times with delay.
     *
     * @param userData user data
     * @param node node in which account should be created
     * @return response: json when request was correct, empty otherwise
     */
    private String requestRemoteAccountCreation(UserData userData, String node) {
        String resp = "";
        int attempt = 0;
        int attemptMax = 6;
        while (attempt++ < attemptMax) {

            resp = FunctionCaller.getInstance().createAccount(userData, node);
            JsonObject o = Utils.convertStringToJsonObject(resp);

            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.info("Error occurred: {}", errorDesc);
                Assert.assertEquals("Unexpected error after account creation.",
                        EscConst.Error.CREATE_ACCOUNT_BAD_TIMING, errorDesc);
            } else {
                break;
            }

            Assert.assertTrue("Cannot create remote account.", attempt < attemptMax);
            // remote account creation cannot be requested in first 8 seconds of block,
            // therefore there is delay
            try {
                Thread.sleep(4000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return resp;
    }

    private String getCreatedAccountAddressFromLog() {
        String address = null;

        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "account_created");

        int attempt = 0;
        int attemptMax = 3;
        while (attempt++ < attemptMax) {
            EscUtils.waitForNextBlock();

            LogChecker lc = new LogChecker(FunctionCaller.getInstance().getLog(userData, lastEventTimestamp));
            JsonArray arr = lc.getFilteredLogArray(lf);
            if (arr.size() > 0) {
                JsonObject accCrObj = arr.get(arr.size() - 1).getAsJsonObject();
                Assert.assertEquals("Request was not accepted.","accepted", accCrObj.get("request").getAsString());
                address = accCrObj.get("address").getAsString();
                break;
            }

            Assert.assertTrue("Cannot get account address after delay", attempt < attemptMax);
        }

        return address;
    }

    private String getDifferentNodeId(UserData userData, String nodeId) {
        // Sometimes account is created with delay,
        // therefore there are next attempts.
        int attempt = 0;
        int attemptMax = 4;
        while (attempt++ < attemptMax) {
            String resp = FunctionCaller.getInstance().getBlock(userData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.info("Error occurred: {}", errorDesc);
                Assert.assertEquals("Unexpected error after account creation.",
                        EscConst.Error.GET_BLOCK_INFO_FAILED, errorDesc);
            } else {
                JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
                for (JsonElement je : arr) {
                    String tmpNodeId = je.getAsJsonObject().get("id").getAsString();
                    if (!"0000".equals(tmpNodeId) && !nodeId.equals(tmpNodeId)) {
                        return tmpNodeId;
                    }
                }
                return null;
            }

            Assert.assertTrue("Cannot get block info after delay", attempt < attemptMax);
            // block info is not available for short time after block change,
            // therefore there is 3 s delay - it cannot be "wait for next block"
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
