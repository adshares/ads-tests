package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.EscConst;
import net.adshares.ads.qa.util.FunctionCaller;
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
}