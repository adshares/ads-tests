package net.adshares.ads.qa.stepdefs;

import cucumber.api.java.en.Given;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.FunctionCaller;
import net.adshares.ads.qa.util.LogChecker;
import net.adshares.ads.qa.util.TransactionIdChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Cucumber common step definitions.
 */
public class CommonStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Given("user log")
    public void user_log() {
        FunctionCaller fc = FunctionCaller.getInstance();
        LogChecker lc = new LogChecker();
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        for (UserData user : userDataList) {
            String userLog = fc.getLog(user);

            final String userAddress = user.getAddress();
            log.trace(userAddress);
            lc.setResp(userLog);
            String reason = new AssertReason.Builder().req(fc.getLastRequest()).res(fc.getLastResponse())
                    .msg("Balance is different than sum of logged events in account " + userAddress).build();
            assertThat(reason, lc.isBalanceFromObjectEqualToArray());
        }
    }

    @Given("transaction ids")
    public void transaction_ids() {
        TransactionIdChecker.getInstance().checkAll();
    }

}