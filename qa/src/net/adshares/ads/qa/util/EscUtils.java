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

package net.adshares.ads.qa.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.data.UserData;

import javax.xml.bind.DatatypeConverter;
import java.util.Random;

public class EscUtils {
    /**
     * Maximum message size in bytes
     * <p>
     * Message size is limited by maximum String length. Every byte is encoded as two chars.
     */
    private static final int MESSAGE_SIZE_MAX = Integer.MAX_VALUE / 2 + 1;

    /**
     * Checks, if node accepted transaction.
     *
     * @param jsonObject response from function (eg. send_one, send_many) as JSONObject
     * @return true, if transfer was accepted by node, false otherwise
     */
    public static boolean isTransactionAcceptedByNode(JsonObject jsonObject) {
        if (jsonObject.has("error")) {
            return false;
        }
        jsonObject = jsonObject.getAsJsonObject("tx");
        boolean hasId = jsonObject.has("id");
        if (hasId) {
            TransactionIdChecker.getInstance().addTransactionId(jsonObject.get("id").getAsString());
        }
        return hasId;
    }

    /**
     * Checks, if node accepted transaction.
     *
     * @param jsonResp response from function (eg. send_one, send_many) as String
     * @return true, if transfer was accepted by node, false otherwise
     */
    public static boolean isTransactionAcceptedByNode(String jsonResp) {
        JsonObject o = Utils.convertStringToJsonObject(jsonResp);
        return isTransactionAcceptedByNode(o);
    }

    /**
     * Generates random message.
     *
     * @param size size of message in bytes
     * @return random message, hexadecimal String (without leading '0x', with even number of characters)
     */
    public static String generateMessage(int size) {
        Random random = new Random();
        if (size > MESSAGE_SIZE_MAX) {
            size = MESSAGE_SIZE_MAX;
        }
        byte[] resBuf = new byte[size];
        random.nextBytes(resBuf);
        return DatatypeConverter.printHexBinary(resBuf);
    }

    /**
     * Returns error description or empty String "", if no error is present.
     *
     * @param jsonResp response from function (eg. send_one, send_many) as String
     * @return error description or empty String "", if no error is present
     */
    public static String getErrorDescription(String jsonResp) {
        JsonObject o = Utils.convertStringToJsonObject(jsonResp);
        return o.has("error") ? o.get("error").getAsString() : "";
    }

    /**
     * Returns timestamp of last event in log.
     *
     * @param jsonObject json response for get_log function
     * @return timestamp of last event in log or 0, if log is empty
     */
    public static LogEventTimestamp getLastLogEventTimestamp(JsonObject jsonObject) {
        long timeStamp = 0;
        int eventsCount = 0;
        if (jsonObject.has("log")) {
            JsonElement jsonElementLog = jsonObject.get("log");
            if (jsonElementLog.isJsonArray()) {
                JsonArray arr = jsonElementLog.getAsJsonArray();

                int size = arr.size();
                if (size > 0) {
                    int index = size - 1;
                    JsonObject entry = arr.get(index).getAsJsonObject();
                    timeStamp = entry.get("time").getAsLong();
                    ++eventsCount;

                    while (index > 0) {
                        --index;

                        if (timeStamp != arr.get(index).getAsJsonObject().get("time").getAsLong()) {
                            break;
                        }
                        ++eventsCount;
                    }
                }
            }
        }
        return new LogEventTimestamp(timeStamp, eventsCount);
    }

    /**
     * Returns hex String for next block.
     *
     * @param blockTime hex String
     * @return next block time in hex
     */
    public static String getNextBlock(String blockTime) {
        int time = Integer.parseInt(blockTime, 16);
        time += EscConst.BLOCK_PERIOD;
        return Integer.toHexString(time);
    }

    /**
     * Returns hex String for previous block.
     *
     * @param blockTime hex String
     * @return previous block time in hex
     */
    public static String getPreviousBlock(String blockTime) {
        int time = Integer.parseInt(blockTime, 16);
        time -= EscConst.BLOCK_PERIOD;
        return Integer.toHexString(time);
    }

    /**
     * Gets node status from get_block function response.
     *
     * @param userData user data
     * @param nodeId   node id
     * @return node status, or -1, if error occur
     */
    public static int getNodeStatus(UserData userData, String nodeId) {
        int status = -1;
        String resp = FunctionCaller.getInstance().getBlock(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
        for (JsonElement je : arr) {
            JsonObject nodeEntry = je.getAsJsonObject();
            if (nodeId.equals(nodeEntry.get("id").getAsString())) {
                status = nodeEntry.get("status").getAsInt();
                break;
            }
        }

        return status;
    }

    /**
     * Checks, if status matches vip status.
     *
     * @param status node status
     * @return true if node status matches vip status, false otherwise
     */
    public static boolean isStatusVip(int status) {
        return (status & 2) != 0;
    }

    /**
     * Checks, if account address is valid.
     *
     * @param address String to validate as account address
     * @return true, if account addess is in correct format, false otherwise
     */
    public static boolean isValidAccountAddress(String address) {
        if (address != null && address.length() == 18) {

            if ('-' == address.charAt(4) && '-' == address.charAt(13)) {
                String node = address.substring(0, 4);
                String num = address.substring(5, 13);
                String checksum = address.substring(14);
                try {
                    Integer.valueOf(node, 16);
                    Integer.valueOf(num, 16);
                    if (!"XXXX".equals(checksum)) {
                        Integer.valueOf(checksum, 16);
                    }
                    return true;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Waits for start of next block.
     */
    public static void waitForNextBlock() {
        long timeAfterLastBlock = System.currentTimeMillis() % EscConst.BLOCK_PERIOD_MS;

        try {
            Thread.sleep(EscConst.BLOCK_PERIOD_MS - timeAfterLastBlock);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
