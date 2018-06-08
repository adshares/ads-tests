package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.util.*;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Cucumber steps definitions for transfer tests
 */
public class TransferStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * Regexp for transfer event in log
     */
    private static final String REGEX_TRANSFER_TYPE = "send_one|send_many";

    private TransferUser txSender;
    private List<TransferUser> txReceivers;


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

        Assert.assertNotEquals("No user with positive balance.", maxBalanceIndex, -1);

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

    @When("^sender sends ([-]?\\d+(\\.\\d+)?) ADST to receiver[s]?$")
    public void send_adst(String txAmount, String decimalPart) {
        FunctionCaller fc = FunctionCaller.getInstance();
        UserData sender = txSender.getUserData();
        String senderAddress = sender.getAddress();

        int receiversCount = txReceivers.size();
        // amount to single receiver
        BigDecimal amount = new BigDecimal(txAmount).setScale(11, BigDecimal.ROUND_FLOOR);
        // amount from sender to all receivers
        BigDecimal amountOut = amount.multiply(new BigDecimal(receiversCount)).setScale(11, BigDecimal.ROUND_FLOOR);
        // transfer fee payed by sender
        BigDecimal fee;
        String jsonResp;
        if (receiversCount > 1) {
            // send many
            Map<String, String> map = new HashMap<>(receiversCount);
            for (TransferUser txReceiver : txReceivers) {
                map.put(txReceiver.getUserData().getAddress(), txAmount);
            }
            jsonResp = fc.sendMany(sender, map);
            fee = getTransferFee(senderAddress, map);
        } else {
            // send one
            UserData receiver = txReceivers.get(0).getUserData();
            String receiverAddress = receiver.getAddress();

            jsonResp = fc.sendOne(sender, receiverAddress, txAmount);
            fee = getTransferFee(senderAddress, receiverAddress, amount);
        }
        boolean isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(jsonResp);
        if (isTransactionAccepted) {
            JsonObject o = Utils.convertStringToJsonObject(jsonResp);
            o = o.getAsJsonObject("account");
            BigDecimal balanceBeforeTransfer = o.get("balance").getAsBigDecimal();
            long transferTime = o.get("time").getAsLong();

            o = Utils.convertStringToJsonObject(fc.getLog(sender, transferTime));
            JsonArray logArr = o.getAsJsonArray("log");

            // look for transfer events, all
            boolean isTransferFound = false;
            int eventsCount = 0;
            for (JsonElement je : logArr) {
                String type = je.getAsJsonObject().get("type").getAsString();

                if (type.matches(REGEX_TRANSFER_TYPE)) {
                    isTransferFound = true;
                } else {
                    if (isTransferFound) {
                        break;
                    }
                }
                ++eventsCount;
            }

            txSender.setStartBalance(balanceBeforeTransfer);
            LogEventTimestamp lastEventTimestamp = new LogEventTimestamp(transferTime, eventsCount);
            txSender.setLastEventTimestamp(lastEventTimestamp.incrementEventNum());
        }

        BigDecimal senderBalance = txSender.getStartBalance();
        BigDecimal tmpSenderExpBalance = senderBalance.subtract(amountOut).subtract(fee);
        BigDecimal minAccountBalance = sender.getMinAllowedBalance();
        // check, if transfer is possible and balance won't be bigger after transfer
        if (tmpSenderExpBalance.compareTo(minAccountBalance) >= 0 && tmpSenderExpBalance.compareTo(senderBalance) < 0) {
            // update balances, if transfer is possible
            Assert.assertTrue("transfer was not accepted by node", isTransactionAccepted);

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
            log.info("Cannot transfer funds");
            log.info("\tbalance: {}", senderBalance);
            log.info("\t amount: {}", txAmount);
            log.info("\t    fee: {}", fee);

            Assert.assertFalse("transfer was accepted by node", isTransactionAccepted);

            for (TransferUser txReceiver : txReceivers) {
                txReceiver.setExpBalance(txReceiver.getStartBalance());
            }
            txSender.setExpBalance(senderBalance);
        }

    }

    @When("^sender sends all to receiver \\(fee is(.*)included\\)$")
    public void send_all(String included) {
        if ("not".equals(included.trim())) {
            // when fee is not included transfer will not be successful
            send_adst(txSender.getStartBalance().toString(), null);
        } else {
            UserData sender = txSender.getUserData();
            String senderAddress = sender.getAddress();
            String receiverAddress = txReceivers.get(0).getUserData().getAddress();

            // minimal account balance after transfer
            BigDecimal minAccountBalance = sender.getMinAllowedBalance();
            // subtraction, because after transfer balance cannot be lesser than minimal allowed
            BigDecimal availableAmount = txSender.getStartBalance().subtract(minAccountBalance);

            // calculate approx value of maximal transfer amount
            BigDecimal feeCoefficient;
            // check if same node
            if (UserData.isAccountFromSameNode(senderAddress, receiverAddress)) {
                feeCoefficient = BigDecimal.ONE.add(EscConst.LOCAL_TX_FEE_COEFFICIENT);
            } else {
                feeCoefficient = BigDecimal.ONE.add(EscConst.LOCAL_TX_FEE_COEFFICIENT).add(EscConst.REMOTE_TX_FEE_COEFFICIENT);
            }
            BigDecimal txAmount = availableAmount.divide(feeCoefficient, 11, BigDecimal.ROUND_FLOOR);

            // increase transfer amount and check if it can be done
            BigDecimal expBalance;
            BigDecimal minAmount = new BigDecimal("0.00000000001");
            log.info("txAmount  pre: {}", txAmount);
            do {
                txAmount = txAmount.add(minAmount);
                log.info("txAmount  ins: {}", txAmount);
                expBalance = availableAmount.subtract(txAmount).subtract(getTransferFee(senderAddress, receiverAddress, txAmount));
            } while (expBalance.compareTo(BigDecimal.ZERO) >= 0);
            txAmount = txAmount.subtract(minAmount);
            log.info("txAmount post: {}", txAmount);

            send_adst(txAmount.toString(), null);
        }
    }

    @When("^wait for balance update")
    public void wait_for_balance_update() {
        log.info("wait for balance update :start");
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        BigDecimal balanceRead;
        BigDecimal balanceFromLogArray;

        // set of receiver index in txReceivers array that need to be checked
        Set<Integer> receiverIds = new HashSet<>(txReceivers.size());
        for (int i = 0; i < txReceivers.size(); i++) {
            receiverIds.add(i);
        }

        LogChecker logChecker = new LogChecker();
        // max block delay for account balance update after successful remote transfer
        int MAX_BLOCK_DELAY = 50;
        int blockDelay;
        for (blockDelay = 0; blockDelay < MAX_BLOCK_DELAY; blockDelay++) {

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
                }
            }

            if (receiverIds.isEmpty()) {
                // all users received transfer
                break;
            }

            sleepOneBlock();
            log.info("");
            log.info("block period delay: {}", blockDelay + 1);
            log.info("");
        }

        if (blockDelay == MAX_BLOCK_DELAY) {
            log.info("Balance was not updated in expected time ({} block periods)", blockDelay);
        } else {
            log.info("Balance was updated in expected time ({} block periods)", blockDelay + 1);
        }


        for (int i = 0; i < txReceivers.size(); i++) {
            TransferUser txReceiver = txReceivers.get(i);
            UserData receiverData = txReceiver.getUserData();
            LogEventTimestamp ts = txReceiver.getLastEventTimestamp();
            resp = fc.getLog(receiverData, ts);
            logChecker.setResp(resp);
            balanceRead = logChecker.getBalanceFromAccountObject();

            TransferData transferData = txReceiver.getTransferData();
            if (transferData != null) {
                BigDecimal txAmountIn = transferData.getAmount();
                LogFilter lf;
                lf = new LogFilter(false);
                lf.addFilter("type", "send_one|send_many");
                lf.addFilter("amount", txAmountIn.toPlainString());
                balanceFromLogArray = logChecker.getBalanceFromLogArray(lf);
            } else {
                balanceFromLogArray = logChecker.getBalanceFromLogArray();
            }

            BigDecimal receiverExpBalance = txReceiver.getExpBalance();
            log.info("balanceFromLog      {} : receiver{}", balanceRead, i);
            log.info("balanceFromLogArray {} : receiver{}", balanceFromLogArray, i);
            log.info("balanceExpected 1   {} : receiver{}", receiverExpBalance, i);
            receiverExpBalance = receiverExpBalance.add(balanceFromLogArray);
            log.info("balanceExpected 2   {} : receiver{}", receiverExpBalance, i);
            txReceiver.setExpBalance(receiverExpBalance);
        }


        UserData sender = txSender.getUserData();
        LogEventTimestamp senderLastEventTs = txSender.getLastEventTimestamp();
        resp = fc.getLog(sender, senderLastEventTs);
        logChecker.setResp(resp);
        balanceRead = logChecker.getBalanceFromAccountObject();
        balanceFromLogArray = logChecker.getBalanceFromLogArray();
        BigDecimal senderExpBalance = txSender.getExpBalance();
        log.info("balanceFromLog      {} : sender", balanceRead);
        log.info("balanceFromLogArray {} : sender", balanceFromLogArray);
        log.info("balanceExpected 1   {} : sender", senderExpBalance);
        senderExpBalance = senderExpBalance.add(balanceFromLogArray);
        log.info("balanceExpected 2   {} : sender", senderExpBalance);
        txSender.setExpBalance(senderExpBalance);

        log.info("wait for balance update :end");
    }

    private void sleepOneBlock() {
        log.info("sleepOneBlock");
        try {
            Thread.sleep(100L * EscConst.BLOCK_PERIOD);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted");
            log.error(e.toString());
        }
    }



    @Then("^receiver balance is increased by sent amount$")
    public void check_balance_chg_receiver() {
        FunctionCaller fc = FunctionCaller.getInstance();
        for (TransferUser receiver : txReceivers) {
            BigDecimal receiverExpBalance = receiver.getExpBalance();
            Assert.assertNotEquals("Receiver balance unchanged.", receiverExpBalance, receiver.getStartBalance());
            Assert.assertEquals("Receiver balance unexpected.", receiverExpBalance, fc.getUserAccountBalance(receiver));
        }
    }

    @Then("^sender balance is decreased by sent amount and fee$")
    public void check_balance_chg_sender() {
        BigDecimal senderExpBalance = txSender.getExpBalance();
        Assert.assertNotEquals("Sender balance unchanged.", senderExpBalance, txSender.getStartBalance());
        FunctionCaller fc = FunctionCaller.getInstance();
        Assert.assertEquals("Sender balance unexpected.", senderExpBalance, fc.getUserAccountBalance(txSender));
    }

    @Then("^sender balance is as expected$")
    public void check_balance_exp_sender() {
        FunctionCaller fc = FunctionCaller.getInstance();
        Assert.assertEquals("Sender balance unexpected.", txSender.getExpBalance(), fc.getUserAccountBalance(txSender));
    }

    @Then("^receiver balance is as expected$")
    public void check_balance_exp_receiver() {
        FunctionCaller fc = FunctionCaller.getInstance();
        for (TransferUser receiver : txReceivers) {
            Assert.assertEquals("Receiver balance unexpected.", receiver.getExpBalance(), fc.getUserAccountBalance(receiver));
        }
    }

    @Given("user log")
    public void user_log() {
        FunctionCaller fc = FunctionCaller.getInstance();
        LogChecker lc = new LogChecker();
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        for (UserData user : userDataList) {
            String userLog = fc.getLog(user);

            log.debug(user.getAddress());
            lc.setResp(userLog);
            Assert.assertTrue("Balance is different than sum of logged events", lc.isBalanceFromObjectEqualToArray());
            log.debug("success");
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
            localFee = localFee.setScale(11, BigDecimal.ROUND_FLOOR);
            summaryFee = summaryFee.add(localFee);

            if (!UserData.isAccountFromSameNode(senderAddress, receiverAddress)) {
                // users in different nodes
                BigDecimal remoteFee = amount.multiply(EscConst.REMOTE_TX_FEE_COEFFICIENT);
                // fee scale must be set, because multiply extends scale
                remoteFee = remoteFee.setScale(11, BigDecimal.ROUND_FLOOR);
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
        JsonObject o = Utils.convertStringToJsonObject(jsonResp);
        o = o.getAsJsonObject("tx");

        BigDecimal fee = o.get("fee").getAsBigDecimal();
        BigDecimal amount = o.get("deduct").getAsBigDecimal().subtract(fee);

        Assert.assertEquals("Invalid transfer amount.", amount, transferData.getAmount());
        Assert.assertEquals("Invalid transfer fee.", fee, transferData.getFee());
    }

}