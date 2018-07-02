package net.adshares.ads.qa.util;

import com.google.gson.JsonObject;
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
