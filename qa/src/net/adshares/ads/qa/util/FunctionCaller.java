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

    private static final String SYSTEM_PROP_IS_DOCKER = "is.docker";
    private static final String SYSTEM_PROP_DATA_DIR = "dir.data";
    private static final String DEFAULT_DATA_DIR = "/ads-data";
    private static final String DOCKER_ESC_BINARY_FMT = "docker exec -i -w %s adshares_ads_1 esc";
    private static final String ESC_BINARY_OPTS = " -n0 ";
    /**
     * True, if test are performed on docker.
     * False, if locally.
     */
    private boolean isDocker;
    private String escBinary;
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
        isDocker = "1".equals(System.getProperty(SYSTEM_PROP_IS_DOCKER));

        dataDir = System.getProperty(SYSTEM_PROP_DATA_DIR, DEFAULT_DATA_DIR);
        // remove '/', if is present at the end of dir
        if (dataDir.charAt(dataDir.length() - 1) == '/') {
            dataDir = dataDir.substring(0, dataDir.length() - 1);
        }
        tmpDir = dataDir + "/tmp";
        if (isDocker) {
            escBinary = String.format(DOCKER_ESC_BINARY_FMT, tmpDir);
            int lastIndex = escBinary.lastIndexOf(" ");
            // remove esc binary from end
            sysCmdPrefix = escBinary.substring(0, lastIndex + 1);
            // remove -w option (working directory)
            sysCmdPrefix = sysCmdPrefix.replaceFirst(" -w \\S+ ", " ");
        } else {
            escBinary = "esc";
            sysCmdPrefix = "";
        }
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
     * @param node     node in which account should be created (decimal)
     * @return response: json when request was correct, empty otherwise
     */
    public String createAccount(UserData userData, int node) {
        log.info("createAccount in {} node", node);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"create_account\", \"node\":\"%d\"}') | ", node)
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
        log.debug("attemptMax = {}", attemptMax);
        while (attempt++ < attemptMax) {
            resp = FunctionCaller.getInstance().getBlockSingleCall(userData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.info("Error occurred: {}", errorDesc);
                Assert.assertEquals("Unexpected error after account creation.",
                        EscConst.Error.GET_BLOCK_INFO_FAILED, errorDesc);
            } else {
                JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
                // special node 0 is on the list but it is counted in total
                int nodesCount = arr.size() - 1;
                int vipNodeCount = 0;
                for (JsonElement je : arr) {
                    int status = je.getAsJsonObject().get("status").getAsInt();
                    if ((status & 2) != 0) {
                        ++vipNodeCount;
                    }
                }

                if (Integer.min(nodesCount, EscConst.VIP_MAX) != vipNodeCount) {
                    log.error("nodes count: {}", nodesCount);
                    log.error("VIP_MAX:     {}", EscConst.VIP_MAX);
                    log.error("vip count:   {}", vipNodeCount);
                    Assert.fail("Incorrect number of vip nodes : " + vipNodeCount);
                }

                log.info("getBlock resp in {} attempt", attempt);
                return resp;
            }

            Assert.assertTrue("Cannot get block info after delay", attempt < attemptMax);

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Assert.assertNotEquals("Cannot get response", null, resp);
        return resp;
    }

    /**
     * Calls get_block function.
     *
     * @param userData user data
     * @return response: json when request was correct, empty otherwise
     */
    private String getBlockSingleCall(UserData userData) {
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
        int node = Integer.valueOf(nodeId, 16);
        log.info("setNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", node, status)
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
        int node = Integer.valueOf(nodeId, 16);
        log.info("setNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"set_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", node, statusDec)
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
        int node = Integer.valueOf(nodeId, 16);
        log.info("unsetNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, Integer.toBinaryString(status));
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%d\"}') | ", node, status)
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
        int node = Integer.valueOf(nodeId, 16);
        log.info("unsetNodeStatus {}->node {} (dec): status {} (bin)", userData.getAddress(), node, status);
        BigInteger bi = new BigInteger(status, 2);
        String statusDec = bi.toString(10);
        String command = String.format("(echo '{\"run\":\"get_me\"}';echo '{\"run\":\"unset_node_status\", \"node\":\"%s\", \"status\":\"%s\"}') | ", node, statusDec)
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
     * @param node       node ordinal number (decimal)
     * @param privateKey private key
     */
    public void addNodePrivateKey(int node, String privateKey) {
        String keyFileName = String.format("%s/node%d/key/key.txt", dataDir, node);
        String keyFileContent = callFunction(sysCmdPrefix.concat("cat " + keyFileName));

        if ("".equals(keyFileContent)) {
            // empty content means that whole directory must be created
            // create directory in which key.txt file is stored
            final String path = keyFileName.substring(0, keyFileName.lastIndexOf("/") + 1);
            callFunction(sysCmdPrefix.concat("mkdir -p ").concat(path));
        }

        if (!keyFileContent.contains(privateKey)) {
            callFunction(sysCmdPrefix.concat("sh -c \"echo '" + privateKey + "' >> " + keyFileName + "\""));
        }
    }

    /**
     * Starts node.
     *
     * @param node node ordinal number (decimal)
     */
    public void startNode(int node) {
        String path = String.format("/ads-data/node%d/", node);

        // create options.cfg file
        String optFileContent = callFunction(sysCmdPrefix.concat("cat /ads-data/node1/options.cfg"));
        String[] arr = optFileContent.split("\n");
        //svid
        arr[0] = "svid=" + node;
        //offi
        int offi = Integer.valueOf(arr[1].split("=")[1]) + node - 1;
        arr[1] = "offi=" + offi;
        // port
        int port = Integer.valueOf(arr[2].split("=")[1]) + node - 1;
        arr[2] = "port=" + port;
        // address is the same
        // peers are the same
        optFileContent = String.join("\n", arr);
        String optFileName = path + "options.cfg";
        callFunction(sysCmdPrefix.concat("sh -c \"echo '" + optFileContent + "' > " + optFileName + "\""));

        // start node
        callFunction(sysCmdPrefix.concat("sh -c \"cd " + path + ";escd -f 1 >stdout 2>stderr &\""));

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
                resp = callFunction(sysCmdPrefix.concat("/docker/wait-up.php"));
                Assert.assertFalse("Timeout during docker start",
                        resp.contains("timeout") || resp.contains("failed")
                                || (System.currentTimeMillis() - startTime > COMPILATION_TIMEOUT));
                Assert.assertNotEquals("No response from docker", "", resp);
            } while (!resp.contains("started"));
        }
    }
}
