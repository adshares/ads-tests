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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
        FunctionCaller fc = FunctionCaller.getInstance();
        String resp = FunctionCaller.getInstance().changeAccountKey(userData, PUBLIC_KEY, SIGNATURE);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String reason;
        // check, if accepted
        reason = new AssertReason.Builder().msg("Change key transaction was not accepted by node.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(o));

        // check 'result' field
        String result = o.has("result") ? o.get("result").getAsString() : "null";
        reason = new AssertReason.Builder().msg("Incorrect 'result' field.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, result, equalTo("PKEY changed"));

        //check fee
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        reason = new AssertReason.Builder().msg("Invalid fee computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, fee, comparesEqualTo(EscConst.CHANGE_ACCOUNT_KEY_FEE));

        // check deduct - in case of change key: deduct == fee
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, deduct, comparesEqualTo(fee));
    }

    @Then("^he cannot use old key$")
    public void he_cannot_use_old_key() {
        String resp = FunctionCaller.getInstance().getMe(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        String errorDesc = o.has("error") ? o.get("error").getAsString() : "null";
        assertThat("Transaction with old key wasn't rejected with correct error description.",
                errorDesc, equalTo("Wrong signature"));
    }

    @Then("^transaction can be authorised with new key$")
    public void transaction_can_be_authorised_with_new_key() {
        userData.setSecret(PRIVATE_KEY);
        String resp = FunctionCaller.getInstance().getMe(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String reason = new AssertReason.Builder().msg("Transaction with new key wasn't accepted.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(resp).build();
        assertThat(reason, !o.has("error"));

        BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
        log.debug("Balance {}", balance.toPlainString());
    }

    @Then("^account change key transaction is present in log$")
    public void account_change_key_transaction_is_present_in_log() {
        final String resp = FunctionCaller.getInstance().getLog(userData, lastEventTimestamp);
        LogChecker lc = new LogChecker(resp);
        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "change_account_key");
        BigDecimal feeFromLog = lc.getBalanceFromLogArray(lf);
        // fee in log is negative, but in doc is positive
        // change sign of fee from log
        feeFromLog = BigDecimal.ZERO.subtract(feeFromLog);
        log.debug("fees in log: {}", feeFromLog.toPlainString());
        log.debug("fees in doc: {}", EscConst.CHANGE_ACCOUNT_KEY_FEE.toPlainString());
        log.debug("diff:        {}", feeFromLog.subtract(EscConst.CHANGE_ACCOUNT_KEY_FEE).toPlainString());

        // check fee from log
        String reason = new AssertReason.Builder().msg("Invalid fee computed.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(resp).build();
        assertThat(reason, feeFromLog, comparesEqualTo(EscConst.CHANGE_ACCOUNT_KEY_FEE));
    }

    @When("^user creates( remote)? account( with custom key)?$")
    public void user_creates_account(String accountType, String customKey) {
        boolean isRemote = " remote".equals(accountType);
        boolean isCustomKey = " with custom key".equals(customKey);
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        if (isRemote) {
            String userNodeId = userData.getNodeId();
            String nodeId = getDifferentNodeId(userData, userNodeId);
            assertThat("Not able to find different node id.", nodeId, notNullValue());
            // Function create_account takes node parameter in decimal format
            int node = (nodeId != null) ? Integer.valueOf(nodeId, 16) : 1;
            if (isCustomKey) {
                resp = requestRemoteAccountCreation(userData, node, PUBLIC_KEY, SIGNATURE);
            } else {
                resp = requestRemoteAccountCreation(userData, node, null, null);
            }
        } else {
            if (isCustomKey) {
                resp = fc.createAccount(userData, PUBLIC_KEY, SIGNATURE);
            } else {
                resp = fc.createAccount(userData);
            }
        }

        JsonObject o = Utils.convertStringToJsonObject(resp);
        String reason;
        // check, if accepted
        reason = new AssertReason.Builder().msg("Create account transaction was not accepted by node.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(o));

        //check fee
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        BigDecimal expectedFee = EscConst.CREATE_ACCOUNT_LOCAL_FEE;
        if (isRemote) {
            // remote transaction has additional fee for remote node
            expectedFee = expectedFee.add(EscConst.CREATE_ACCOUNT_REMOTE_FEE);
        }
        reason = new AssertReason.Builder().msg("Invalid fee computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, fee, comparesEqualTo(expectedFee));

        // check deduct - in case of change key: deduct == fee
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        BigDecimal expectedDeduct = expectedFee.add(EscConst.USER_MIN_MASS);
        reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, deduct, comparesEqualTo(expectedDeduct));

        // check address
        String address;
        if (isRemote) {
            address = getCreatedAccountAddressFromLog();
        } else {
            reason = new AssertReason.Builder().msg("Missing 'new_account' object in response.")
                    .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
            assertThat(reason, o.has("new_account"));
            address = o.getAsJsonObject("new_account").get("address").getAsString();
        }
        assertThat("Created address is not valid: " + address, EscUtils.isValidAccountAddress(address));
        createdUserData = UserDataProvider.getInstance().cloneUser(userData, address);
        if (isCustomKey) {
            createdUserData.setSecret(PRIVATE_KEY);
        }
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
                log.debug("Error occurred: {}", errorDesc);
                assertThat("Unexpected error after account creation.", errorDesc,
                        equalTo(EscConst.Error.GET_GLOBAL_USER_FAILED));
            } else {
                BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
                log.debug("Balance {}", balance.toPlainString());
                break;
            }

            assertThat("Cannot get account info after delay.", attempt < attemptMax);
            EscUtils.waitForNextBlock();
        }
    }

    /**
     * Requests account creation few times with delay.
     *
     * @param userData  user data
     * @param node      node in which account should be created (decimal)
     * @param publicKey new public key. Null is allowed. If key is null, key will be the same as creator key.
     * @param signature empty String signed with new private key. Null is allowed. If signature is null, key
     *                  will not be changed
     * @return response: json when request was correct, empty otherwise
     */
    private String requestRemoteAccountCreation(UserData userData, int node, String publicKey, String signature) {
        String resp = "";
        int attempt = 0;
        int attemptMax = 6;
        while (attempt++ < attemptMax) {

            resp = FunctionCaller.getInstance().createAccount(userData, node, publicKey, signature);
            JsonObject o = Utils.convertStringToJsonObject(resp);

            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.debug("Error occurred: {}", errorDesc);
                assertThat("Unexpected error after account creation.", errorDesc,
                        equalTo(EscConst.Error.CREATE_ACCOUNT_BAD_TIMING));
            } else {
                break;
            }

            assertThat("Cannot create remote account.", attempt < attemptMax);
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

        FunctionCaller fc = FunctionCaller.getInstance();
        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "account_created");

        int attempt = 0;
        int attemptMax = 3;
        while (attempt++ < attemptMax) {
            EscUtils.waitForNextBlock();

            String resp = fc.getLog(userData, lastEventTimestamp);
            LogChecker lc = new LogChecker(resp);
            JsonArray arr = lc.getFilteredLogArray(lf);
            if (arr.size() > 0) {
                JsonObject accCrObj = arr.get(arr.size() - 1).getAsJsonObject();
                String requestStatus =
                        accCrObj.has("request") ? accCrObj.get("request").getAsString() : "null";
                assertThat("Request was not accepted.", requestStatus, equalTo("accepted"));
                address = accCrObj.get("address").getAsString();
                break;
            }

            String reason = new AssertReason.Builder().msg("Cannot get account address after delay.")
                    .req(fc.getLastRequest()).res(resp)
                    .msg("Full log:").msg(fc.getLog(userData)).build();
            assertThat(reason, attempt < attemptMax);
        }

        return address;
    }

    private String getDifferentNodeId(UserData userData, String nodeId) {
        String resp = FunctionCaller.getInstance().getBlock(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
        for (JsonElement je : arr) {
            String tmpNodeId = je.getAsJsonObject().get("id").getAsString();
            if (!"0000".equals(tmpNodeId) && !nodeId.equals(tmpNodeId)) {
                return tmpNodeId;
            }
        }
        return null;
    }
}
