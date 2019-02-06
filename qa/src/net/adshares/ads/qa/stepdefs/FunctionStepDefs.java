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
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.command.CustomCommand;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.EscConst;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.util.EscUtils;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Cucumber steps definitions for other function tests.
 */
public class FunctionStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final static int MAX_EXTRA_DATA_SIZE_IN_BYTES = 16000;

    private int node;
    private String response;
    private String txId;
    private UserData userData;

    @Given("^user, who will check (\\w+) function$")
    public void user_who_check_log_account(String function) {
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        userData = userDataList.get(new Random().nextInt(userDataList.size()));
        log.debug("Selected user {} for function {} test", userData.getAddress(), function);
    }

    @Given("^user, who will check extra_data parameter$")
    public void user_who_check_extra_data() {
        List<UserData> userDataList = UserDataProvider.getInstance().getMainUserDataList();

        userData = userDataList.get(0);
        log.debug("Selected user {} for extra_data test", userData.getAddress());
    }

    @When("^user calls log_account function$")
    public void user_calls_log_account() {
        FunctionCaller fc = FunctionCaller.getInstance();
        String resp = fc.logAccount(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        JsonObject tx = o.getAsJsonObject("tx");
        txId = tx.get("id").getAsString();
        BigDecimal fee = tx.get("fee").getAsBigDecimal();
        BigDecimal deduct = tx.get("deduct").getAsBigDecimal();
        log.debug("deduct:      {}", deduct.toPlainString());
        log.debug("fee:         {}", fee.toPlainString());
        log.debug("feeExpected: {}", EscConst.LOG_ACCOUNT_FEE.toPlainString());
        log.debug("diff:        {}", EscConst.LOG_ACCOUNT_FEE.subtract(fee).toPlainString());

        String reason;
        reason = new AssertReason.Builder().msg("Deduct is not equal to fee.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, deduct, comparesEqualTo(fee));

        reason = new AssertReason.Builder().msg("Unexpected fee for log_account.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, fee, comparesEqualTo(EscConst.LOG_ACCOUNT_FEE));
    }

    @Then("^user is able to get signature$")
    public void user_is_able_to_get_signature() {
        FunctionCaller fc = FunctionCaller.getInstance();
        String resp = fc.getTransaction(userData, txId);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        AssertReason.Builder arb = new AssertReason.Builder().req(fc.getLastRequest()).res(fc.getLastResponse());
        if (o.has("error")) {
            Assert.fail(arb.msg("Unexpected error for log_account tx: " + o.get("error").getAsString()).build());
        }

        JsonObject txObj = o.getAsJsonObject("txn");

        // check 'type'
        String txnType = txObj.get("type").getAsString();
        if (!"log_account".equals(txnType)) {
            Assert.fail(arb.msg("Invalid txn.type:" + txnType).build());
        }
        // check 'address'
        String txnAccAddr = txObj.getAsJsonObject("network_account").get("address").getAsString();
        if (!userData.getAddress().equals(txnAccAddr)) {
            Assert.fail(arb.msg("Invalid txn.network_account.address:" + txnAccAddr).build());
        }
        // check 'signature'
        String txnSign = txObj.get("signature").getAsString();
        log.debug("signature: {}", txnSign);
        if (txnSign.length() != 128) {
            Assert.fail(arb.msg("Invalid txn.signature length: " + txnSign.length()).build());
        }
    }

    @When("^user calls get_accounts function$")
    public void user_calls_get_accounts_function() {
        int nodeCount = UserDataProvider.getInstance().getNodeCount();
        node = new Random().nextInt(nodeCount) + 1;
        response = FunctionCaller.getInstance().getAccounts(userData, node);
    }

    @Then("^response contains all accounts in node$")
    public void response_contains_all_accounts_in_node() {
        FunctionCaller fc = FunctionCaller.getInstance();
        String resp = fc.getBlock(userData);

        JsonObject blockObj = Utils.convertStringToJsonObject(resp);
        int accountCountGetBlock = getNodeAccountCountFromGetBlock(blockObj, node);

        JsonObject responseObj = Utils.convertStringToJsonObject(response);
        int accountCountGetAccounts = getNodeAccountCountFromGetAccounts(responseObj);

        AssertReason.Builder arb = new AssertReason.Builder().msg("get_account response: " + response)
                .req(fc.getLastRequest()).res(fc.getLastResponse());

        // check number of accounts
        assertThat(arb.msg("Different number of accounts.").build(), accountCountGetAccounts, equalTo(accountCountGetBlock));
    }

    @When("^user call all functions with extra data$")
    public void user_calls_all_functions_with_extra_data() {
        Random random = new Random();

        String vipHash = getViphash(userData);
        String nodePublicKey = getNodePublicKey(userData);
        String userPublicKey = "A9C0D972D8AAB73805EC4A28291E052E3B5FAFE0ADC9D724917054E5E2690363";
        String userEmptyStringSignature = "CE5BDDAED1392710A4252B7A636D8D44F2FD0272D6005450F1A834A8B9301EC8BAA59CFC8C5548B8936D76853FAFF64835262BEEBD948A5C4DE2338279A10801";

        String command = "("
                + String.format("echo '{\"run\":\"get_me\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(MAX_EXTRA_DATA_SIZE_IN_BYTES))
                + String.format("echo '{\"run\":\"get_account\",\"address\":\"0001-00000000-XXXX\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_accounts\",\"node\":\"1\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_block\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_blocks\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_broadcast\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_log\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_message\", \"message_id\":\"0001:00000001\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_message_list\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_transaction\", \"txid\":\"0001:00000001:0001\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_signatures\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"get_vipkeys\", \"viphash\":\"%s\", \"extra_data\":\"%s\"}';", vipHash, EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"decode_raw\", \"data\":\"0301000000000001000000A1679B5B010000\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"create_account\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"create_node\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"log_account\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"retrieve_funds\",\"address\":\"0002-00000000-XXXX\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"change_account_key\",\"public_key\":\"%s\",\"confirm\":\"%s\", \"extra_data\":\"%s\"}';", userPublicKey, userEmptyStringSignature, EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"change_node_key\",\"public_key\":\"%s\", \"extra_data\":\"%s\"}';", nodePublicKey, EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"send_one\",\"address\":\"0001-00000000-XXXX\",\"amount\":\"1\", \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + String.format("echo '{\"run\":\"send_many\",\"wires\":{\"0001-00000000-XXXX\":\"1\", \"0002-00000000-XXXX\":\"1\"}, \"extra_data\":\"%s\"}';", EscUtils.generateMessage(random.nextInt(MAX_EXTRA_DATA_SIZE_IN_BYTES + 1)))
                + "echo '{\"run\":\"get_me\"}')";

        CustomCommand customCommand = new CustomCommand(userData, command);
        response = FunctionCaller.getInstance().callCustomCommand(customCommand);
    }

    @Then("^node will work$")
    public void node_will_work() {

    }

    /**
     * Returns number of accounts in node.
     *
     * @param accountsObj get_accounts response as JSONObject
     * @return number of accounts in in node or -1 if appropriate field was not present
     */
    private int getNodeAccountCountFromGetAccounts(JsonObject accountsObj) {
        JsonArray accArr = accountsObj.getAsJsonArray("accounts");

        return accArr.size();
    }

    /**
     * Returns number of accounts in node.
     *
     * @param blockObj get_block response as JSONObject
     * @param node     node id
     * @return number of accounts in in node or -1 if appropriate field was not present
     */
    private int getNodeAccountCountFromGetBlock(JsonObject blockObj, int node) {
        int accountCount = -1;

        JsonArray nodeArr = blockObj.getAsJsonObject("block").getAsJsonArray("nodes");
        for (JsonElement je : nodeArr) {
            JsonObject nodeObj = je.getAsJsonObject();
            String tmpNodeId = nodeObj.get("id").getAsString();
            int tmpNode = Integer.valueOf(tmpNodeId, 16);
            if (node == tmpNode) {
                accountCount = nodeObj.get("account_count").getAsInt();
                break;
            }
        }

        return accountCount;
    }

    private String getViphash(UserData userData) {
        JsonObject o = Utils.convertStringToJsonObject(FunctionCaller.getInstance().getBlock(userData));
        JsonObject block = o.get("block").getAsJsonObject();

        return block.get("viphash").getAsString();
    }

    private String getNodePublicKey(UserData userData) {
        String publicKey = null;
        String nodeId = userData.getNodeId();

        JsonObject o = Utils.convertStringToJsonObject(FunctionCaller.getInstance().getBlock(userData));
        JsonObject block = o.get("block").getAsJsonObject();
        JsonArray nodes = block.getAsJsonArray("nodes");
        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++) {
            JsonObject node = nodes.get(i).getAsJsonObject();
            if (nodeId.equals(node.get("id").getAsString())) {
                publicKey = node.get("public_key").getAsString();
                break;
            }
        }

        return publicKey;
    }
}