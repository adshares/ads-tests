package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
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
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RetrieveFundsStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private TransferUser retriever;
    private TransferUser inactiveUser;
    private String lastResp;

    @Given("^user in one node$")
    public void users_in_node() {
        FunctionCaller fc = FunctionCaller.getInstance();
        UserData u = UserDataProvider.getInstance().getUserDataList(1).get(0);

        LogChecker lc = new LogChecker(fc.getLog(u));
        retriever = new TransferUser();
        retriever.setUserData(u);
        retriever.setStartBalance(lc.getBalanceFromAccountObject());
        retriever.setLastEventTimestamp(lc.getLastEventTimestamp().incrementEventNum());
    }

    @Given("^different user in the same node$")
    public void diff_user_in_same_node() {
        FunctionCaller fc = FunctionCaller.getInstance();
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        String retrieverAddress = retriever.getUserData().getAddress();
        inactiveUser = null;
        for (UserData u : userDataList) {
            if (u.isAccountFromSameNode(retrieverAddress) && !retrieverAddress.equals(u.getAddress())) {
                inactiveUser = new TransferUser();
                inactiveUser.setUserData(u);
            }
        }
        assertThat("Cannot find user in the same node", inactiveUser, notNullValue());
        inactiveUser.setStartBalance(fc.getUserAccountBalance(inactiveUser.getUserData()));
    }

    @Given("^different user \\((main|normal)\\) in the different node$")
    public void diff_user_in_diff_node(String type) {
        FunctionCaller fc = FunctionCaller.getInstance();
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        String retrieverAddress = retriever.getUserData().getAddress();
        inactiveUser = null;
        for (UserData u : userDataList) {
            if (!u.isAccountFromSameNode(retrieverAddress)) {
                if (("main".equals(type) && u.isMainAccount()) || ("normal".equals(type) && !u.isMainAccount())) {
                    inactiveUser = new TransferUser();
                    inactiveUser.setUserData(u);
                }
            }
        }
        assertThat(String.format("Cannot find %s user in the different node", type), inactiveUser, notNullValue());
        inactiveUser.setStartBalance(fc.getUserAccountBalance(inactiveUser.getUserData()));
    }

    @When("^user requests retrieve$")
    public void user_requests_retrieve() {
        FunctionCaller fc = FunctionCaller.getInstance();
        lastResp = fc.retrieveFunds(retriever.getUserData(), inactiveUser.getUserData().getAddress());

        String reason = new AssertReason.Builder().msg("Retrieve request not accepted.")
                .req(fc.getLastRequest()).res(lastResp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(lastResp));
    }

    @When("^user retrieves funds$")
    public void user_retrieves_funds() {
        FunctionCaller fc = FunctionCaller.getInstance();
        lastResp = fc.retrieveFunds(retriever.getUserData(), inactiveUser.getUserData().getAddress());

        String reason = new AssertReason.Builder().msg("Retrieve funds not accepted.")
                .req(fc.getLastRequest()).res(lastResp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(lastResp));
    }

    @When("^user requests retrieve but it is not accepted$")
    public void user_requests_retrieve_not_accepted() {
        FunctionCaller fc = FunctionCaller.getInstance();
        lastResp = fc.retrieveFunds(retriever.getUserData(), inactiveUser.getUserData().getAddress());

        String reason = new AssertReason.Builder().msg("Retrieve request was accepted.")
                .req(fc.getLastRequest()).res(lastResp).build();
        assertThat(reason, !EscUtils.isTransactionAcceptedByNode(lastResp));
    }

    @When("^account is not active for RETRIEVE_DELAY time$")
    public void delay_before_retrieve() {
        // before retrieval account must be inactive for time of:
        // EscConst.BLOCK_DIVIDEND * EscConst.BLOCK_PERIOD (in seconds)
        try {
            Thread.sleep(EscConst.BLOCK_PERIOD_MS * EscConst.BLOCK_DIVIDEND);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted");
            log.error(e.toString());
        }
    }

    @Then("^after processing time inactive account is empty$")
    public void inactive_account_is_empty() {
        FunctionCaller fc = FunctionCaller.getInstance();


        int loopCnt = 0;
        int loopCntMax = 5;
        do {
            assertThat(String.format("Account was not empty in time of %d blocks", loopCntMax), loopCnt < loopCntMax);
            try {
                Thread.sleep(EscConst.BLOCK_PERIOD_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            loopCnt++;
        } while (BigDecimal.ZERO.compareTo(fc.getUserAccountBalance(inactiveUser)) != 0);
        log.debug("Account was empty after {} block(s)", loopCnt);

        String resp = fc.getLog(inactiveUser.getUserData());
        String reason = new AssertReason.Builder().msg("Inactive account balance is different than sum of logged events.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, new LogChecker(resp).isBalanceFromObjectEqualToArray());
    }

    @Then("^retriever account is increased by retrieved amount$")
    public void retriever_account_is_increased() {
        FunctionCaller fc = FunctionCaller.getInstance();

        UserData retrieverData = retriever.getUserData();
        LogChecker lc = new LogChecker();
        String resp = fc.getLog(retrieverData);
        lc.setResp(resp);
        String reason = new AssertReason.Builder().msg("Balance from log is different than that from object")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, lc.isBalanceFromObjectEqualToArray());

        lastResp = fc.getLog(retrieverData, retriever.getLastEventTimestamp());
        lc.setResp(lastResp);

        LogFilter lf;
        lf = new LogFilter(false);
        lf.addFilter("type", "retrieve_funds");
        // get sum for other than retrieve_funds
        BigDecimal balanceOtherEvents = lc.getBalanceFromLogArray(lf);
        lf = new LogFilter(true);
        lf.addFilter("type", "retrieve_funds");
        // get sum for retrieve_funds
        BigDecimal balanceRetrieveEvents = lc.getBalanceFromLogArray(lf);
        // check retrieve_funds events
        JsonArray arr = lc.getFilteredLogArray(lf);
        log.debug("size = {}", arr.size());
        for (int i = 0; i < 4; i++) {
            JsonObject o = arr.get(i).getAsJsonObject();

            log.debug(Utils.jsonPrettyPrint(o.toString()));
            BigDecimal senderFee;
            switch (i) {
                case 0:
                case 2:
                    senderFee = o.get("sender_fee").getAsBigDecimal();
                    assertThat("Invalid fee for retrieve_funds.", senderFee, comparesEqualTo(EscConst.RETRIEVE_REQUEST_FEE));
                    break;
                case 1:
                    // 1st call confirm
                    break;
                case 3:
                    // 2nd call confirm
                    senderFee = o.get("sender_fee").getAsBigDecimal();
                    BigDecimal senderBalance = o.get("sender_balance").getAsBigDecimal();
                    BigDecimal senderFeeExpected = senderBalance.multiply(EscConst.RETRIEVE_FEE).setScale(11, BigDecimal.ROUND_FLOOR);
                    log.debug("senderFeeExpected1: {}", senderFeeExpected.toPlainString());
                    BigDecimal additionalRemoteFee = senderBalance.subtract(senderFeeExpected)
                            .multiply(EscConst.REMOTE_TX_FEE_COEFFICIENT).setScale(11, BigDecimal.ROUND_FLOOR);
                    senderFeeExpected = senderFeeExpected.add(additionalRemoteFee);
                    log.debug("senderFee:          {}", senderFee.toPlainString());
                    log.debug("senderFeeExpected2: {}", senderFeeExpected.toPlainString());
                    log.debug("diff:               {}", senderFee.subtract(senderFeeExpected).toPlainString());
                    assertThat("Unexpected fee for retrieve funds.", senderFee, comparesEqualTo(senderFeeExpected));
                    break;
            }
        }

        BigDecimal startBalance = retriever.getStartBalance();
        BigDecimal balance = lc.getBalanceFromAccountObject();

        reason = new AssertReason.Builder().msg("Unexpected balance after retrieve funds.")
                .msg("Start Balance:   " + startBalance.toPlainString())
                .msg("Retrieve Events: " + balanceRetrieveEvents.toPlainString())
                .msg("Other Events:    " + balanceOtherEvents.toPlainString())
                .msg("End Balance:     " + balance.toPlainString()).build();
        assertThat(reason, balance,
                comparesEqualTo(startBalance.add(balanceRetrieveEvents).add(balanceOtherEvents)));

        log.debug("retriever Start Balance: {}", startBalance.toPlainString());
        log.debug("log Retrieve Events:     {}", balanceRetrieveEvents.toPlainString());
        log.debug("log Other Events:        {}", balanceOtherEvents.toPlainString());
        log.debug("retriever End Balance:   {}", balance.toPlainString());

    }
}
