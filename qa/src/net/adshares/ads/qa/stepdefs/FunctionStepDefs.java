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
        int nodeAccCount = -1;
        BigDecimal nodeBalance = BigDecimal.ZERO;
        BigDecimal totalBalance = BigDecimal.ZERO;

        FunctionCaller fc = FunctionCaller.getInstance();
        String resp = fc.getBlock(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        JsonArray nodeArr = o.getAsJsonObject("block").getAsJsonArray("nodes");
        for (JsonElement je : nodeArr) {
            JsonObject nodeObj = je.getAsJsonObject();
            String tmpNodeId = nodeObj.get("id").getAsString();
            int tmpNode = Integer.valueOf(tmpNodeId, 16);
            if (node == tmpNode) {
                nodeAccCount = nodeObj.get("account_count").getAsInt();
                nodeBalance = nodeObj.get("balance").getAsBigDecimal();
                break;
            }
        }
        JsonObject responseObj = Utils.convertStringToJsonObject(response);
        JsonArray accArr = responseObj.getAsJsonArray("accounts");

        AssertReason.Builder arb = new AssertReason.Builder().msg("get_account response: " + response)
                .req(fc.getLastRequest()).res(fc.getLastResponse());

        if (accArr.size() != nodeAccCount) {
            assertThat(arb.msg("Different number of accounts.").build(), accArr.size(), equalTo(nodeAccCount));
        }

        for (JsonElement je : accArr) {
            totalBalance = totalBalance.add(je.getAsJsonObject().get("balance").getAsBigDecimal());
        }

        log.debug("totalBalance: {}", totalBalance.toPlainString());
        log.debug("nodeBalance : {}", nodeBalance.toPlainString());
        log.debug("diff        : {}", totalBalance.subtract(nodeBalance).toPlainString());
        /*
         * Currently difference between balances from both responses is compared with variable.
         * Eventually difference should be computer from other function response.
         * Matcher "lessThan" should be changed to "comparesEqualTo".
         */
        BigDecimal allowedDifference = new BigDecimal("0.00500000000");
        assertThat(arb.msg("Balance difference is too big.").build(),
                totalBalance.subtract(nodeBalance).abs(), lessThan(allowedDifference));
    }
}