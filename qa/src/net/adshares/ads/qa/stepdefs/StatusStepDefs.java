package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.EscConst;
import net.adshares.ads.qa.util.EscUtils;
import net.adshares.ads.qa.util.FunctionCaller;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     *
     */
    private Set<Integer> successfullyChangedBitsSet;

    @Given("^(main|regular) account user, who wants to change (own|local|remote) status$")
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

        Assert.assertTrue("Cannot find suitable data", isDataFound);
    }

    @Given("^(main|regular) account user, who wants to change (own|remote) node status$")
    public void user_who_wants_to_change_node_status(String accountType, String otherAccountType) {

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
                        if ("remote".equals(otherAccountType) && !isSameNode) {
                            address = addressU2;
                            isDataFound = true;
                            break;
                        }
                    }
                }

                if (isDataFound) {
                    address = address.substring(0, 4);
                    break;
                }
            }
        }

        Assert.assertTrue("Cannot find suitable data", isDataFound);
    }

    @When("^user changes status$")
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
                    log.info("Error occurred: {}", errorDesc);

                    boolean isExpectedErrorDesc = EscConst.Error.CHANGE_STATUS_FAILED.equals(errorDesc)
                            || (EscConst.Error.CHANGE_STATUS_REMOTE_FAILED.equals(errorDesc) && !userData.isAccountFromSameNode(address));

                    Assert.assertTrue("Unexpected error during account status change.", isExpectedErrorDesc);
                } else {
                    BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
                    BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
                    Assert.assertEquals("Invalid fee computed", 0, expectedFee.compareTo(fee));
                    // expected deduct is equal to expected fee
                    Assert.assertEquals("Invalid deduct computed", 0, expectedFee.compareTo(deduct));
                }

                status = getAccountStatus();
                log.info("lastStatus: {}", convertIntToBinaryString16b(lastStatus));
                log.info("         i: {}", convertIntToBinaryString16b(i));
                log.info("    status: {}", convertIntToBinaryString16b(status));

                if ((status ^ lastStatus) == i) {
                    log.info("    result: correct");
                    successfulChangeCount++;
                } else {
                    log.info("    result: incorrect");
                }

                lastStatus = status;
            }

            if (successfulChangeCount == maxChangeCount) {
                successfullyChangedBitsSet.add(k);
            } else if (successfulChangeCount > 0) {
                // some changes were successful, some not
                // this is wrong behaviour, because if bit could be changed, all changes should be possible
                Assert.fail("Not all changes were possible");
            }
        }
    }

    @When("^user changes node status$")
    public void user_changes_node_status() {
        FunctionCaller fc = FunctionCaller.getInstance();
        successfullyChangedBitsSet = new HashSet<>();

        int status;
        int lastStatus = getNodeStatus();

        final int maxChangeCount = 2;
        String resp;
        BigDecimal expectedFee;
        // 32 bits
        for (int i = 1, k = 1; k <= 32; i = i << 1, k++) {

            int successfulChangeCount = 0;
            for (int j = 0; j < maxChangeCount; j++) {

                if ((lastStatus & i) == 0) {
                    resp = fc.setNodeStatus(userData, address, i);
                    expectedFee = EscConst.SET_BANK_STATUS_FEE;
                } else {
                    resp = fc.unsetNodeStatus(userData, address, i);
                    expectedFee = EscConst.UNSET_BANK_STATUS_FEE;
                }

                JsonObject o = Utils.convertStringToJsonObject(resp);
                if (o.has("error")) {
                    String errorDesc = o.get("error").getAsString();
                    log.info("Error occurred: {}", errorDesc);

                    boolean isExpectedErrorDesc = EscConst.Error.CHANGE_NODE_STATUS_FAILED.equals(errorDesc);

                    Assert.assertTrue("Unexpected error during account status change.", isExpectedErrorDesc);
                } else {
                    BigDecimal fee = o.getAsJsonObject("tx").get("fee").getAsBigDecimal();
                    BigDecimal deduct = o.getAsJsonObject("tx").get("deduct").getAsBigDecimal();
                    Assert.assertEquals("Invalid fee computed", 0, expectedFee.compareTo(fee));
                    // expected deduct is equal to expected fee
                    Assert.assertEquals("Invalid deduct computed", 0, expectedFee.compareTo(deduct));
                }

                EscUtils.waitForNextBlock();
                status = getNodeStatus();
                log.info("lastStatus: {}", convertIntToBinaryString32b(lastStatus));
                log.info("         i: {}", convertIntToBinaryString32b(i));
                log.info("    status: {}", convertIntToBinaryString32b(status));

                if ((status ^ lastStatus) == i) {
                    log.info("    result: correct");
                    successfulChangeCount++;
                } else {
                    log.info("    result: incorrect");
                }

                lastStatus = status;
            }

            if (successfulChangeCount == maxChangeCount) {
                successfullyChangedBitsSet.add(k);
            } else if (successfulChangeCount > 0) {
                // some changes were successful, some not
                // this is wrong behaviour, because if bit could be changed, all changes should be possible
                Assert.fail("Not all changes were possible");
            }
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

        log.info("expected set: {}", formatSetLog(availableBitsSet));
        log.info("  result set: {}", formatSetLog(successfullyChangedBitsSet));

        Assert.assertEquals("Changed bit set is different than expected.",
                availableBitsSet, successfullyChangedBitsSet);
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
     * Returns node status from get_account function response.
     *
     * @return node status
     */
    private int getNodeStatus() {
        // adress is node id in hex format - as in account address
        String nodeId = address;
        int status = -1;

        int attempt = 0;
        int attemptMax = 4;
        while (attempt++ < attemptMax) {
            String resp = FunctionCaller.getInstance().getBlock(userData);
            JsonObject o = Utils.convertStringToJsonObject(resp);
            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();
                log.info("Error occurred: {}", errorDesc);
                Assert.assertEquals("Unexpected error after account creation.",
                        EscConst.Error.GET_BLOCK_INFO_FAILED, errorDesc);
            } else {
                JsonArray arr = o.getAsJsonObject("block").getAsJsonArray("nodes");
                for (JsonElement je :                arr) {
                    JsonObject nodeEntry = je.getAsJsonObject();
                    if (nodeId.equals(nodeEntry.get("id").getAsString())) {
                        status = nodeEntry.get("status").getAsInt();
                        return status;
                    }
                }
            }

            Assert.assertTrue("Cannot get block info after delay", attempt < attemptMax);
            // block info is not available for short time after block change,
            // therefore there is 3 s delay - it cannot be "wait for next block"
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Assert.assertNotEquals("Cannot get node status", -1, status);
        return status;
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
}
