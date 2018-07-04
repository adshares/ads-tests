package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.FunctionCaller;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Cucumber steps definitions for other function tests.
 */
public class FunctionStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserData userData;
    private String txId;

    @Given("^user, who will check log_account function$")
    public void user_who_check_log_account() {
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        userData = userDataList.get(new Random().nextInt(userDataList.size()));
        log.debug("Selected user {}", userData.getAddress());
    }

    @When("^user calls log_account function$")
    public void user_calls_log_account() {
        String resp = FunctionCaller.getInstance().logAccount(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        txId = o.getAsJsonObject("tx").get("id").getAsString();
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
}