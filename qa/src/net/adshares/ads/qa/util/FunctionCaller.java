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
import java.util.Iterator;
import java.util.Map;

public class FunctionCaller {

    private static final String SYSTEM_PROP_ESC_BINARY = "bin.esc";
    private static final String DEFAULT_ESC_BINARY = "docker exec -i -w /tmp/esc adshares_ads_1 esc";
    private static final String ESC_BINARY_OPTS = " -n0 ";
    private static String escBinary;
    /**
     * If docker is in use, all system commands must be preceded with "docker exec -i <<docker_image>> ".
     * System command is every command, which is not called on esc binary.
     */
    private static String sysCmdPrefix;

    /**
     * Timeout for compilation in milliseconds
     */
    private static final int COMPILATION_TIMEOUT = 300000;// 300000 ms = 5 min.
    /**
     * Name of temporary file that is created for commands that cannot be called directly in shell
     */
    private static final String TEMP_FILE_NAME = "tmp";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static FunctionCaller instance;


    private FunctionCaller() {
    }

    public static FunctionCaller getInstance() {
        if (instance == null) {
            instance = new FunctionCaller();

            escBinary = System.getProperty(SYSTEM_PROP_ESC_BINARY, DEFAULT_ESC_BINARY);
            if (escBinary.contains("docker")) {
                int lastIndex = escBinary.lastIndexOf(" ");
                // remove esc binary from end
                sysCmdPrefix = escBinary.substring(0, lastIndex + 1);
                // remove -w option (working directory)
                sysCmdPrefix = sysCmdPrefix.replaceFirst(" -w \\S+ ", " ");
            } else {
                sysCmdPrefix = "";
            }
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
        log.info("broadcast");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"broadcast\", \"message\":\"%s\"}') | ", message)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("changeAccountKey");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"change_account_key\", \"pkey\":\"%s\", \"signature\":\"%s\"}') | ", publicKey, signature)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("changeNodeKey");
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"change_node_key\", \"pkey\":\"%s\"}') | ", publicKey)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls create_account function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String createAccount(UserData userData) {
        log.info("createAccount in current node");
        String command = "(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_account\"}') | "
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls create_account function.
     *
     * @param userData user data
     * @param node     node in which account should be created
     * @return response: json when request was correct, empty otherwise
     */
    public String createAccount(UserData userData, String node) {
        log.info("createAccount in {} node", node);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_account\", \"node\":\"%s\"}') | ", node)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls create_node function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String createNode(UserData userData) {
        log.info("createNode");
        String command = "(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_node\"}') | "
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls get_account function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getAccount(UserData userData, String address) {
        log.info("getAccount {}", address);
        String command = String.format("echo '{\"run\":\"get_account\", \"address\":\"%s\"}' | ", address)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_block function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getBlock(UserData userData) {
        log.info("getBlock");
        String command = ("echo '{\"run\":\"get_block\"}' | ")
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
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
     * @param blockTime block time in Unix Epoch seconds, 0 for last block
     * @return response: json when request was correct, empty otherwise
     */
    public String getBroadcast(UserData userData, String blockTime) {
        log.info("getBroadcast");
        String command = String.format("echo '{\"run\":\"get_broadcast\", \"from\":\"%s\"}' | ", blockTime)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        return callFunction(command);
    }

    /**
     * Calls get_me function for specific user.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    public String getMe(UserData userData) {
        log.info("getMe");
        String command = ("echo '{\"run\":\"get_me\"}' | ")
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
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
        log.info("getLog from {}", fromTimeStamp);
        String command = String.format("echo '{\"run\":\"get_log\", \"from\":\"%d\"}' | ", fromTimeStamp)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
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
        log.info("getLog from {}", logEventTimeStamp);
        long timestamp = logEventTimeStamp.getTimestamp();
        String command = String.format("echo '{\"run\":\"get_log\", \"from\":\"%d\"}' | ", timestamp)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
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
                        log.debug("REMOVED");
                    }

                    Gson gson = new GsonBuilder().create();
                    resp = gson.toJson(o);

                }
            }
        }

        return resp;
    }

    /**
     * Calls retrieve_funds function.
     *
     * @param userData      data of user, who will retrieve funds
     * @param remoteAddress account address from which funds will be retrieved
     * @return response: json when request was correct, empty otherwise
     */
    public String retrieveFunds(UserData userData, String remoteAddress) {
        log.info("retrieveFunds by {} from {}", userData.getAddress(), remoteAddress);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"retrieve_funds\", \"address\":\"%s\"}') | ", remoteAddress)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls send_one function
     *
     * @param sender          sender data
     * @param receiverAddress receiver address
     * @param amount          transfer amount
     * @return response: json when request was correct, empty otherwise
     */
    public String sendOne(UserData sender, String receiverAddress, String amount) {
        log.info("sendOne {}->{}: {}", sender.getAddress(), receiverAddress, amount);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_one\", \"address\":\"%s\", \"amount\":\"%s\"}') | ", receiverAddress, amount)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls send_many function
     *
     * @param sender      sender data
     * @param receiverMap map of receiver - amount pairs
     * @return response: json when request was correct, empty otherwise
     */
    public String sendMany(UserData sender, Map<String, String> receiverMap) {
        log.info("sendMany {}->", sender.getAddress());
        for (Map.Entry<String, String> entry : receiverMap.entrySet()) {
            log.info("sendMany ->{}: {}", entry.getKey(), entry.getValue());
        }
        Gson gson = new GsonBuilder().create();
        String wires = gson.toJson(receiverMap);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"send_many\", \"wires\":%s}') | ", wires)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(sender.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("setAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_account_status\", \"address\":\"%s\", \"status\":\"%d\"}') | ", address, status)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("setAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_account_status\", \"address\":\"%s\", \"status\":\"%s\"}') | ", address, statusDec)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("setNodeStatus {}->{}: status {} (bin)", userData.getAddress(), nodeId, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", nodeId, status)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("setNodeStatus {}->{}: status {} (bin)", userData.getAddress(), nodeId, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", nodeId, statusDec)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("unsetAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_account_status\", \"address\":\"%s\", \"status\":\"%d\"}') | ", address, status)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("unsetAccountStatus {}->{}: status {} (bin)", userData.getAddress(), address, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_account_status\", \"address\":\"%s\", \"status\":\"%s\"}') | ", address, statusDec)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("unsetNodeStatus {}->{}: status {} (bin)", userData.getAddress(), nodeId, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", nodeId, status)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
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
        log.info("unsetNodeStatus {}->{}: status {} (bin)", userData.getAddress(), nodeId, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", nodeId, statusDec)
                .concat(escBinary).concat(ESC_BINARY_OPTS).concat(userData.getDataAsEscParams());
        String output = callFunction(command);
        output = output.replaceFirst(".*}\\s*\\{", "{");
        return output;
    }

    /**
     * Calls command in sh shell
     *
     * @param cmd command
     * @return stdout response
     */
    private String callFunction(String cmd) {
        log.debug("request: {}", cmd);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);

        CommandLine cmdLine = new CommandLine("/bin/sh");

        if (cmd.length() < 12000) {
            cmdLine.addArgument("-c").addArgument(cmd, false);
        } else {
            log.info("command length {}", cmd.length());
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
        log.info("last log event time: {} ({}) for {}", let, Utils.formatSecondsAsDate(let.getTimestamp()),
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
        JsonObject o = Utils.convertStringToJsonObject(getMe(userData));
        BigDecimal balance = o.getAsJsonObject("account").get("balance").getAsBigDecimal();
        log.info("user {} balance: {}", userData.getAddress(), balance.toPlainString());
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
     * @param nodeId     node id
     * @param privateKey private key
     */
    public void addNodePrivateKey(int nodeId, String privateKey) {
        String keyFileName = String.format("/ads-data/node%d/key/key.txt", nodeId);
        String keyFileContent = callFunction(sysCmdPrefix.concat("cat " + keyFileName));
        if (!keyFileContent.contains(privateKey)) {
            callFunction(sysCmdPrefix.concat("sh -c \"echo '" + privateKey + "' >> " + keyFileName + "\""));
        }
    }

    /**
     * Deletes ads cache.
     */
    public void deleteCache() {
        // deletes local cache
//        try {
//            Utils.deleteDirectory("log");
//            Utils.deleteDirectory("out");
//        } catch (IOException e) {
//            log.warn("Unable to delete cache");
//        }
        // deletes cache in docker
        callFunction(sysCmdPrefix.concat("rm -rf /tmp/esc"));
        callFunction(sysCmdPrefix.concat("mkdir /tmp/esc"));
    }

    /**
     * Waits for esc compilation
     */
    public void waitForCompilation() {
        String resp;
        long startTime = System.currentTimeMillis();
        do {
            resp = callFunction(sysCmdPrefix.concat("/docker/wait-up.php"));
            Assert.assertFalse("Timeout during docker start",
                    resp.contains("timeout") || resp.contains("failed")
                    || (System.currentTimeMillis() - startTime > COMPILATION_TIMEOUT));
            Assert.assertNotEquals("No response from docker", "", resp);
        } while (!resp.contains("started"));
    }
}
