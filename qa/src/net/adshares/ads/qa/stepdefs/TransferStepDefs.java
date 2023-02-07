/*
 * Copyright (C) 2018-2023 Adshares sp. z. o.o.
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
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.SendManyTransaction;
import net.adshares.ads.qa.caller.command.SendOneTransaction;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.stepdefs.exception.TransferBlockedDueToUpcomingDormantFeeException;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Cucumber steps definitions for transfer tests
 */
public class TransferStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String ERROR_TOO_LOW_BALANCE = "Too low balance";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_ERROR_INFO = "error_info";
    /**
     * Regular expression for error message which explains transaction blocked due to upcoming dormant fee
     */
    private static final String REGEX_ERROR_DORMANT_FEE = "Available balance reduced to \\d+\\.\\d+ for upcoming dormant fee. Issue cheaper tx first.";
    /**
     * Regexp for transfer event in log
     */
    private static final String REGEX_TRANSFER_TYPE = "send_one|send_many";

    private TransferUser txSender;
    private List<TransferUser> txReceivers;
    private String message;
    private boolean isTransactionAccepted;


    @Given("^(\\d+) users in (same|different) node$")
    public void user_in_same_node(int userCount, String nodeType) {
        boolean isSameNode = "same".equals(nodeType);
        List<UserData> userDataList;
        if (isSameNode) {
            userDataList = UserDataProvider.getInstance().getUserDataList(userCount, true);
        } else {
            userDataList = UserDataProvider.getInstance().getUserDataFromDifferentNodes(userCount);
        }

        FunctionCaller fc = FunctionCaller.getInstance();
        BigDecimal maxBalance = BigDecimal.ZERO;
        int maxBalanceIndex = -1;
        for (int i = 0; i < userDataList.size(); i++) {
            BigDecimal balance = fc.getUserAccountBalance(userDataList.get(i));
            if (balance.compareTo(maxBalance) > 0) {
                maxBalance = balance;
                maxBalanceIndex = i;
            }
        }
        assertThat("No user with positive balance.", maxBalanceIndex != -1);

        LogChecker lc = new LogChecker();
        txReceivers = new ArrayList<>();
        for (int i = 0; i < userDataList.size(); i++) {
            UserData userData = userDataList.get(i);
            lc.setResp(fc.getLog(userData));

            TransferUser tu = new TransferUser();
            tu.setUserData(userData);
            tu.setStartBalance(lc.getBalanceFromAccountObject());
            tu.setLastEventTimestamp(lc.getLastEventTimestamp().incrementEventNum());

            if (i == maxBalanceIndex) {
                txSender = tu;
            } else {
                txReceivers.add(tu);
            }
        }
    }

    @Given("^user, who wants to send transfer$")
    public void user_who_wants_to_send_transfer() {
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        FunctionCaller fc = FunctionCaller.getInstance();

        txSender = null;
        for (UserData u : userDataList) {
            BigDecimal balance = fc.getUserAccountBalance(u);
            if (balance.compareTo(new BigDecimal("1000")) > 0) {
                txSender = new TransferUser();
                txSender.setUserData(u);
                txSender.setStartBalance(balance);
                break;
            }
        }

        assertThat("No user with sufficient balance.", txSender, notNullValue());
    }

    @When("^sender sends ([-]?\\d+(\\.\\d+)?) ADS to receiver[s]?( with message)?$")
    public void send_ads(String txAmount, String decimalPart, String withMessage) throws TransferBlockedDueToUpcomingDormantFeeException {
        FunctionCaller fc = FunctionCaller.getInstance();
        UserData sender = txSender.getUserData();
        String senderAddress = sender.getAddress();
        BigDecimal senderBalance = txSender.getStartBalance();

        int receiversCount = txReceivers.size();
        // amount to single receiver
        BigDecimal amount = new BigDecimal(txAmount).setScale(11, RoundingMode.FLOOR);
        // amount from sender to all receivers
        BigDecimal amountOut = amount.multiply(new BigDecimal(receiversCount)).setScale(11, RoundingMode.FLOOR);
        // transfer fee payed by sender
        BigDecimal fee;
        String jsonResp;

        if (receiversCount > 1) {
            // send many
            Map<String, String> map = new HashMap<>(receiversCount);
            for (TransferUser txReceiver : txReceivers) {
                map.put(txReceiver.getUserData().getAddress(), txAmount);
            }
            jsonResp = fc.sendMany(new SendManyTransaction(sender, map));
            fee = getTransferFee(senderAddress, map);
        } else {
            // send one
            UserData receiver = txReceivers.get(0).getUserData();
            String receiverAddress = receiver.getAddress();

            SendOneTransaction command = new SendOneTransaction(sender, receiverAddress, txAmount);
            if (withMessage != null) {
                message = EscUtils.generateHexMessage(32);
                command.setMessage(message);
            }
            jsonResp = fc.sendOne(command);

            JsonObject jsonObject = Utils.convertStringToJsonObject(jsonResp);
            if (jsonObject.has(FIELD_ERROR)
                    && jsonObject.has(FIELD_ERROR_INFO)
                    && ERROR_TOO_LOW_BALANCE.equals(jsonObject.get(FIELD_ERROR).getAsString())
                    && jsonObject.get(FIELD_ERROR_INFO).getAsString().matches(REGEX_ERROR_DORMANT_FEE)
            ) {
                throw new TransferBlockedDueToUpcomingDormantFeeException();
            }
            fee = getTransferFee(senderAddress, receiverAddress, amount);
        }

        boolean isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(jsonResp);
        if (isTransactionAccepted) {
            JsonObject o = Utils.convertStringToJsonObject(jsonResp);
            long transferTime = o.getAsJsonObject("account").get("time").getAsLong();

            BigDecimal eventsAmount = getTotalAmountOfEventsBeforeTransfer(receiversCount);

            if (eventsAmount.compareTo(BigDecimal.ZERO) != 0) {
                log.debug("Additional events amount {}", eventsAmount.toPlainString());
                senderBalance = senderBalance.add(eventsAmount);
                txSender.setStartBalance(senderBalance);
            }

            LogEventTimestamp lastEventTimestamp = getLastTransferEventTimestamp(sender, transferTime);
            txSender.setLastEventTimestamp(lastEventTimestamp.incrementEventNum());
        }

        BigDecimal tmpSenderExpBalance = senderBalance.subtract(amountOut).subtract(fee);
        BigDecimal minAccountBalance = sender.getMinAllowedBalance();

        AssertReason.Builder assertReasonBuilder = new AssertReason.Builder()
                .req(fc.getLastRequest()).res(fc.getLastResponse())
                .msg("Sender: " + txSender.getUserData().getAddress())
                .msg("\tbalance: " + senderBalance.toPlainString())
                .msg("\tamount:  " + amountOut.toPlainString())
                .msg("\tfee:     " + fee.toPlainString());

        // check, if transfer is possible and balance won't be bigger after transfer
        if (tmpSenderExpBalance.compareTo(minAccountBalance) >= 0 && tmpSenderExpBalance.compareTo(senderBalance) < 0) {
            // update balances, if transfer is possible
            String reason = assertReasonBuilder.msg("Transfer was not accepted by node.").build();
            assertThat(reason, isTransactionAccepted);

            // receivers
            TransferData txDataIn = new TransferData();
            txDataIn.setAmount(amount);
            txDataIn.setFee(BigDecimal.ZERO);
            for (TransferUser txReceiver : txReceivers) {
                txReceiver.setExpBalance(txReceiver.getStartBalance().add(amount));
                txReceiver.setTransferData(txDataIn);
            }
            //sender
            TransferData txDataOut = new TransferData();
            txDataOut.setAmount(amountOut);
            txDataOut.setFee(fee);
            txSender.setExpBalance(tmpSenderExpBalance);
            txSender.setTransferData(txDataOut);

            checkComputedFeeWithResponse(jsonResp, txDataOut);

        } else {
            String reason = assertReasonBuilder.msg("Transfer was accepted by node.").build();
            assertThat(reason, !isTransactionAccepted);

            log.debug("Cannot transfer funds");
            log.debug("\tbalance: {}", senderBalance);
            log.debug("\t amount: {}", txAmount);
            log.debug("\t    fee: {}", fee);

            for (TransferUser txReceiver : txReceivers) {
                txReceiver.setExpBalance(txReceiver.getStartBalance());
            }
            txSender.setExpBalance(senderBalance);
        }
    }

    private BigDecimal getTotalAmountOfEventsBeforeTransfer(int receiversCount) {
        BigDecimal additionalEventsAmount = BigDecimal.ZERO;
        FunctionCaller fc = FunctionCaller.getInstance();

        String getLogResponse = fc.getLog(txSender.getUserData(), txSender.getLastEventTimestamp());

        LogChecker logChecker = new LogChecker(getLogResponse);
        JsonArray logArray = logChecker.getLogArray();
        final int logArraySize = logArray.size();

        AssertReason.Builder assertReasonBuilder = new AssertReason.Builder()
                .req(fc.getLastRequest()).res(fc.getLastResponse());

        if (logArraySize <= 0) {
            Assert.fail(assertReasonBuilder.msg("Missing events in log.").build());
        }

        LogFilter transferLogFilter = new LogFilter(true);
        transferLogFilter.addFilter("type", REGEX_TRANSFER_TYPE);
        final int numberOfTransfersInLog = logChecker.getFilteredLogArray(transferLogFilter).size();
        if (numberOfTransfersInLog < receiversCount) {
            Assert.fail(assertReasonBuilder.msg("Missing transfers in log.").build());
        }
        if (numberOfTransfersInLog > receiversCount) {
            Assert.fail(assertReasonBuilder.msg("Too many transfers in log.").build());
        }

        // look for transfer events, all
        boolean isTransferFound = false;
        int node = txSender.getUserData().getNode();
        for (JsonElement je : logArray) {
            final JsonObject logEntry = je.getAsJsonObject();
            String type = logEntry.get("type").getAsString();

            if (type.matches(REGEX_TRANSFER_TYPE)) {
                isTransferFound = true;
            } else {
                if (isTransferFound) {
                    break;
                }

                if ("dividend".equals(type)) {
                    additionalEventsAmount = additionalEventsAmount.add(logEntry.get("dividend").getAsBigDecimal());

                } else if ("bank_profit".equals(type)) {// type_no == 32785
                    if (logEntry.has("node") && logEntry.get("node").getAsInt() != node) {
                        log.debug("bank profit for different node");
                    } else {
                        final BigDecimal logEntryProfit = logEntry.get("profit").getAsBigDecimal();
                        additionalEventsAmount = additionalEventsAmount.add(logEntryProfit);
                        if (logEntry.has("fee")) {
                            final BigDecimal logEntryFee = logEntry.get("fee").getAsBigDecimal();
                            additionalEventsAmount = additionalEventsAmount.subtract(logEntryFee);
                        }
                    }
                } else {
                    Assert.fail(assertReasonBuilder.msg(String.format("Unexpected event type: %s", type)).build());
                }
            }
        }

        return additionalEventsAmount;
    }

    private LogEventTimestamp getLastTransferEventTimestamp(UserData sender, long transferTime) {
        String getLogResponse = FunctionCaller.getInstance().getLog(sender, transferTime);
        LogChecker logChecker = new LogChecker(getLogResponse);
        JsonArray logArray = logChecker.getLogArray();

        // look for transfer events, all
        boolean isTransferFound = false;
        int eventsCount = 0;
        for (JsonElement je : logArray) {
            final JsonObject logEntry = je.getAsJsonObject();
            String type = logEntry.get("type").getAsString();
            long transferTimeLog = logEntry.get("time").getAsLong();
            if (transferTime != transferTimeLog) {
                // very rarely time of transfer event in log is different than returned in transfer response
                // it this case transferTime must be updated as well as eventsCount
                transferTime = transferTimeLog;
                eventsCount = 0;
            }

            if (type.matches(REGEX_TRANSFER_TYPE)) {
                isTransferFound = true;
            } else {
                if (isTransferFound) {
                    break;
                }
            }
            ++eventsCount;
        }

        return new LogEventTimestamp(transferTime, eventsCount);
    }

    @When("^sender sends all to receiver \\(fee is(.*)included\\)$")
    public void send_all(String included) {
        if ("not".equals(included.trim())) {
            // when fee is not included transfer will not be successful
            try {
                send_ads(txSender.getStartBalance().toString(), null, null);
            } catch (TransferBlockedDueToUpcomingDormantFeeException e) {
                log.error("Transaction blocked due to upcoming dormant fee");
            }
        } else {
            UserData sender = txSender.getUserData();
            String senderAddress = sender.getAddress();
            String receiverAddress = txReceivers.get(0).getUserData().getAddress();

            // minimal account balance after transfer
            BigDecimal minAccountBalance = sender.getMinAllowedBalance();
            // subtraction, because after transfer balance cannot be lesser than minimal allowed
            BigDecimal availableAmount = txSender.getStartBalance().subtract(minAccountBalance);
            BigDecimal txAmount = getMaximalTxAmount(senderAddress, receiverAddress, availableAmount);

            try {
                send_ads(txAmount.toString(), null, null);
            } catch (TransferBlockedDueToUpcomingDormantFeeException e) {
                log.info("Transaction blocked due to upcoming dormant fee. Will be tried again");
                EscUtils.waitForNextBlock();

                // reset sender state
                LogChecker logChecker = new LogChecker();
                logChecker.setResp(FunctionCaller.getInstance().getLog(txSender.getUserData()));
                txSender.setStartBalance(logChecker.getBalanceFromAccountObject());
                txSender.setLastEventTimestamp(logChecker.getLastEventTimestamp().incrementEventNum());

                // send again
                availableAmount = txSender.getStartBalance().subtract(minAccountBalance);
                txAmount = getMaximalTxAmount(senderAddress, receiverAddress, availableAmount);
                try {
                    send_ads(txAmount.toString(), null, null);
                } catch (TransferBlockedDueToUpcomingDormantFeeException ex) {
                    log.error("Transaction blocked due to upcoming dormant fee");
                }
            }
        }
    }

    private BigDecimal getMaximalTxAmount(String senderAddress, String receiverAddress, BigDecimal availableAmount) {
        BigDecimal feeCoefficient;
        if (UserData.isAccountFromSameNode(senderAddress, receiverAddress)) {
            feeCoefficient = BigDecimal.ONE.add(EscConst.LOCAL_TX_FEE_COEFFICIENT);
        } else {
            feeCoefficient = BigDecimal.ONE.add(EscConst.LOCAL_TX_FEE_COEFFICIENT).add(EscConst.REMOTE_TX_FEE_COEFFICIENT);
        }
        // approximated value of maximal transfer amount
        BigDecimal txAmount = availableAmount.divide(feeCoefficient, 11, RoundingMode.FLOOR);

        // increase transfer amount and check if it can be done
        BigDecimal expectedBalance;
        BigDecimal minAmount = new BigDecimal("0.00000000001");
        do {
            txAmount = txAmount.add(minAmount);
            expectedBalance = availableAmount.subtract(txAmount).subtract(getTransferFee(senderAddress, receiverAddress, txAmount));
        } while (expectedBalance.compareTo(BigDecimal.ZERO) >= 0);
        return txAmount.subtract(minAmount);
    }

    @When("^wait for balance update")
    public void wait_for_balance_update() {
        log.debug("wait for balance update :start");
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        BigDecimal balanceFromLogArray;

        // set of receiver index in txReceivers array that need to be checked
        Set<Integer> receiverIds = new HashSet<>(txReceivers.size());
        for (int i = 0; i < txReceivers.size(); i++) {
            receiverIds.add(i);
        }

        LogChecker logChecker = new LogChecker();
        // max block delay for account balance update after successful remote transfer
        int attempt;
        final int attemptMax = 50;
        final long delay = EscConst.BLOCK_PERIOD_MS / 10;
        for (attempt = 0; attempt < attemptMax; attempt++) {

            for (int i = 0; i < txReceivers.size(); i++) {
                if (!receiverIds.contains(i)) {
                    // current user received transfer
                    continue;
                }

                TransferUser txReceiver = txReceivers.get(i);
                TransferData transferData = txReceiver.getTransferData();
                /*
                Null transfer means that test is for incorrect transfer.
                In that case waiting time for balance update is extended to maximum. Early break is disabled.
                 */
                if (transferData != null) {
                    BigDecimal txAmountIn = transferData.getAmount();

                    UserData receiverData = txReceiver.getUserData();
                    LogEventTimestamp ts = txReceiver.getLastEventTimestamp();
                    resp = fc.getLog(receiverData, ts);
                    logChecker.setResp(resp);

                    LogFilter lf;
                    lf = new LogFilter(true);
                    lf.addFilter("type", "send_one|send_many");
                    lf.addFilter("amount", txAmountIn.toPlainString());
                    balanceFromLogArray = logChecker.getBalanceFromLogArray(lf);

                    if (txAmountIn.compareTo(balanceFromLogArray) == 0) {
                        // transfer received
                        receiverIds.remove(i);
                    }
                } else {
                    //skip check for cancelled transfer after two blocks
                    if (attempt > 20) {
                        receiverIds.remove(i);
                    }
                }
            }

            if (receiverIds.isEmpty()) {
                // all users received transfer
                break;
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted");
                log.error(e.toString());
            }
            log.debug("");
            log.debug("period delay: {}", attempt + 1);
            log.debug("");
        }

        if (attempt == attemptMax) {
            log.debug("Balance was not updated in expected time ({} block periods)", attempt);
        } else {
            log.debug("Balance was updated in expected time ({} block periods)", attempt + 1);
        }

        log.debug("wait for balance update :end");
    }

    @Then("^sender balance is as expected( \\(changed by amount and fee\\))?$")
    public void check_balance_exp_sender(String change) {
        boolean isChangeExpected = change != null;
        FunctionCaller fc = FunctionCaller.getInstance();

        UserData sender = txSender.getUserData();
        LogEventTimestamp senderLastEventTs = txSender.getLastEventTimestamp();
        String resp = fc.getLog(sender, senderLastEventTs);
        LogChecker logChecker = new LogChecker(resp);

        // update expected balance
        BigDecimal balance = logChecker.getBalanceFromAccountObject();
        BigDecimal balanceFromLog = logChecker.getBalanceFromLogArray();
        BigDecimal senderExpBalance = txSender.getExpBalance();
        log.debug("balance           {} : sender", balance);
        log.debug("balanceFromLog    {} : sender", balanceFromLog);
        log.debug("balanceExpected 1 {} : sender", senderExpBalance);
        senderExpBalance = senderExpBalance.add(balanceFromLog);
        log.debug("balanceExpected 2 {} : sender", senderExpBalance);
        txSender.setExpBalance(senderExpBalance);

        AssertReason.Builder ar = new AssertReason.Builder()
                .req(fc.getLastRequest()).res(fc.getLastResponse())
                .msg("Sender " + txSender.getUserData().getAddress());

        if (isChangeExpected) {
            if (balance.compareTo(txSender.getStartBalance()) == 0) {
                Assert.fail(ar.msg("Sender balance unchanged.").build());
            }
        }
        assertThat(ar.msg("Sender balance unexpected.").build(), balance, comparesEqualTo(senderExpBalance));
    }

    @Then("^receiver balance is as expected( \\(changed by amount\\))?$")
    public void check_balance_exp_receiver(String change) {
        boolean isChangeExpected = change != null;

        FunctionCaller fc = FunctionCaller.getInstance();
        LogChecker logChecker = new LogChecker();

        // update expected balance
        String resp;
        BigDecimal balance;
        BigDecimal balanceFromLog;
        for (int i = 0; i < txReceivers.size(); i++) {
            TransferUser txReceiver = txReceivers.get(i);
            UserData receiverData = txReceiver.getUserData();
            LogEventTimestamp ts = txReceiver.getLastEventTimestamp();
            resp = fc.getLog(receiverData, ts);
            logChecker.setResp(resp);
            balance = logChecker.getBalanceFromAccountObject();

            TransferData transferData = txReceiver.getTransferData();
            if (transferData != null) {
                BigDecimal txAmountIn = transferData.getAmount();
                LogFilter lf;
                lf = new LogFilter(false);
                lf.addFilter("type", "send_one|send_many");
                lf.addFilter("amount", txAmountIn.toPlainString());
                balanceFromLog = logChecker.getBalanceFromLogArray(lf);
            } else {
                balanceFromLog = logChecker.getBalanceFromLogArray();
            }

            BigDecimal receiverExpBalance = txReceiver.getExpBalance();
            log.debug("balance           {} : receiver{}", balance, i);
            log.debug("balanceFromLog    {} : receiver{}", balanceFromLog, i);
            log.debug("balanceExpected 1 {} : receiver{}", receiverExpBalance, i);
            receiverExpBalance = receiverExpBalance.add(balanceFromLog);
            log.debug("balanceExpected 2 {} : receiver{}", receiverExpBalance, i);
            txReceiver.setExpBalance(receiverExpBalance);


            if (isChangeExpected) {
                AssertReason.Builder ar = new AssertReason.Builder()
                        .req(fc.getLastRequest()).res(fc.getLastResponse())
                        .msg("Receiver " + txReceiver.getUserData().getAddress())
                        .msg("Receiver balance unchanged.");
                assertThat(ar.build(), balance, not(comparesEqualTo(txReceiver.getStartBalance())));
            }

            AssertReason.Builder ar = new AssertReason.Builder()
                    .req(fc.getLastRequest()).res(fc.getLastResponse())
                    .msg("Receiver " + txReceiver.getUserData().getAddress())
                    .msg("Receiver balance unexpected.");
            assertThat(ar.build(), balance, comparesEqualTo(receiverExpBalance));
        }
    }

    @Then("^receiver can read message$")
    public void receiver_can_read_message() {

        FunctionCaller fc = FunctionCaller.getInstance();
        TransferUser txReceiver = txReceivers.get(0);
        String resp = fc.getLog(txReceiver.getUserData(), txReceiver.getLastEventTimestamp());
        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "send_one");
        LogChecker lc = new LogChecker(resp);
        JsonArray arr = lc.getFilteredLogArray(lf);
        String receivedMessage = arr.get(0).getAsJsonObject().get("message").getAsString();

        String reason = new AssertReason.Builder().msg("Invalid message.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, receivedMessage, equalTo(message));
        log.debug("read message {}", receivedMessage);
    }

    @When("^sender sends many transfers with (\\d+) ADS amount$")
    public void sender_sends_many_transfers_with_invalid_amount(String amount) {
        Map<String, String> map = new HashMap<>(txReceivers.size());
        for (TransferUser txReceiver : txReceivers) {
            map.put(txReceiver.getUserData().getAddress(), amount);
        }

        String resp = FunctionCaller.getInstance().sendMany(new SendManyTransaction(txSender.getUserData(), map));
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String errorDesc = o.has("error") ? o.get("error").getAsString() : "null";
        String reason = new AssertReason.Builder().req(FunctionCaller.getInstance().getLastRequest()).res(resp)
                .msg("Unexpected error for send multiple transfers with amount: " + amount).build();

        int amountAsInt = Integer.parseInt(amount);
        String expectedErrorDesc = (amountAsInt > 0) ? EscConst.Error.TOO_LOW_BALANCE
                : EscConst.Error.AMOUNT_MUST_BE_POSITIVE;
        assertThat(reason, errorDesc, equalTo(expectedErrorDesc));

        isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(o);
    }

    @When("^sender sends many transfers to single receiver$")
    public void sender_sends_many_transfers_to_single_receiver() {
        String[] data = new String[]{"00FF-00000000-XXXX", "0.00000000001"};

        List<String[]> recList = new ArrayList<>(2);
        for (int j = 0; j < 2; j++) {
            recList.add(data);
        }

        String resp = FunctionCaller.getInstance().sendMany(new SendManyTransaction(txSender.getUserData(), recList));
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String errorDesc = o.has("error") ? o.get("error").getAsString() : "null";
        String reason = new AssertReason.Builder().req(FunctionCaller.getInstance().getLastRequest()).res(resp)
                .msg("Unexpected error for send multiple transfers to single receiver.").build();
        assertThat(reason, errorDesc, equalTo(EscConst.Error.DUPLICATED_TARGET));

        isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(o);
    }

    @When("^sender sends many transfers which sum exceeds balance$")
    public void sender_sends_many_transfers_which_sum_exceeds_balance() {
        /*
        Sends to multiple recipents.
        Each wire amount is less than balance (0.6 of balance), but sum of wires exceeds balance.
         */
        BigDecimal balance = txSender.getStartBalance();
        BigDecimal amount = balance.multiply(new BigDecimal("0.6")).setScale(11, RoundingMode.FLOOR);
        String amountAsString = amount.toPlainString();

        Map<String, String> map = new HashMap<>(txReceivers.size());
        for (TransferUser txReceiver : txReceivers) {
            map.put(txReceiver.getUserData().getAddress(), amountAsString);
        }

        String resp = FunctionCaller.getInstance().sendMany(new SendManyTransaction(txSender.getUserData(), map));
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String errorDesc = o.has("error") ? o.get("error").getAsString() : "null";
        String reason = new AssertReason.Builder().req(FunctionCaller.getInstance().getLastRequest()).res(resp)
                .msg("Unexpected error for send multiple transfers with amount: " + amount).build();
        assertThat(reason, errorDesc, equalTo(EscConst.Error.TOO_LOW_BALANCE));

        isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(o);
    }


    @When("^sender sends transfer in which message length is incorrect$")
    public void sender_sends_transfer_incorrect_msg_length() {
        SendOneTransaction command = new SendOneTransaction(
                txSender.getUserData(),
                "00FF-00000000-XXXX",
                "0.00000000001"
        );
        command.setMessage("ABCD");
        String resp = FunctionCaller.getInstance().sendOne(command);
        JsonObject o = Utils.convertStringToJsonObject(resp);

        String errorDesc = o.has("error") ? o.get("error").getAsString() : "null";
        String reason = new AssertReason.Builder().req(FunctionCaller.getInstance().getLastRequest()).res(resp)
                .msg("Unexpected error for incorrect message length.").build();
        assertThat(reason, errorDesc, equalTo(EscConst.Error.COMMAND_PARSE_ERROR));

        isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(o);
    }

    @Then("^transfer is rejected$")
    public void transfer_is_rejected() {
        if (isTransactionAccepted) {
            FunctionCaller fc = FunctionCaller.getInstance();
            String reason = new AssertReason.Builder().msg("Transaction is accepted.")
                    .req(fc.getLastRequest()).res(fc.getLastResponse()).build();

            assertThat(reason, !isTransactionAccepted);
        }
    }

    /**
     * Returns transfer fee in tokens.
     *
     * @param senderAddress   sender address
     * @param receiverAddress receiver address
     * @param amount          transfer amount
     * @return transfer fee in tokens
     */
    private BigDecimal getTransferFee(String senderAddress, String receiverAddress, BigDecimal amount) {
        Map<String, String> receiverMap = new HashMap<>(1);
        receiverMap.put(receiverAddress, amount.toPlainString());
        return getTransferFee(senderAddress, receiverMap);
    }

    /**
     * Returns transfer fee in tokens.
     *
     * @param senderAddress sender address
     * @param receiverMap   map of receiver - amount pairs
     * @return transfer fee in tokens
     */
    private BigDecimal getTransferFee(String senderAddress, Map<String, String> receiverMap) {
        BigDecimal summaryFee = BigDecimal.ZERO;

        int receiverCount = receiverMap.size();
        for (String receiverAddress : receiverMap.keySet()) {
            BigDecimal amount = new BigDecimal(receiverMap.get(receiverAddress));

            BigDecimal localFee = amount.multiply(
                    (receiverCount == 1) ? EscConst.LOCAL_TX_FEE_COEFFICIENT : EscConst.MULTI_TX_FEE_COEFFICIENT);
            // fee scale must be set, because multiply extends scale
            localFee = localFee.setScale(11, RoundingMode.FLOOR);
            summaryFee = summaryFee.add(localFee);

            if (!UserData.isAccountFromSameNode(senderAddress, receiverAddress)) {
                // users in different nodes
                BigDecimal remoteFee = amount.multiply(EscConst.REMOTE_TX_FEE_COEFFICIENT);
                // fee scale must be set, because multiply extends scale
                remoteFee = remoteFee.setScale(11, RoundingMode.FLOOR);
                summaryFee = summaryFee.add(remoteFee);
            }
        }

        if (receiverCount > 10) {
            // minimum varies when transfers are sent to more than 10 accounts
            summaryFee = EscConst.MIN_MULTI_TX_PER_RECIPIENT.multiply(new BigDecimal(receiverCount)).max(summaryFee);
        } else {
            summaryFee = EscConst.MIN_TX_FEE.max(summaryFee);
        }
        return summaryFee;
    }

    /**
     * Compares transfer amount and computed fee with values returned from send_one function (tx/deduct, tx/fee).
     *
     * @param jsonResp     send_one response as String
     * @param transferData transfer data
     */
    private void checkComputedFeeWithResponse(String jsonResp, TransferData transferData) {
        FunctionCaller fc = FunctionCaller.getInstance();
        JsonObject o = Utils.convertStringToJsonObject(jsonResp);
        o = o.getAsJsonObject("tx");

        BigDecimal fee = o.get("fee").getAsBigDecimal();
        BigDecimal feeExpected = transferData.getFee();
        String reason = new AssertReason.Builder().msg("Invalid transfer fee.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, fee, comparesEqualTo(feeExpected));

        BigDecimal amount = o.get("deduct").getAsBigDecimal().subtract(fee);
        BigDecimal amountExpected = transferData.getAmount();
        reason = new AssertReason.Builder().msg("Invalid transfer amount.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, amount, comparesEqualTo(amountExpected));
    }
}