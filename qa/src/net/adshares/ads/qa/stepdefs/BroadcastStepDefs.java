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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;

public class BroadcastStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<UserData> userDataList;
    private Set<BroadcastMessageData> bmdSet;
    private String lastResp;


    @Given("^set of users$")
    public void set_of_users() {
        userDataList = UserDataProvider.getInstance().getUserDataList();
    }

    @When("^one of them sends valid broadcast message which size is (\\d+) byte\\(s\\)$")
    public void one_of_them_sends_broadcast_message(int messageSize) {
        UserData u = userDataList.get(0);

        BroadcastMessageData bmd = sendBroadcastMessageData(u, messageSize);
        bmdSet = new HashSet<>();
        bmdSet.add(bmd);
    }

    @When("^one of them sends broadcast message which size is (\\d+) bytes$")
    public void one_of_them_sends_broadcast_message_len(int messageSize) {
        UserData u = userDataList.get(0);
        FunctionCaller fc = FunctionCaller.getInstance();
        lastResp = fc.broadcast(u, EscUtils.generateMessage(messageSize));
    }

    @When("^one of them sends many broadcast messages$")
    public void one_of_them_send_many_broadcast_messages() {
        UserData u = userDataList.get(0);

        int messageSize = 1;
        bmdSet = new HashSet<>();
        do {
            BroadcastMessageData bmd = sendBroadcastMessageData(u, messageSize);
            bmdSet.add(bmd);

            messageSize *= 2;
        } while (messageSize <= EscConst.BROADCAST_MESSAGE_MAX_SIZE);
    }

    /**
     * Broadcast random message.
     *
     * @param userData    user data
     * @param messageSize size of message in bytes
     * @return BroadcastMessageData object
     */
    private BroadcastMessageData sendBroadcastMessageData(UserData userData, int messageSize) {
        FunctionCaller fc = FunctionCaller.getInstance();

        String message = EscUtils.generateMessage(messageSize);
        BigDecimal feeExpected = getBroadcastFee(message);

        String resp = fc.broadcast(userData, message);
        AssertReason.Builder ar = new AssertReason.Builder().msg("Broadcast transaction was not accepted by node.")
                .req(fc.getLastRequest()).res(fc.getLastResponse());
        assertThat(ar.build(), EscUtils.isTransactionAcceptedByNode(resp));

        JsonObject o = Utils.convertStringToJsonObject(resp);
        BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
        BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
        int blockTimeInt = o.get("current_block_time").getAsInt();
        String blockTime = Integer.toHexString(blockTimeInt);
        log.debug("block time:  {}", blockTime);

        log.debug("deduct:         {}", deduct.toPlainString());
        log.debug("fee:         {}", fee.toPlainString());
        log.debug("feeExpected: {}", feeExpected.toPlainString());
        log.debug("diff:        {}", feeExpected.subtract(fee).toPlainString());

        String reason;

        reason = new AssertReason.Builder().msg("Deduct is not equal to fee.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, deduct, comparesEqualTo(fee));

        reason = new AssertReason.Builder().msg("Unexpected fee for broadcast message.")
                .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
        assertThat(reason, fee, comparesEqualTo(feeExpected));

        return new BroadcastMessageData(message, blockTime);
    }

    @Then("^all of them can read it$")
    public void all_of_them_can_read_it() {
        FunctionCaller fc = FunctionCaller.getInstance();
        // waiting time for message in block periods
        int delay = 0;
        // maximum waiting time for message in block periods
        int delayMax = 2;

        for (UserData u : userDataList) {

            /*
            Temporary list of broadcast messages is created for each user.

            First message is taken from this list and is looked up in get_broadcast response.
            During search all received messages are compared with list.
            If there is match, message is removed from list.

            This algorithm reduces get_broadcast calls.
             */
            List<BroadcastMessageData> userBmdList = new LinkedList<>(bmdSet);
            do {
                BroadcastMessageData bmd = userBmdList.get(0);

                String message = bmd.getMessage();
                String blockTime = bmd.getBlockTime();

                boolean isMessageReceived = false;
                int nextBlockCheckAttempt = 0;
                int nextBlockCheckAttemptMax = 2;
                String resp;
                JsonObject o;

                boolean isError;
                do {
                    resp = fc.getBroadcast(u, blockTime);
                    o = Utils.convertStringToJsonObject(resp);

                    isError = o.has("error");
                    if (isError) {
                        String err = o.get("error").getAsString();
                        log.debug("get_broadcast error: {}", err);

                        if (EscConst.Error.BROADCAST_NOT_READY.equals(err)) {
                            Assert.assertTrue("Broadcast wasn't prepared in expected time.", delay < delayMax);
                            log.debug("wait another block");
                            waitForBlock();
                            delay++;
                        } else {
                            Assert.fail("Unexpected error message: " + err);
                        }
                    } else {
                        int broadcastCount = o.get("broadcast_count").getAsInt();
                        log.debug("get_broadcast count: {}", broadcastCount);
                        if (broadcastCount > 0) {
                            JsonElement el = o.get("broadcast");
                            if (el != null && el.isJsonArray()) {
                                JsonArray broadcastArr = (JsonArray) el;
                                int size = broadcastArr.size();
                                log.debug("size {}", size);
                                for (int i = 0; i < size; i++) {
                                    String receivedMessage = broadcastArr.get(i).getAsJsonObject().get("message").getAsString();

                                    Iterator<BroadcastMessageData> it = userBmdList.iterator();
                                    while (it.hasNext()) {
                                        String otherMessage = it.next().getMessage();
                                        if (otherMessage.equals(receivedMessage)) {
                                            log.debug("got message: {}", receivedMessage);
                                            it.remove();

                                            if (message.equals(receivedMessage)) {
                                                log.debug("received message");
                                                isMessageReceived = true;
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else {
                                String reason = new AssertReason.Builder().msg("Invalid 'broadcast' field.")
                                        .req(fc.getLastRequest()).res(resp).build();
                                Assert.fail(reason);
                            }
                        }

                        if (!isMessageReceived) {
                            // broadcast messages can be split into different block,
                            // therefore next block must be checked
                            Assert.assertTrue("Broadcast message wasn't found in further blocks.",
                                    nextBlockCheckAttempt < nextBlockCheckAttemptMax);
                            log.debug("check next block");
                            blockTime = EscUtils.getNextBlock(blockTime);
                            nextBlockCheckAttempt++;
                        }

                    }
                } while (!isMessageReceived);

            } while (userBmdList.size() > 0);

        }
    }

    private void waitForBlock() {
        try {
            Thread.sleep(EscConst.BLOCK_PERIOD_MS);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted");
            log.error(e.toString());
        }
    }

    @Then("^message is rejected$")
    public void message_is_rejected() {
        JsonObject o = Utils.convertStringToJsonObject(lastResp);
        if (o.has("error")) {
            String err = o.get("error").getAsString();
            log.debug("Message was rejected with error: {}", err);
        } else {
            FunctionCaller fc = FunctionCaller.getInstance();
            String reason = new AssertReason.Builder().msg("Message was accepted.")
                    .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
            assertThat(reason, !EscUtils.isTransactionAcceptedByNode(o));
            log.debug("Message was rejected: not accepted by node");
        }
    }

    /**
     * Calculates fee for broadcasting message.
     *
     * @param message message
     * @return fee for broadcasting message
     */
    private BigDecimal getBroadcastFee(String message) {
        int len = message.length();

        assertThat("Not even length of message. Current length = " + len, len % 2 == 0);
        int sizeBytes = len / 2;

        BigDecimal fee = EscConst.MIN_TX_FEE;
        if (sizeBytes > 32) {
            fee = fee.add(EscConst.BROADCAST_FEE_PER_BYTE.multiply(new BigDecimal(sizeBytes - 32)));
        }
        return fee;
    }

    class BroadcastMessageData {
        private String message;
        private String blockTime;

        BroadcastMessageData(String message, String blockTime) {
            this.message = message;
            this.blockTime = blockTime;
        }

        String getMessage() {
            return message;
        }

        String getBlockTime() {
            return blockTime;
        }
    }
}
