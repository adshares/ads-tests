package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.SendOneTransaction;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DryRunStepDefs {

    private static final Set<String> FIELDS_SEND_ONE_TX_DRY_RUN = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("data", "signature", "time", "account_msid", "account_hashin", "account_hashout", "deduct", "fee")));

    private TransferUser userData;
    private TransferUser operator;

    /**
     * User time of transaction
     */
    private int time = 10;
    private JsonObject respTxWithDryRun;
    private JsonObject respTxWithoutDryRun;
    private String amount;

    @Given("^user, who wants to check dry-run$")
    public void user_who_wants_dry_run() {
        UserData u = UserDataProvider.getInstance().getUserDataList(1).get(0);
        LogChecker lc = new LogChecker(FunctionCaller.getInstance().getLog(u));
        userData = new TransferUser();
        userData.setUserData(u);
        userData.setStartBalance(lc.getBalanceFromAccountObject());
        userData.setLastEventTimestamp(lc.getLastEventTimestamp().incrementEventNum());

        amount = "0.00000000001";
    }

    @Given("^main and regular user, who want to check dry-run$")
    public void main_and_regular_user_who_want_to_check_dry_run() {
        operator = null;
        userData = null;
        List<UserData> list = UserDataProvider.getInstance().getUserDataList();

        LogChecker lc = new LogChecker();
        FunctionCaller fc = FunctionCaller.getInstance();
        for (UserData u : list) {
            if (u.getNode() == 1) {
                if (u.isMainAccount()) {
                    lc.setResp(fc.getLog(u));
                    operator = new TransferUser();
                    operator.setUserData(u);
                    operator.setStartBalance(lc.getBalanceFromAccountObject());
                    operator.setLastEventTimestamp(lc.getLastEventTimestamp().incrementEventNum());
                }
            }
        }
        for (UserData u : list) {
            if (u.getNode() == 1) {
                if (!u.isMainAccount()) {
                    lc.setResp(fc.getLog(u));
                    userData = new TransferUser();
                    userData.setUserData(u);
                    userData.setStartBalance(lc.getBalanceFromAccountObject());
                    userData.setLastEventTimestamp(lc.getLastEventTimestamp().incrementEventNum());
                }
            }
        }
        assertThat(userData, notNullValue());
        assertThat(operator, notNullValue());

        amount = "123";
    }

    @When("^user sends transfer (with|without) dry-run$")
    public void user_sends_transfer_w_wo_dry_run(String dryRun) {
        boolean isDryRun = "with".equals(dryRun);

        FunctionCaller fc = FunctionCaller.getInstance();
        UserData u = userData.getUserData();
        SendOneTransaction command = new SendOneTransaction(u, u.getAddress(), amount);
        command.setTime(time);
        if (isDryRun) {
            fc.setDryRun(true);
        }
        String resp = fc.sendOne(command);
        if (isDryRun) {
            fc.setDryRun(false);
        }
        JsonObject o = Utils.convertStringToJsonObject(resp);
        assertNoError(o);
        assertPresentTx(o);
        assertThat("Transaction status is invalid.", EscUtils.isTransactionAcceptedByNode(o), not(equalTo(isDryRun)));

        o = o.getAsJsonObject("tx");

        if (isDryRun) {
            respTxWithDryRun = o;
        } else {
            respTxWithoutDryRun = o;
        }

        assertEventsCount(userData, isDryRun);
    }

    /**
     * Checks, if user log has valid number of outgoing transfer.
     *
     * @param u        user data
     * @param isDryRun true if dry-run, false otherwise
     */
    private void assertEventsCount(TransferUser u, boolean isDryRun) {
        FunctionCaller fc = FunctionCaller.getInstance();
        String logResp = fc.getLog(u.getUserData(), u.getLastEventTimestamp());
        LogChecker lc = new LogChecker(logResp);

        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "send_one");
        lf.addFilter("inout", "out");
        JsonArray array = lc.getFilteredLogArray(lf);
        int expectedEventsCount = isDryRun ? 0 : 1;
        assertThat("Incorrect number of events", array.size(), equalTo(expectedEventsCount));
    }

    /**
     * Checks, if error occur (response have error description).
     *
     * @param o response object
     */
    private void assertNoError(JsonObject o) {
        if (null != o.get("error")) {
            FunctionCaller fc = FunctionCaller.getInstance();
            String reason = new AssertReason.Builder()
                    .msg("Unexpected error during dry-run test: " + o.get("error").getAsString())
                    .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
            Assert.fail(reason);
        }
    }

    /**
     * Checks, if tx object is present.
     *
     * @param o response object
     */
    private void assertPresentTx(JsonObject o) {
        if (null == o.get("tx")) {
            FunctionCaller fc = FunctionCaller.getInstance();
            String reason = new AssertReason.Builder()
                    .msg("Empty tx field for send_one.").req(fc.getLastRequest()).res(fc.getLastResponse()).build();
            Assert.fail(reason);
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

    @When("^regular user sends to main with dry-run$")
    public void regular_sends_to_main_with_dry_run() {
        FunctionCaller fc = FunctionCaller.getInstance();
        fc.setDryRun(true);
        SendOneTransaction transaction = new SendOneTransaction(userData.getUserData(), operator.getUserData().getAddress(), amount);
        transaction.setTime(time);
        String resp = fc.sendOne(transaction);
        fc.setDryRun(false);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        assertNoError(o);
        assertPresentTx(o);
        Assert.assertFalse("Dry-run transaction was accepted by node.", EscUtils.isTransactionAcceptedByNode(o));
        respTxWithDryRun = o.getAsJsonObject("tx");

        assertEventsCount(userData, true);
    }

    @And("^main user sends transfer using tx data$")
    public void main_sends_using_tx_data() {
        FunctionCaller fc = FunctionCaller.getInstance();

        // get tx data from dry-run transfer
        int msid = respTxWithDryRun.get("account_msid").getAsInt();
        String hash = respTxWithDryRun.get("account_hashin").getAsString();
        String signature = respTxWithDryRun.get("signature").getAsString();

        SendOneTransaction transaction = new SendOneTransaction(operator.getUserData(), operator.getUserData().getAddress(), amount);
        transaction.setTime(time);
        transaction.setAccountMsid(msid);
        transaction.setAccountHash(hash);
        transaction.setSender(userData.getUserData().getAddress());
        transaction.setSignature(signature);

        String resp = fc.sendOne(transaction);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        assertNoError(o);
        assertPresentTx(o);
        Assert.assertTrue("Transaction was not accepted by node.", EscUtils.isTransactionAcceptedByNode(o));
        respTxWithoutDryRun = o.getAsJsonObject("tx");

        assertEventsCount(userData, false);
    }
}
