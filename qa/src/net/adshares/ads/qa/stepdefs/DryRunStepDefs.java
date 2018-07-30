package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.SendOneTransaction;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;

import java.util.*;

public class DryRunStepDefs {

    private static final Set<String> FIELDS_SEND_ONE_TX_DRY_RUN = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("data", "signature", "time", "account_msid", "account_hashin", "account_hashout", "deduct", "fee")));

    private UserData userData;
    /**
     * User time of transaction
     */
    private int time;
    private JsonObject respTxWithDryRun;
    private JsonObject respTxWithoutDryRun;

    @Given("^user, who wants to check dry-run$")
    public void user_who_wants_dry_run() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
        time = 10;
    }

    @When("^user sends transfer (with|without) dry-run$")
    public void user_sends_transfer_w_wo_dry_run(String dryRun) {
        boolean isDryRun = "with".equals(dryRun);

        FunctionCaller fc = FunctionCaller.getInstance();
        if (isDryRun) {
            fc.setDryRun(true);
        }
        SendOneTransaction command = new SendOneTransaction(userData, userData.getAddress(), "0.00000000001");
        command.setTime(time);
        String resp = fc.sendOne(command);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        if (null != o.get("error")) {
            String reason = new AssertReason.Builder()
                    .msg("Unexpected error during send_one: " + o.get("error").getAsString())
                    .req(fc.getLastRequest()).res(resp).build();
            Assert.fail(reason);
        }
        if (null == o.get("tx")) {
            String reason = new AssertReason.Builder()
                    .msg("Empty tx field for send_one.").req(fc.getLastRequest()).res(resp).build();
            Assert.fail(reason);
        } else {
            o = o.getAsJsonObject("tx");
        }

        if (isDryRun) {
            respTxWithDryRun = o;
        } else {
            respTxWithoutDryRun = o;
        }

        if (isDryRun) {
            fc.setDryRun(false);
        }
    }

    @Then("^data with dry-run is the same as without$")
    public void data_with_dry_run_is_same_as_without() {
        // compare only fields that are present at response tx with dry-run

        Set<Map.Entry<String, JsonElement>> entrySet = respTxWithDryRun.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            Assert.assertTrue("Unknown field: " + key, FIELDS_SEND_ONE_TX_DRY_RUN.contains(key));

            if (respTxWithoutDryRun.has(key)) {
                String valueWdr = entry.getValue().getAsString();
                String valueWodr = respTxWithoutDryRun.get(key).getAsString();
                Assert.assertEquals("Field values not equal: " + key, valueWdr, valueWodr);
            } else {
                Assert.fail("Missing field: " + key);
            }
        }
    }
}
