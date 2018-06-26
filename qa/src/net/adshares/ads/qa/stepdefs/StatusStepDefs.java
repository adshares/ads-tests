package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class StatusStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Data of user, who will change status
     */
    private UserData userData;
    /**
     * Address of account, which status will be changed
     */
    private String address;
    /**
     * List of "pair" user-node
     */
    private List<ChangeNodePair> chgNodePairList;
    /**
     * Set of bits, which was successfully changed (set/reset)
     */
    private Set<Integer> successfullyChangedBitsSet;

    private boolean isTransactionAccepted;

    /**
     * Columns:<br />
     * 1. address requested,<br />
     * 2. caller address,<br />
     * 3. response.
     */
    private String[][] getAccountResponses;
    private List<UserData> userDataList;

    @Given("^(main|regular) account user, who wants to change (own|local|remote) account status$")
    public void user_who_wants_to_change_status(String accountType, String otherAccountType) {

        boolean isMain = "main".equals(accountType);
        boolean isDataFound = false;
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        for (UserData u1 : userDataList) {
            boolean isMainU1 = u1.isMainAccount();
            if (isMain && isMainU1 || (!isMain && !isMainU1)) {
                userData = u1;

                String addressU1 = userData.getAddress();
                if ("own".equals(otherAccountType)) {
                    address = addressU1;
                    isDataFound = true;
                } else {
                    for (UserData u2 : userDataList) {
                        String addressU2 = u2.getAddress();

                        if (addressU2.equals(addressU1)) {
                            continue;
                        }

                        boolean isSameNode = userData.isAccountFromSameNode(addressU2);
                        if (("local".equals(otherAccountType) && isSameNode)
                                || ("remote".equals(otherAccountType) && !isSameNode)) {
                            address = addressU2;
                            isDataFound = true;
                            break;
                        }
                    }
                }

                if (isDataFound) {
                    break;
                }
            }
        }

        assertThat(String.format("Cannot find suitable data (for accountType=<%1$s>, otherAccountType=<%2$s>).",
                accountType, otherAccountType), isDataFound);
    }

    @Given("^(vip|main|regular) account user, who wants to change (own|remote) node status$")
    public void user_who_wants_to_change_node_status(String accountType, String otherAccountType) {

        boolean isMain = "main".equals(accountType);
        boolean isVip = "vip".equals(accountType);

        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        chgNodePairList = new ArrayList<>();
        Map<String, Integer> statusMap = getNodeStatusMap(userDataList.get(0));
        if ("remote".equals(otherAccountType)) {
            // remote
            for (UserData u1 : userDataList) {
                boolean isMainU1 = u1.isMainAccount();
                boolean isVipU1 = isMainU1 && EscUtils.isStatusVip(statusMap.get(u1.getNodeId()));

                if ((isVip && isVipU1) || (isMain && isMainU1 && !isVipU1) || (!(isMain || isVip) && !isMainU1)) {
                    Set<UserData> uSet = new HashSet<>();
                    uSet.add(u1);

                    Set<String> addedIds = new HashSet<>();
                    for (UserData u2 : userDataList) {
                        String addressU2 = u2.getAddress();

                        if (!u1.isAccountFromSameNode(addressU2)) {
                            String nodeId = addressU2.substring(0, 4);
                            if (!addedIds.contains(nodeId)) {
                                addedIds.add(nodeId);

                                chgNodePairList.add(new ChangeNodePair(uSet, nodeId));
                            }
                        }
                    }
                    break;
                }
            }
        } else {
            // own
            for (UserData u1 : userDataList) {
                boolean isMainU1 = u1.isMainAccount();
                boolean isVipU1 = isMainU1 && EscUtils.isStatusVip(statusMap.get(u1.getNodeId()));
                if ((isVip && isVipU1) || (isMain && isMainU1 && !isVipU1) || (!(isMain || isVip) && !isMainU1)) {
                    String nodeId = u1.getNodeId();
                    Set<UserData> uSet = new HashSet<>();
                    uSet.add(u1);
                    chgNodePairList.add(new ChangeNodePair(uSet, nodeId));
                }
            }
        }

        for (ChangeNodePair p : chgNodePairList) {
            log.debug(p.toString());
        }
        assertThat(String.format("Cannot find suitable data (for accountType=<%1$s>, otherAccountType=<%2$s>).",
                accountType, otherAccountType), chgNodePairList.size() != 0);
    }

    @When("^user changes account status$")
    public void user_changes_status() {
        FunctionCaller fc = FunctionCaller.getInstance();
        successfullyChangedBitsSet = new HashSet<>();

        int status;
        int lastStatus = getAccountStatus();

        final int maxChangeCount = 2;
        String resp;
        BigDecimal expectedFee;
        // 16 bits
        for (int i = 1, k = 1; k <= 16; i = i << 1, k++) {

            int successfulChangeCount = 0;
            for (int j = 0; j < maxChangeCount; j++) {

                if ((lastStatus & i) == 0) {
                    resp = fc.setAccountStatus(userData, address, i);
                    expectedFee = EscConst.SET_USER_STATUS_FEE;
                } else {
                    resp = fc.unsetAccountStatus(userData, address, i);
                    expectedFee = EscConst.UNSET_USER_STATUS_FEE;
                }

                JsonObject o = Utils.convertStringToJsonObject(resp);
                if (o.has("error")) {
                    String errorDesc = o.get("error").getAsString();
                    log.debug("Error occurred: {}", errorDesc);

                    boolean isExpectedErrorDesc = EscConst.Error.CHANGE_STATUS_FAILED.equals(errorDesc)
                            || (EscConst.Error.CHANGE_STATUS_REMOTE_FAILED.equals(errorDesc) && !userData.isAccountFromSameNode(address));

                    String reason = new AssertReason.Builder().req(fc.getLastRequest()).res(resp)
                            .msg("Unexpected error during account status change: " + errorDesc).build();
                    assertThat(reason, isExpectedErrorDesc);

                } else {
                    if (!EscUtils.isTransactionAcceptedByNode(o)) {
                        Assert.fail(new AssertReason.Builder()
                                .msg("User status change was not accepted by node.").build());
                    }

                    String reason;
                    //check fee
                    BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
                    reason = new AssertReason.Builder().msg("Invalid fee computed.")
                            .req(fc.getLastRequest()).res(resp).build();
                    assertThat(reason, fee, comparesEqualTo(expectedFee));

                    // check deduct - expected deduct is equal to expected fee
                    BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
                    reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                            .req(fc.getLastRequest()).res(resp).build();
                    assertThat(reason, deduct, comparesEqualTo(expectedFee));
                }

                status = getAccountStatus();
                log.debug("lastStatus: {}", convertIntToBinaryString16b(lastStatus));
                log.debug("         i: {} bit {}", convertIntToBinaryString16b(i), k);
                log.debug("    status: {}", convertIntToBinaryString16b(status));

                if ((status ^ lastStatus) == i) {
                    log.debug("    result: correct");
                    successfulChangeCount++;
                } else {
                    log.debug("    result: incorrect");
                }

                lastStatus = status;
            }

            if (successfulChangeCount == maxChangeCount) {
                successfullyChangedBitsSet.add(k);
            } else if (successfulChangeCount > 0) {
                // some changes were successful, some not
                // this is wrong behaviour, because if bit could be changed, all changes should be possible
                Assert.fail("Not all changes were possible for bit " + k);
            }
        }
    }


    @When("^user changes node status$")
    public void user_changes_node_status() {
        FunctionCaller fc = FunctionCaller.getInstance();
        successfullyChangedBitsSet = new HashSet<>();

        final int maxChangeCount = 2;
        String resp;
        BigDecimal expectedFee;

        int groupCnt = chgNodePairList.size();
        // 32 bits
        int bitNo = 32;

        int status;
        int startBit = 0;
        int curBit;


        while (startBit < bitNo) {
            Map<Integer, Integer> statusChangeCountMap = new HashMap<>();

            for (int i = 0; i < maxChangeCount; i++) {
                Map<Integer, StatusChange> statusChangeMap = new HashMap<>();

                // change node status request block
                curBit = startBit;
                Map<String, Integer> statusMap = getNodeStatusMap(chgNodePairList.get(0).getUser());
                do {

                    // send request
                    int curBitValue = 1 << curBit;
                    ChangeNodePair cnp = chgNodePairList.get(curBit % groupCnt);
                    String nodeId = cnp.getNodeId();

                    assertThat(String.format("Cannot get node %s status.", nodeId), statusMap.get(nodeId), notNullValue());
                    status = statusMap.get(nodeId);
                    StatusChange sc = new StatusChange();
                    sc.setStatusBefore(status);
                    statusChangeMap.put(curBit, sc);

                    final boolean isBitNotSet = (status & curBitValue) == 0;
                    if (isBitNotSet) {
                        expectedFee = EscConst.SET_BANK_STATUS_FEE;
                    } else {
                        expectedFee = EscConst.UNSET_BANK_STATUS_FEE;
                    }

                    Set<UserData> userDataSet = cnp.getUserDataSet();
                    for (UserData u : userDataSet) {
                        if (isBitNotSet) {
                            resp = fc.setNodeStatus(u, nodeId, curBitValue);
                        } else {
                            resp = fc.unsetNodeStatus(u, nodeId, curBitValue);
                        }

                        JsonObject o = Utils.convertStringToJsonObject(resp);
                        if (o.has("error")) {
                            String errorDesc = o.get("error").getAsString();
                            log.debug("Error occurred: {}", errorDesc);

                            boolean isExpectedErrorDesc = EscConst.Error.CHANGE_STATUS_FAILED.equals(errorDesc)
                                    || EscConst.Error.CHANGE_NODE_STATUS_FAILED.equals(errorDesc);

                            String reason = new AssertReason.Builder().req(fc.getLastRequest()).res(resp)
                                    .msg("Unexpected error during node status change: " + errorDesc).build();
                            assertThat(reason, isExpectedErrorDesc);
                        } else {
                            if (!EscUtils.isTransactionAcceptedByNode(o)) {
                                Assert.fail(new AssertReason.Builder()
                                        .msg("Node status change was not accepted by node.").build());
                            }

                            String reason;
                            //check fee
                            BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
                            reason = new AssertReason.Builder().msg("Invalid fee computed.")
                                    .req(fc.getLastRequest()).res(resp).build();
                            assertThat(reason, fee, comparesEqualTo(expectedFee));

                            // check deduct - expected deduct is equal to expected fee
                            BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
                            reason = new AssertReason.Builder().msg("Invalid deduct computed.")
                                    .req(fc.getLastRequest()).res(resp).build();
                            assertThat(reason, deduct, comparesEqualTo(expectedFee));
                        }
                    }

                } while ((++curBit % groupCnt != 0) && ((curBit < bitNo)));

                // wait for block
                EscUtils.waitForNextBlock();
                EscUtils.waitForNextBlock();
                statusMap = getNodeStatusMap(chgNodePairList.get(0).getUser());

                // get node status response block
                curBit = startBit;
                do {
                    // get node status
                    ChangeNodePair cnp = chgNodePairList.get(curBit % groupCnt);
                    String nodeId = cnp.getNodeId();

                    String reason = new AssertReason.Builder().msg(String.format("Cannot get node %s status", nodeId))
                            .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
                    assertThat(reason, statusMap.get(nodeId), notNullValue());
                    status = statusMap.get(nodeId);
                    statusChangeMap.get(curBit).setStatusAfter(status);

                } while ((++curBit % groupCnt != 0) && ((curBit < bitNo)));

                for (Integer k : statusChangeMap.keySet()) {
                    int curBitValue = 1 << k;
                    StatusChange sc = statusChangeMap.get(k);

                    log.debug("lastStatus: {}", convertIntToBinaryString32b(sc.getStatusBefore()));
                    log.debug("         i: {} bit {}", convertIntToBinaryString32b(curBitValue), k + 1);
                    log.debug("    status: {}", convertIntToBinaryString32b(sc.getStatusAfter()));

                    if ((sc.getStatusAfter() ^ sc.getStatusBefore()) == curBitValue) {
                        log.debug("    result: correct");
                        if (statusChangeCountMap.containsKey(k)) {
                            // replace with incremented value
                            Integer changeCount = statusChangeCountMap.get(k) + 1;
                            statusChangeCountMap.replace(k, changeCount);
                        } else {
                            // put new value
                            statusChangeCountMap.put(k, 1);
                        }
                    } else {
                        log.debug("    result: incorrect");
                    }
                }

                log.info("Performed change {} of node status bit ({}-{}).", i + 1, startBit, curBit - 1);
            }

            for (Integer k : statusChangeCountMap.keySet()) {
                int changeCount = statusChangeCountMap.get(k);
                if (changeCount == maxChangeCount) {
                    successfullyChangedBitsSet.add(k + 1);
                } else if (changeCount > 0) {
                    // some changes were successful, some not
                    // this is wrong behaviour, because if bit could be changed, all changes should be possible
                    Assert.fail("Not all changes were possible for bit " + (k + 1));
                }
            }

            startBit += groupCnt;
        }
    }

    /**
     * @param availableBits bits that can be changed, if empty ("") - no bits can be changed
     *                      format: bitRange1[,bitRange2][,bitRange3]...[,bitRangeN]
     *                      bitRange format: N[-M], ex. "4" means only 4th bit, "3-6" - 3rd, 4th, 5th, 6th bit
     */
    @Then("^only (.*) could be changed$")
    public void every_change_is_performed_as_expected(String availableBits) {
        Set<Integer> availableBitsSet = new HashSet<>();
        if (!"".equals(availableBits)) {
            String[] bitRangeArr = availableBits.split(",");
            for (String bitRange : bitRangeArr) {
                String[] arr = bitRange.split("-");
                int startBit = Integer.parseInt(arr[0]);
                int endBit = (arr.length == 2) ? Integer.parseInt(arr[1]) : startBit;
                int curBit = startBit;
                while (curBit <= endBit) {
                    availableBitsSet.add(curBit);
                    curBit++;
                }
            }
        }

        log.debug("expected set: {}", formatSetLog(availableBitsSet));
        log.debug("  result set: {}", formatSetLog(successfullyChangedBitsSet));

        assertThat("Changed bit set is different than expected.",
                successfullyChangedBitsSet, equalTo(availableBitsSet));
    }

    private String convertIntToBinaryString16b(int i) {
        return String.format("%16s", Integer.toBinaryString(i)).replace(' ', '0');
    }

    private String convertIntToBinaryString32b(int i) {
        return String.format("%32s", Integer.toBinaryString(i)).replace(' ', '0');
    }

    /**
     * Returns account status from get_account function response.
     *
     * @return account status
     */
    private int getAccountStatus() {
        JsonObject o = Utils.convertStringToJsonObject(FunctionCaller.getInstance().getAccount(userData, address));
        return o.getAsJsonObject("account").get("status").getAsInt();
    }

    /**
     * Gets nodes status from get_block function response.
     *
     * @param userData user data
     * @return nodes status in map:<br />
     * - key: node id in hex format - as in account address,<br />
     * - value: status <br />
     */
    private Map<String, Integer> getNodeStatusMap(UserData userData) {
        Map<String, Integer> m = new HashMap<>();

        String resp = FunctionCaller.getInstance().getBlock(userData);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
        for (JsonElement je : arr) {
            JsonObject nodeEntry = je.getAsJsonObject();
            m.put(nodeEntry.get("id").getAsString(), nodeEntry.get("status").getAsInt());
        }

        String reason = new AssertReason.Builder().msg("Cannot get any node status.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(resp).build();
        assertThat(reason, m.size() > 0);
        return m;
    }

    private String formatSetLog(Set<Integer> intSet) {
        final String delimiter = ", ";

        StringBuilder sb = new StringBuilder();
        if (intSet.size() > 0) {
            for (Integer i : intSet) {
                sb.append(i);
                sb.append(delimiter);
            }
            sb.setLength(sb.length() - delimiter.length());
        }
        return sb.toString();
    }

    @When("^user tries to set out of range status$")
    public void user_tries_set_oor_acc_status() {
        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 15; i++) {
            sb.append('0');
        }

        while (sb.length() < 65) {
            sb.append('0');
            String outOfRangeStatusBin = sb.toString();
            isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(
                    FunctionCaller.getInstance().setAccountStatus(userData, address, outOfRangeStatusBin));
            if (isTransactionAccepted) {
                break;
            }
            isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(
                    FunctionCaller.getInstance().unsetAccountStatus(userData, address, outOfRangeStatusBin));
            if (isTransactionAccepted) {
                break;
            }
        }

    }

    @When("^user tries to set out of range node status$")
    public void user_tries_set_oor_node_status() {
        UserData ud = chgNodePairList.get(0).getUser();
        String nodeId = chgNodePairList.get(0).getNodeId();

        StringBuilder sb = new StringBuilder("1");
        for (int i = 0; i < 31; i++) {
            sb.append('0');
        }

        while (sb.length() < 65) {
            sb.append('0');
            String outOfRangeStatusBin = sb.toString();
            isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(
                    FunctionCaller.getInstance().setNodeStatus(ud, nodeId, outOfRangeStatusBin));
            if (isTransactionAccepted) {
                break;
            }
            isTransactionAccepted = EscUtils.isTransactionAcceptedByNode(
                    FunctionCaller.getInstance().unsetNodeStatus(ud, nodeId, outOfRangeStatusBin));
            if (isTransactionAccepted) {
                break;
            }
        }
    }

    @Then("^change status transaction is rejected$")
    public void changeStatusTransactionIsRejected() {
        String reason = new AssertReason.Builder().msg("Change status transaction was accepted.")
                .req(FunctionCaller.getInstance().getLastRequest())
                .res(FunctionCaller.getInstance().getLastResponse())
                .build();
        assertThat(reason, !isTransactionAccepted);
    }

    @Given("^(\\d+) users, who will check get_account function$")
    public void users_who_will_check_get_account_function(int userCount) {
        userDataList = UserDataProvider.getInstance().getUserDataFromDifferentNodes(userCount);
    }

    @When("^they call get_account$")
    public void they_call_get_account() {
        FunctionCaller fc = FunctionCaller.getInstance();
        getAccountResponses = new String[3][userDataList.size() * userDataList.size()];
        int row = 0;
        for (UserData userCalled : userDataList) {
            String requestedAddress = userCalled.getAddress();
            for (UserData userCaller : userDataList) {
                String callerAddress = userCaller.getAddress();
                String resp = fc.getAccount(userCaller, requestedAddress);
                getAccountResponses[0][row] = requestedAddress;
                getAccountResponses[1][row] = callerAddress;
                getAccountResponses[2][row] = resp;
                ++row;
            }
        }
    }

    @Then("^all get_account responses are correct$")
    public void all_get_account_responses_are_correct() {
        int rows = userDataList.size() * userDataList.size();
        // last row doesn't need to be compared
        for (int row = 0; row < rows - 1; row++) {
            String requestedAddress1 = getAccountResponses[0][row];
            String callerAddress1 = getAccountResponses[1][row];
            String resp1 = getAccountResponses[2][row];

            for (int crow = row + 1; crow < rows; crow++) {
                if (!requestedAddress1.equals(getAccountResponses[0][crow])) {
                    continue;
                }

                String callerAddress2 = getAccountResponses[1][crow];
                String resp2 = getAccountResponses[2][crow];

                JsonObject respAccount1 = Utils.convertStringToJsonObject(resp1).getAsJsonObject("network_account");
                JsonObject respAccount2 = Utils.convertStringToJsonObject(resp2).getAsJsonObject("network_account");

                if (!respAccount1.equals(respAccount2)) {
                    AssertReason.Builder arb = new AssertReason.Builder().msg("Requested account " + requestedAddress1)
                            .msg("by1: " + callerAddress1).msg("resp1: " + resp1)
                            .msg("by2: " + callerAddress2).msg("resp2: " + resp2);

                    if (respAccount1.size() != respAccount2.size()) {
                        Assert.fail(arb.msg("Different number of keys in JsonObject.").build());
                    } else {
                        for (String key1 : respAccount1.keySet()) {
                            if (!respAccount2.has(key1)) {
                                Assert.fail(arb.msg("Missing key: " + key1).build());
                            } else {
                                String val1 = respAccount1.get(key1).getAsString();
                                String val2 = respAccount2.get(key1).getAsString();

                                if (!val1.equals(val2)) {
                                    Assert.fail(arb.msg("Different value of key: " + key1)
                                            .msg("in resp1: " + val1).msg("in resp2: " + val2).build());
                                }
                            }
                        }
                    }
                    Assert.fail(arb.msg("Difference not recognized.").build());
                }
            }
        }

    }

    class ChangeNodePair {

        ChangeNodePair(Set<UserData> userDataSet, String nodeId) {
            this.userDataSet = userDataSet;
            this.nodeId = nodeId;
        }

        Set<UserData> getUserDataSet() {
            return userDataSet;
        }

//        public void setUserDataSet(Set<UserData> userDataSet) {
//            this.userDataSet = userDataSet;
//        }

        UserData getUser() {
            return userDataSet.size() > 0 ? userDataSet.iterator().next() : null;
        }

        String getNodeId() {
            return nodeId;
        }

//        public void setNodeId(String nodeId) {
//            this.nodeId = nodeId;
//        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ChangeNodePair{");
            sb.append("userDataSet=<");

            for (UserData ud : userDataSet) {
                sb.append(ud.getAddress());
            }
            sb.append(">, nodeId='");
            sb.append(nodeId);
            sb.append("'}");
            return sb.toString();
        }

        private Set<UserData> userDataSet;
        private String nodeId;

    }

    class StatusChange {

        int getStatusBefore() {
            return statusBefore;
        }

        void setStatusBefore(int statusBefore) {
            this.statusBefore = statusBefore;
        }

        int getStatusAfter() {
            return statusAfter;
        }

        void setStatusAfter(int statusAfter) {
            this.statusAfter = statusAfter;
        }

        private int statusBefore;
        private int statusAfter;

    }

}
