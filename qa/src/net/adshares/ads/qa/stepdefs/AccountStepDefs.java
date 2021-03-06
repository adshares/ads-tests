/*
 * Copyright (C) 2018 Adshares sp. z. o.o.
 *
 * This file is part of ADS Tests
 *
 * ADS Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADS Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ADS Tests. If not, see <https://www.gnu.org/licenses/>.
 */

package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.CreateAccountTransaction;
import net.adshares.ads.qa.caller.command.GetLogCommand;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    /**
     * THe current ads allow to create one remote account per node without account's key change.
     * If the account's key will not be changed, the next account should be created in another node.
     * This set store all nodes that were used for remote account creation.
     */
    private static Set<String> nodeIdsUsedForCreateRemoteAccount = new HashSet<>();

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

    @When("^user creates( remote)? account( with custom key)?( in dividend block)?$")
    public void user_creates_account(String accountType, String customKey, String dividendBlock) {
        boolean isRemote = " remote".equals(accountType);
        boolean isCustomKey = " with custom key".equals(customKey);
        boolean inDividendBlock = " in dividend block".equals(dividendBlock);
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        CreateAccountTransaction command = new CreateAccountTransaction(userData);
        if (isCustomKey) {
            command.setPublicKey(PUBLIC_KEY, SIGNATURE);
        }

        if (isRemote) {
            String userNodeId = userData.getNodeId();
            String nodeId = getDifferentNodeId(userData, userNodeId);
            assertThat("Not able to find different node id.", nodeId, notNullValue());
            int node = Integer.valueOf(nodeId, 16);
            command.setNode(node);

            resp = requestRemoteAccountCreation(command, inDividendBlock);
        } else {
            if (inDividendBlock) {
                waitForDividendBlock();
            }

            resp = fc.createAccount(command);
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

    private void waitForDividendBlock() {
        while (!EscUtils.isDividendBlock()) {
            EscUtils.waitForNextBlock();
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
     * @param command         create account command
     * @param inDividendBlock should account be created in dividend block
     * @return response: json when request was correct, empty otherwise
     */
    private String requestRemoteAccountCreation(CreateAccountTransaction command, boolean inDividendBlock) {
        String resp = "";
        int attempt = 0;
        int attemptMax = 6;
        while (attempt++ < attemptMax) {
            if (inDividendBlock) {
                waitForDividendBlock();
            }

            resp = FunctionCaller.getInstance().createAccount(command);
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
            if (!"0000".equals(tmpNodeId) && !nodeId.equals(tmpNodeId)
                    && !nodeIdsUsedForCreateRemoteAccount.contains(tmpNodeId)) {
                nodeIdsUsedForCreateRemoteAccount.add(tmpNodeId);

                return tmpNodeId;
            }
        }

        return null;
    }

    @Then("^wait until newly created account is marked as deleted$")
    public void wait_until_account_is_marked_as_deleted() {
        log.debug("start: " + System.currentTimeMillis());

        FunctionCaller fc = FunctionCaller.getInstance();
        int attemptMax = 2 * EscConst.BLOCK_DIVIDEND;
        int attempt = 0;

        boolean isDeleted;
        do {
            EscUtils.waitForNextBlock();
            String resp = fc.getMe(createdUserData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            JsonObject accountObject = o.getAsJsonObject("account");
            int status = accountObject.get("status").getAsInt();

            isDeleted = (status & 1) != 0;
        } while (!isDeleted && attempt++ < attemptMax);
        Assert.assertTrue("Account was not deleted in expected time.", isDeleted);


        log.debug("end:   " + System.currentTimeMillis());
    }

    @And("^checks( full)? log for this account$")
    public void check_log_of_created_account(String full) {
        final boolean isFull = full != null;
        FunctionCaller fc = FunctionCaller.getInstance();

        GetLogCommand getLogCommand = new GetLogCommand(createdUserData);
        getLogCommand.setType("create_account");
        if (isFull) {
            getLogCommand.setFull(true);
        }
        String response = fc.getLog(getLogCommand);

        LogChecker lc = new LogChecker(response);
        JsonArray arr = lc.getLogArray();

        final int expected = isFull ? 2 : 1;
        String reason = new AssertReason.Builder().msg("Unexpected events in user log.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(response).build();
        assertThat(reason, arr.size(), equalTo(expected));
    }
}
