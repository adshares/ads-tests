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

import com.google.gson.JsonObject;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.DecodeRawCommand;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class TransactionIdChecker {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static TransactionIdChecker instance;
    private static UserData userData;
    private Queue<TransactionData> txQueue = new LinkedList<>();

    private TransactionIdChecker() {
    }

    public static TransactionIdChecker getInstance() {
        if (instance == null) {
            instance = new TransactionIdChecker();
            userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
        }
        return instance;
    }

    public void addTransactionId(String txId) {
        txQueue.add(new TransactionData(txId, null, null));
    }

    void addTransactionId(String txId, String data, String signature) {
        txQueue.add(new TransactionData(txId, data, signature));
    }

    public void check(String txId) {
        log.debug("check txId: " + txId);
        getTransactionObj(txId);
    }

    public void check(TransactionData transactionData) {
        log.debug("check transactionData: " + transactionData.getTxId());
        JsonObject o1 = getTransactionObj(transactionData.getTxId());

        if (transactionData.getData() != null) {
            log.debug("check transactionData: compare with decoded");
            o1 = o1.getAsJsonObject("txn");

            JsonObject o2 = getDecodeRawObj(transactionData);
            Assert.assertEquals("Result from getTransaction and decodeRaw differs.", o1, o2);
        }
    }

    public void checkAll() {
        while (!txQueue.isEmpty()) {
            check(txQueue.remove());
        }
    }

    private JsonObject getTransactionObj(String txId) {
        String resp = FunctionCaller.getInstance().getTransaction(userData, txId);

        JsonObject o = Utils.convertStringToJsonObject(resp);
        if (o.has("error")) {
            Assert.fail("Unexpected error for getTransaction: " + o.get("error").getAsString());
        }
        return o;
    }

    private JsonObject getDecodeRawObj(TransactionData transactionData) {
        DecodeRawCommand command = new DecodeRawCommand(userData, transactionData.getData(), transactionData.getSignature());
        String resp = FunctionCaller.getInstance().decodeRaw(command);

        JsonObject o = Utils.convertStringToJsonObject(resp);
        if (o.has("error")) {
            Assert.fail("Unexpected error for decodeRaw: " + o.get("error").getAsString());
        }
        return o;
    }

    public class TransactionData {
        private String txId;
        private String data;
        private String signature;

        TransactionData(String txId, String data, String signature) {
            this.txId = txId;
            this.data = data;
            this.signature = signature;
        }

        String getTxId() {
            return txId;
        }

        public String getData() {
            return data;
        }

        public String getSignature() {
            return signature;
        }
    }
}
