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

import cucumber.api.java.en.Given;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.caller.FunctionCaller;
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