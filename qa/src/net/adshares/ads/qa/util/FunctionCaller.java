package net.adshares.ads.qa.util;

import com.google.gson.*;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.stepdefs.TransferUser;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FunctionCaller {

    /**
     * Timeout for compilation in milliseconds
     */
    private static final int COMPILATION_TIMEOUT = 300000;// 300000 ms = 5 min.
    /**
     * Name of temporary file that is created for commands that cannot be called directly in shell
     */
    private static final String TEMP_FILE_NAME = "tmp";
    private static final String SYSTEM_PROP_IS_DOCKER = "is.docker";
    private static final String SYSTEM_PROP_DATA_DIR = "dir.data";
    private static final String DEFAULT_DATA_DIR = "/ads-data";
    private static final String DOCKER_ADS_BINARY = "docker exec -i adshares_ads_1 ads";
    private static final String ADS_BINARY = "ads";
    /**
     * Regular expression which match when two json responses are returned. It is useful for calls preceded by get_me.
     */
    private static final String DOUBLE_RESP_REGEX = "(?s).*}\\s*\\{";
    /**
     * True, if test are performed on docker.
     * False, if locally.
     */
    private boolean isDocker;
    /**
     * Blockchain client application
     */
    private String clientApp;
    /**
     * Blockchain client application's options
     */
    private String clientAppOpts;
    /**
     * Directory in which node and user data is stored
     */
    private String dataDir;
    private String tmpDir;
    /**
     * If docker is in use, all system commands must be preceded with "docker exec -i <<docker_image>> ".
     * System command is every command, which is not called on esc binary.
     */
    private String sysCmdPrefix;
    /**
     * Last request processed by esc
     */
    private String lastRequest;
    /**
     * Last response from esc
     */
    private String lastResponse;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static FunctionCaller instance;


    private FunctionCaller() {
        isDocker = "1".equals(System.getProperty(SYSTEM_PROP_IS_DOCKER));

        dataDir = System.getProperty(SYSTEM_PROP_DATA_DIR, DEFAULT_DATA_DIR);
        // remove '/', if is present at the end of dir
        if (dataDir.charAt(dataDir.length() - 1) == '/') {
            dataDir = dataDir.substring(0, dataDir.length() - 1);
        }
        tmpDir = dataDir + "/tmp";
        if (isDocker) {
            int lastIndex = DOCKER_ADS_BINARY.lastIndexOf(" ");
            // remove client app binary from end
            sysCmdPrefix = DOCKER_ADS_BINARY.substring(0, lastIndex + 1);
            clientApp = DOCKER_ADS_BINARY;
        } else {
            sysCmdPrefix = "";
            clientApp = ADS_BINARY;
        }
        // options:
        // -n (--nice)
        clientAppOpts = " -n0 -w " + tmpDir;
    }

    public static FunctionCaller getInstance() {
        if (instance == null) {
            instance = new FunctionCaller();
        }
        return instance;
    }

    /**
     * Calls broadcast function, which sends message to the network.
     *
     * @param userData user data
     * @param message  message as hexadecimal String with even number of characters
     * @return response: json when request was correct, empty otherwise
     */
    public String broadcast(UserData userData, String message) {
        log.debug("broadcast");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"broadcast\", \"message\":\"%s\"}') | ", message)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls change_account_key function.
     *
     * @param userData  user data
     * @param publicKey new public key
     * @param signature empty String signed with new private key
     * @return response: json when request was correct, empty otherwise
     */
    public String changeAccountKey(UserData userData, String publicKey, String signature) {
        log.debug("changeAccountKey");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"change_account_key\", \"public_key\":\"%s\", \"confirm\":\"%s\"}') | ", publicKey, signature)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls change_node_key function.
     *
     * @param userData  user data
     * @param publicKey new public key
     * @return response: json when request was correct, empty otherwise
     */
    public String changeNodeKey(UserData userData, String publicKey) {
        log.debug("changeNodeKey");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"change_node_key\", \"public_key\":\"%s\"}') | ", publicKey)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls create_account function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String createAccount(UserData userData) {
        log.debug("createAccount in current node");
        String command = "(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_account\"}') | "
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls create_account function.
     *
     * @param userData user data
     * @param node     node in which account should be created (decimal)
     * @return response: json when request was correct, empty otherwise
     */
    public String createAccount(UserData userData, int node) {
        log.debug("createAccount in {} node", node);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_account\", \"node\":\"%d\"}') | ", node)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls create_node function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String createNode(UserData userData) {
        log.debug("createNode");
        String command = "(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_node\"}') | "
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls get_account function.
     *
     * @param userData user data
     * @param address  address of checked account
     * @return response: json when request was correct, empty otherwise
     */
    public String getAccount(UserData userData, String address) {
        log.debug("getAccount {}", address);
        String command = String.format("echo '{\"run\":\"get_account\", \"address\":\"%s\"}' | ", address)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_accounts function.
     *
     * @param userData user data
     * @param node     node id
     * @return response: json when request was correct, empty otherwise
     */
    public String getAccounts(UserData userData, int node) {
        return getAccounts(userData, node, "0");
    }

    /**
     * Calls get_accounts function.
     *
     * @param userData  user data
     * @param node      node id
     * @param blockTime block time in Unix Epoch seconds as hexadecimal String, 0 for previous (last closed) block
     * @return response: json when request was correct, empty otherwise
     */
    public String getAccounts(UserData userData, int node, String blockTime) {
        log.debug("getAccounts node={}, block={}", node, blockTime);
        String command = String.format("echo '{\"run\":\"get_accounts\", \"node\":%d, \"block\":\"%s\"}' | ", node, blockTime)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Wrapper for get_block function. Block information is not available for short time after block change,
     * therefore this function calls get_block few times.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getBlock(UserData userData) {
        String resp = null;

        // block info is not available for short time after block change,
        // expected delay is longer after node creation,
        // therefore there is delay - it cannot be "wait for next block"
        int attempt = 0;
        final long delay = 3000L;
        final int attemptMax = (int) (EscConst.BLOCK_PERIOD_MS / delay);
        log.trace("attemptMax = {}", attemptMax);
        while (attempt++ < attemptMax) {
            resp = FunctionCaller.getInstance().getBlockSingleCall(userData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.debug("Error occurred: {}", errorDesc);
                assertThat("Unexpected error for get_block request.", errorDesc,
                        equalTo(EscConst.Error.GET_BLOCK_INFO_FAILED));
            } else {
                JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
                // special node 0 is on the list but it is shouldn't be counted in total
                int nodesCount = arr.size() - 1;
                int vipNodeCount = 0;
                for (JsonElement je : arr) {
                    int status = je.getAsJsonObject().get("status").getAsInt();
                    if (EscUtils.isStatusVip(status)) {
                        ++vipNodeCount;
                    }
                }

                if (Integer.min(nodesCount, EscConst.VIP_MAX) != vipNodeCount) {
                    String reason = new AssertReason.Builder().msg("Incorrect number of vip nodes")
                            .msg("nodes count: " + nodesCount)
                            .msg("VIP_MAX:     " + EscConst.VIP_MAX)
                            .msg("vip count:   " + vipNodeCount)
                            .res(resp).build();
                    Assert.fail(reason);
                }

                log.debug("getBlock resp in {} attempt", attempt);
                return resp;
            }

            assertThat("Cannot get block info after delay", attempt < attemptMax);

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertThat("Cannot get response", resp, notNullValue());
        return resp;
    }

    /**
     * Calls get_block function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    private String getBlockSingleCall(UserData userData) {
        log.debug("getBlock");
        String command = ("echo '{\"run\":\"get_block\"}' | ")
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Wrapper for get_blocks function. Calls get_blocks until it updates all blocks.
     * All blocks are updated when error "No new blocks to download" is returned.
     *
     * @param userData user data
     */
    public void updateBlocks(UserData userData) {
        int attempt = 0;
        int attemptMax = 5;

        while (attempt++ < attemptMax) {
            String resp = getBlocks(userData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDescription = o.get("error").getAsString();

                if (EscConst.Error.GET_SIGNATURE_UNAVAILABLE.equals(errorDescription)) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                String reason = new AssertReason.Builder().msg("Unexpected error for get_blocks: " + errorDescription)
                        .req(FunctionCaller.getInstance().getLastRequest())
                        .res(FunctionCaller.getInstance().getLastResponse())
                        .build();
                Assert.fail(reason);
            } else {
                if (0 == o.get("updated_blocks").getAsInt()) {
                    // If updated_blocks == 0, then all blocks are updated.
                    return;
                }
            }
        }
        assertThat("Didn't update blocks in expected attempts.", attempt, lessThan(attemptMax));
    }

    /**
     * Calls get_blocks function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    private String getBlocks(UserData userData) {
        log.debug("getBlocks");
        String command = ("echo '{\"run\":\"get_blocks\"}' | ")
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

//    /**
//     * Calls get_broadcast function, which gets broadcast messages from last block.
//     *
//     * @param userData user data
//     * @return response: json when request was correct, empty otherwise
//     */
//    public String getBroadcast(UserData userData) {
//        return getBroadcast(userData, "0");
//    }

    /**
     * Calls get_broadcast function, which gets broadcast messages from block.
     *
     * @param userData  user data
     * @param blockTime block time in Unix Epoch seconds as hexadecimal String, 0 for last block
     * @return response: json when request was correct, empty otherwise
     */
    public String getBroadcast(UserData userData, String blockTime) {
        log.debug("getBroadcast");
        String command = String.format("echo '{\"run\":\"get_broadcast\", \"from\":\"%s\"}' | ", blockTime)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_me function for specific user.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getMe(UserData userData) {
        log.debug("getMe");
        String command = ("echo '{\"run\":\"get_me\"}' | ")
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_message function.
     *
     * @param userData  user data
     * @param messageId message id
     * @return response: json when request was correct, empty otherwise
     */
    public String getMessage(UserData userData, String messageId) {
        log.debug("getMessage");
        String command = String.format("echo '{\"run\":\"get_message\", \"message_id\":\"%s\"}' | ", messageId)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

//    /**
//     * Calls get_message function.
//     *
//     * @param userData  user data
//     * @param blockTime block time in Unix Epoch seconds as hexadecimal String
//     * @param messageId message id
//     * @return response: json when request was correct, empty otherwise
//     */
//    public String getMessage(UserData userData, String blockTime, String messageId) {
//        log.debug("getMessage");
//        String command = String.format(
//                "echo '{\"run\":\"get_message\", \"block\":\"%s\", \"message_id\":%s}' | ",
//                blockTime, messageId)
//                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
//        return callFunction(command);
//    }

    /**
     * Calls get_message_list function.
     *
     * @param userData  user data
     * @param blockTime block time in Unix Epoch seconds as hexadecimal String
     * @return response: json when request was correct, empty otherwise
     */
    public String getMessageList(UserData userData, String blockTime) {
        log.debug("getMessageList");
        String command = String.format("echo '{\"run\":\"get_message_list\", \"block\":\"%s\"}' | ", blockTime)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_log function for specific user.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getLog(UserData userData) {
        return getLog(userData, 0L);
    }

    /**
     * Calls get_log function for specific user.
     *
     * @param userData      user data
     * @param fromTimeStamp log start time in Unix Epoch seconds
     * @return response: json when request was correct, empty otherwise
     */
    public String getLog(UserData userData, long fromTimeStamp) {
        log.debug("getLog from {}", fromTimeStamp);
        String command = String.format("echo '{\"run\":\"get_log\", \"from\":\"%d\"}' | ", fromTimeStamp)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_log function for specific user.
     *
     * @param userData          user data
     * @param logEventTimeStamp log start timestamp
     * @return response: json when request was correct, empty otherwise
     */
    public String getLog(UserData userData, LogEventTimestamp logEventTimeStamp) {
        log.debug("getLog from {}", logEventTimeStamp);
        long timestamp = logEventTimeStamp.getTimestamp();
        String command = String.format("echo '{\"run\":\"get_log\", \"from\":\"%d\"}' | ", timestamp)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String resp = callFunction(command);

        // if eventNum is lesser than 2, no event will be removed
        int eventNum = logEventTimeStamp.getEventNum();
        if (eventNum > 1) {
            JsonObject o = Utils.convertStringToJsonObject(resp);
            JsonElement jsonElementLog = o.get("log");
            if (jsonElementLog.isJsonArray()) {
                JsonArray arr = jsonElementLog.getAsJsonArray();
                if (arr.size() > 0) {
                    Iterator<JsonElement> it = arr.iterator();

                    int eventCount = 0;
                    while (it.hasNext()) {
                        JsonObject entry = it.next().getAsJsonObject();
                        // events in log are sorted by time:
                        // if event time is different than timestamp,
                        // this means that event happened later than timestamp
                        long ts = entry.get("time").getAsLong();
                        if (ts != timestamp) {
                            break;
                        }
                        ++eventCount;
                        if (eventCount >= eventNum) {
                            break;
                        }
                        it.remove();
                        log.trace("event removed");
                    }

                    Gson gson = new GsonBuilder().create();
                    resp = gson.toJson(o);

                }
            }
        }

        return resp;
    }

    /**
     * Wrapper for get_transaction function. Transaction information is not available for short time after execute,
     * therefore this function calls get_transaction few times.
     *
     * @param userData user data
     * @param txid     transaction id
     * @return response: json when request was correct, empty otherwise
     */
    public String getTransaction(UserData userData, String txid) {
        String resp = null;

        // transaction info is not available for short time after transaction commit,
        // expected delay is longer after node creation,
        // therefore there is delay - it cannot be "wait for next block"
        int attempt = 0;
        final long delay = EscConst.BLOCK_PERIOD_MS;
        final int attemptMax = 3;
        log.trace("attemptMax = {}", attemptMax);
        while (attempt++ < attemptMax) {
            updateBlocks(userData);
            resp = getTransactionSingleCall(userData, txid);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.debug("Error occurred: {}", errorDesc);

                boolean isExpectedError = EscConst.Error.FAILED_TO_PROVIDE_TX_INFO.equals(errorDesc)
                        || EscConst.Error.FAILED_TO_LOAD_HASH.equals(errorDesc);
                assertThat("Unexpected error for get_transaction request: " + errorDesc, isExpectedError);
                assertThat("Cannot get transaction info after delay.", attempt < attemptMax);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                return resp;
            }
        }

        assertThat("Cannot get response.", resp, notNullValue());
        return resp;
    }

    /**
     * Calls get_transaction function.
     *
     * @param userData user data
     * @param txid     transaction id
     * @return response: json when request was correct, empty otherwise
     */
    private String getTransactionSingleCall(UserData userData, String txid) {
        log.debug("getTransaction {}", txid);
        String command = String.format("echo '{\"run\":\"get_transaction\", \"txid\":\"%s\"}' | ", txid)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_vipkeys function.
     *
     * @param userData user data
     * @param vipHash  vip hash
     * @return response: json when request was correct, empty otherwise
     */
    public String getVipKeys(UserData userData, String vipHash) {
        log.debug("getVipKeys for vip hash {}", vipHash);
        String command = String.format("echo '{\"run\":\"get_vipkeys\", \"viphash\":\"%s\"}' | ", vipHash)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls log_account function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String logAccount(UserData userData) {
        log.debug("logAccount");
        String command = ("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"log_account\"}') | ")
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        return callFunction(command).replaceFirst(DOUBLE_RESP_REGEX, "{");
    }

    /**
     * Calls retrieve_funds function.
     *
     * @param userData      data of user, who will retrieve funds
     * @param remoteAddress account address from which funds will be retrieved
     * @return response: json when request was correct, empty otherwise
     */
    public String retrieveFunds(UserData userData, String remoteAddress) {
        log.debug("retrieveFunds by {} from {}", userData.getAddress(), remoteAddress);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"retrieve_funds\", \"address\":\"%s\"}') | ", remoteAddress)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls send_one function.
     *
     * @param sender          sender data
     * @param receiverAddress receiver address
     * @param amount          transfer amount
     * @return response: json when request was correct, empty otherwise
     */
    public String sendOne(UserData sender, String receiverAddress, String amount) {
        log.debug("sendOne {}->{}: {}", sender.getAddress(), receiverAddress, amount);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_one\", \"address\":\"%s\", \"amount\":\"%s\"}') | ", receiverAddress, amount)
                .concat(clientApp).concat(clientAppOpts).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        return output.replaceFirst(DOUBLE_RESP_REGEX, "{");
    }

    /**
     * Calls send_one function.
     *
     * @param sender          sender data
     * @param receiverAddress receiver address
     * @param amount          transfer amount
     * @param message         message
     * @return response: json when request was correct, empty otherwise
     */
    public String sendOne(UserData sender, String receiverAddress, String amount, String message) {
        log.debug("sendOne {}->{}: {}, msg: {}", sender.getAddress(), receiverAddress, amount, message);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_one\", \"address\":\"%s\", \"amount\":\"%s\", \"message\":\"%s\"}') | ", receiverAddress, amount, message)
                .concat(clientApp).concat(clientAppOpts).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        return output.replaceFirst(DOUBLE_RESP_REGEX, "{");
    }

    /**
     * Calls send_many function.
     *
     * @param sender      sender data
     * @param receiverMap map of receiver - amount pairs
     * @return response: json when request was correct, empty otherwise
     */
    public String sendMany(UserData sender, Map<String, String> receiverMap) {
        log.debug("sendMany {}->", sender.getAddress());
        for (Map.Entry<String, String> entry : receiverMap.entrySet()) {
            log.debug("sendMany ->{}: {}", entry.getKey(), entry.getValue());
        }
        Gson gson = new GsonBuilder().create();
        String wires = gson.toJson(receiverMap);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_many\", \"wires\":%s}') | ", wires)
                .concat(clientApp).concat(clientAppOpts).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls send_many function.
     *
     * @param sender       sender data
     * @param receiverList list of receiver - amount pairs
     * @return response: json when request was correct, empty otherwise
     */
    public String sendMany(UserData sender, List<String[]> receiverList) {
        log.debug("sendMany {}->", sender.getAddress());
        StringBuilder sb = new StringBuilder("{");
        for (String[] entry : receiverList) {
            log.debug("sendMany ->{}: {}", entry[0], entry[1]);
            sb.append("\"");
            sb.append(entry[0]);
            sb.append("\":\"");
            sb.append(entry[1]);
            sb.append("\",");
        }
        sb.setLength(sb.length() - 1);
        sb.append("}");
        String wires = sb.toString();
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_many\", \"wires\":%s}') | ", wires)
                .concat(clientApp).concat(clientAppOpts).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls set_account_status function.
     *
     * @param userData user data
     * @param address  address of account, which status should be changed
     * @param status   integer, bits which are 1, should be set in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String setAccountStatus(UserData userData, String address, int status) {
        log.debug("setAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_account_status\", \"address\":\"%s\", \"status\":\"%d\"}') | ", address, status)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls set_account_status function.
     *
     * @param userData user data
     * @param address  address of account, which status should be changed
     * @param status   binary String, bits which are 1, should be set in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String setAccountStatus(UserData userData, String address, String status) {
        log.debug("setAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_account_status\", \"address\":\"%s\", \"status\":\"%s\"}') | ", address, statusDec)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls set_node_status function.
     *
     * @param userData user data
     * @param nodeId   id of node, which status should be changed
     * @param status   integer, bits which are 1, should be set in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String setNodeStatus(UserData userData, String nodeId, int status) {
        int node = Integer.valueOf(nodeId, 16);
        log.debug("setNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", node, status)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls set_node_status function.
     *
     * @param userData user data
     * @param nodeId   id of node, which status should be changed
     * @param status   binary String, bits which are 1, should be set in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String setNodeStatus(UserData userData, String nodeId, String status) {
        int node = Integer.valueOf(nodeId, 16);
        log.debug("setNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", node, statusDec)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls unset_account_status function.
     *
     * @param userData user data
     * @param address  address of account, which status should be changed
     * @param status   integer, bits which are 1, should be unset in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String unsetAccountStatus(UserData userData, String address, int status) {
        log.debug("unsetAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_account_status\", \"address\":\"%s\", \"status\":\"%d\"}') | ", address, status)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls unset_account_status function.
     *
     * @param userData user data
     * @param address  address of account, which status should be changed
     * @param status   binary String, bits which are 1, should be unset in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String unsetAccountStatus(UserData userData, String address, String status) {
        log.debug("unsetAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_account_status\", \"address\":\"%s\", \"status\":\"%s\"}') | ", address, statusDec)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls unset_node_status function.
     *
     * @param userData user data
     * @param nodeId   id of node, which status should be changed
     * @param status   integer, bits which are 1, should be set in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String unsetNodeStatus(UserData userData, String nodeId, int status) {
        int node = Integer.valueOf(nodeId, 16);
        log.debug("unsetNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", node, status)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls unset_account_status function.
     *
     * @param userData user data
     * @param nodeId   id of node, which status should be changed
     * @param status   binary String, bits which are 1, should be unset in account status
     * @return response: json when request was correct, empty otherwise
     */
    public String unsetNodeStatus(UserData userData, String nodeId, String status) {
        int node = Integer.valueOf(nodeId, 16);
        log.debug("unsetNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", node, statusDec)
                .concat(clientApp).concat(clientAppOpts).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(DOUBLE_RESP_REGEX, "{");
        return output;
    }

    /**
     * Calls command in sh shell.
     *
     * @param cmd command
     * @return stdout response
     */
    private String callFunction(String cmd) {
        log.debug("request: {}", cmd);
        lastRequest = cmd;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);

        CommandLine cmdLine = new CommandLine("/bin/sh");

        if (cmd.length() < 12000) {
            cmdLine.addArgument("-c").addArgument(cmd, false);
        } else {
            log.debug("command length {}", cmd.length());
            // when command is too long it is not possible to call it using sh shell
            // command is saved to file and file is new command

            PrintWriter writer;
            try {
                writer = new PrintWriter(TEMP_FILE_NAME, "UTF-8");
                writer.println(cmd);
                writer.close();
            } catch (FileNotFoundException e) {
                log.error("File not found");
                log.error(e.toString());
            } catch (UnsupportedEncodingException uee) {
                log.error("Unsupported Encoding");
                log.error(uee.toString());
            }
            cmdLine.addArgument(TEMP_FILE_NAME);
        }

        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(streamHandler);
        executor.setWatchdog(new ExecuteWatchdog(60000L));// 60,000 ms = 1 min.
        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            log.error("Cannot read from ESC");
            log.error(e.toString());
        }
        String resp = outputStream.toString();
        log.debug("resp: {}", resp);
        if ("".equals(resp)) {
            log.warn("Empty response for: {}", cmd);
        }

        try {
            Utils.deleteDirectory(TEMP_FILE_NAME);
        } catch (IOException e) {
            log.error("Cannot delete {}", TEMP_FILE_NAME);
            log.error(e.toString());
        }

        lastResponse = resp;
        return resp;
    }

    /**
     * Returns timestamp of last event in log.
     *
     * @param userData user data
     * @return timestamp of last event in log or 0, if log is empty
     */
    public LogEventTimestamp getLastEventTimestamp(UserData userData) {
        JsonObject o = Utils.convertStringToJsonObject(getLog(userData));

        LogEventTimestamp let = EscUtils.getLastLogEventTimestamp(o);
        log.debug("last log event time: {} ({}) for {}", let, Utils.formatSecondsAsDate(let.getTimestamp()),
                o.getAsJsonObject("account").get("address").getAsString());
        return let;
    }

    /**
     * Returns user account balance. Shortcut for getting balance from get_me response
     *
     * @param userData user data
     * @return user account balance
     */
    public BigDecimal getUserAccountBalance(UserData userData) {
        String resp = getMe(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        BigDecimal balance = null;
        try {
            balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
        } catch (NullPointerException npe) {
            String msg = new AssertReason.Builder().req(getLastRequest()).res(getLastResponse())
                    .msg("User " + userData.getAddress())
                    .msg("NullPointerException").build();
            Assert.fail(msg);
        }
        log.debug("user {} balance: {}", userData.getAddress(), balance.toPlainString());
        return balance;
    }

    /**
     * Returns user account balance. Shortcut for getting balance from get_me response
     *
     * @param user user
     * @return user account balance
     */
    public BigDecimal getUserAccountBalance(TransferUser user) {
        return getUserAccountBalance(user.getUserData());
    }

    /**
     * Appends node's private key to key.txt file.
     *
     * @param node       node ordinal number (decimal)
     * @param privateKey private key
     */
    public void addNodePrivateKey(int node, String privateKey) {
        String keyFileName = String.format("%1$s/node%2$d/key/key.txt", dataDir, node);
        String keyFileContent = callFunction(sysCmdPrefix.concat("cat " + keyFileName));
        if ("".equals(keyFileContent)) {
            // if node in dec format is not available, check new hex format
            keyFileName = String.format("%1$s/node%2$04X/key/key.txt", dataDir, node);
            keyFileContent = callFunction(sysCmdPrefix.concat("cat " + keyFileName));
        }

        if ("".equals(keyFileContent)) {
            // empty content means that whole directory must be created
            // create directory in which key.txt file is stored
            final String path = keyFileName.substring(0, keyFileName.lastIndexOf("/") + 1);
            callFunction(sysCmdPrefix.concat("mkdir -p ").concat(path));
        }

        if (!keyFileContent.contains(privateKey)) {
            callFunction(sysCmdPrefix.concat("sh -c \"echo '\n" + privateKey + "' >> " + keyFileName + "\""));
        }
    }

    /**
     * Starts node.
     *
     * @param node node ordinal number (decimal)
     */
    public void startNode(int node) {
        String path = String.format("%1$s/node%2$04X/", dataDir, node);

        // create options.cfg file
        String optFileContent = callFunction(sysCmdPrefix.concat(String.format("cat %s/node0001/options.cfg", dataDir)));
        if ("".equals(optFileContent)) {
            // node number should be in hex format, but to keep compatibility it is also checked in dec format
            optFileContent = callFunction(sysCmdPrefix.concat(String.format("cat %s/node1/options.cfg", dataDir)));
        }
        assertThat("Missing node0001 options.cfg.", !"".equals(optFileContent));

        String[] lines = optFileContent.split("\n");
        Map<String, String> optionMap = new HashMap<>();
        for (String line : lines) {
            if (line.lastIndexOf("=") != -1) {
                String[] arr = line.split("=");
                optionMap.put(arr[0], arr[1]);
            }
        }

        String node1Addr = optionMap.get("addr");
        assertThat("Missing `addr` in node0001 options.cfg.", node1Addr, notNullValue());
        // port for clients
        String node1OfficePort = optionMap.get("offi");
        assertThat("Missing `offi` in node0001 options.cfg.", node1OfficePort, notNullValue());
        //port for peers
        String node1Port = optionMap.get("port");
        assertThat("Missing `port` in node0001 options.cfg.", node1Port, notNullValue());

        optionMap.put("svid", String.valueOf(node));
        int offi = Integer.valueOf(node1OfficePort) + node - 1;
        optionMap.put("offi", String.valueOf(offi));
        int port = Integer.valueOf(node1Port) + node - 1;
        optionMap.put("port", String.valueOf(port));
        optionMap.put("peer", node1Addr + ":" + node1Port);

        StringBuilder sb = new StringBuilder();
        for (String key : optionMap.keySet()) {
            sb.append(key);
            sb.append("=");
            sb.append(optionMap.get(key));
            sb.append("\n");
        }
        optFileContent = sb.toString();

        String optFileName = path + "options.cfg";
        callFunction(sysCmdPrefix.concat("sh -c \"echo '" + optFileContent + "' > " + optFileName + "\""));

        // start node
        callFunction(sysCmdPrefix.concat("sh -c \"adsd -f 1 -w " + path + " >stdout 2>stderr &\""));
    }

    /**
     * Deletes ads cache.
     */
    public void deleteCache() {
        // deletes cache
        callFunction(sysCmdPrefix + "rm -rf " + tmpDir);
        callFunction(sysCmdPrefix + "mkdir " + tmpDir);
    }

    /**
     * Waits for esc compilation
     */
    public void waitForCompilation() {
        if (isDocker) {
            String resp;
            long startTime = System.currentTimeMillis();
            do {
                resp = callFunction(
                        sysCmdPrefix + String.format("/ads_repo/scripts/ads_ctl.py --data-dir=%s wait", dataDir));
                assertThat("Timeout during docker start.",
                        !(resp.contains("timeout") || resp.contains("failed")
                                || (System.currentTimeMillis() - startTime > COMPILATION_TIMEOUT)));
                assertThat("No response from docker.", resp, not(""));
            } while (!resp.contains("started"));
        }
    }

    /**
     * @return last request
     */
    public String getLastRequest() {
        return lastRequest;
    }

    /**
     * @return last response
     */
    public String getLastResponse() {
        return lastResponse;
    }
}
