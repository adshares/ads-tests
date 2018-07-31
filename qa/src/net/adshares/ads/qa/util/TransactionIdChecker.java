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
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class TransactionIdChecker {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static TransactionIdChecker instance;
    private Queue<String> txQueue = new LinkedList<>();

    private TransactionIdChecker() {
    }

    public static TransactionIdChecker getInstance() {
        if (instance == null) {
            instance = new TransactionIdChecker();
        }
        return instance;
    }

    public void addTransactionId(String txId) {
        txQueue.add(txId);
    }

    public void check(String txId) {
        log.debug("check txId: " + txId);
        UserData u = UserDataProvider.getInstance().getUserDataList(1).get(0);
        String resp = FunctionCaller.getInstance().getTransaction(u, txId);

        JsonObject o = Utils.convertStringToJsonObject(resp);
        if (o.has("error")) {
            log.error("err: " + o.get("error").getAsString());
        }
    }

    public void checkAll() {
        while (!txQueue.isEmpty()) {
            check(txQueue.remove());
        }
    }
}
