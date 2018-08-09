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
import net.adshares.ads.qa.caller.command.SendManyTransaction;
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

public class FeeSharingStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * list of vip node ids
     */
    private List<String> vipList;
    /**
     * list of top node ids
     */
    private List<String> topList;
    private UserData userData;
    private long blockTime;
    private Map<String, ProfitData> userProfitMap;

    @Given("^top group (.+) vip group$")
    public void top_and_vip_group(String type) {
        boolean isTopAndVipSeparated = "is separated from".equals(type);
        boolean isEveryVipInTop = "includes all from".equals(type);

        vipList = getVipList();
        topList = getTopList(isTopAndVipSeparated, isEveryVipInTop);
        assertThat("Cannot create top list with enough elements.", topList.size(), comparesEqualTo(EscConst.TOP_MAX));

        // get user, who will be operator of first node in top group
        String userNodeId = topList.get(0);
        userData = null;
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();
        for (UserData u : userDataList) {
            if (u.isMainAccount() && userNodeId.equals(u.getNodeId())) {
                userData = u;
                break;
            }
        }
        log.debug("user {}", userData.getAddress());
        assertThat("Cannot find user.", userData, notNullValue());

        FunctionCaller fc = FunctionCaller.getInstance();

        BigDecimal amount = new BigDecimal("10");
        Map<String, String> wires = new HashMap<>();
        for (int i = topList.size() - 1; i >= 0; i--) {
            String address = topList.get(i).concat("-00000000-XXXX");
            wires.put(address, amount.toPlainString());
            amount = amount.multiply(new BigDecimal("2"));
        }
        SendManyTransaction command = new SendManyTransaction(userData, wires);
        String resp = fc.sendMany(command);
        JsonObject o = Utils.convertStringToJsonObject(resp);
        String reason = new AssertReason.Builder().msg("Transaction was not accepted by node.")
                .req(fc.getLastRequest()).res(resp).build();
        assertThat(reason, EscUtils.isTransactionAcceptedByNode(o));
        log.debug("fee : {}", o.getAsJsonObject("tx").get("fee").getAsString());

        blockTime = o.get("current_block_time").getAsInt();
        String blockTimeHex = Long.toHexString(blockTime);
        log.debug("blockTime (hex): {}", blockTimeHex);
    }

    @When("^collect all logs$")
    public void collect_all_logs() {
        List<UserData> userList = UserDataProvider.getInstance().getUserDataList();
        userList.removeIf(user -> !user.isMainAccount() || !(topList.contains(user.getNodeId()) || vipList.contains(user.getNodeId())));

        EscUtils.waitForNextBlock();

        FunctionCaller fc = FunctionCaller.getInstance();

        // select user, different than that who send transfer
        // that user will check, if transfer was accepted by network
        UserData u = null;
        for (UserData user : userList) {
            if (!userData.getNodeId().equals(user.getNodeId())) {
                u = user;
            }
        }
        String blockId = getBlockId(u);
        assertThat("BlockId is null", blockId, notNullValue());


        LogChecker lc = new LogChecker();
        LogFilter lf = new LogFilter(true);
        lf.addFilter("type", "bank_profit");
        lf.addFilter("block_id", blockId);


        userProfitMap = new HashMap<>();
        for (UserData user : userList) {
            lc.setResp(fc.getLog(user, blockTime));

            JsonArray arr = lc.getFilteredLogArray(lf);
            if (arr.size() > 0) {
                long time = arr.get(0).getAsJsonObject().get("time").getAsLong();
                lc.setResp(fc.getLog(user, time));

                LogFilter bpLf = new LogFilter(true);
                bpLf.addFilter("time", Long.toString(time));
                bpLf.addFilter("type", "bank_profit");

                JsonArray bpArr = lc.getFilteredLogArray(bpLf);
                String nodeId = user.getNodeId();
                log.debug("{}:", nodeId);
                ProfitData profitData = new ProfitData();
                BigDecimal userProfit = BigDecimal.ZERO;
                for (JsonElement el : bpArr) {
                    JsonObject o = el.getAsJsonObject();

                    BigDecimal profit = o.get("profit").getAsBigDecimal();
                    if (o.has("profit_shared")) {
                        log.debug("\tprofit: {}", profit.toPlainString());
                        // if profit_shared is present, it must be subtracted from profitToShare
                        BigDecimal profitShared = o.get("profit_shared").getAsBigDecimal();
                        profitData.profitSharedLog = profitShared;
                        BigDecimal profitDiff = profit.subtract(profitShared);
                        userProfit = userProfit.add(profitDiff);
                        log.debug("\tshared: {}", profitShared.toPlainString());
                        log.debug("\tpro-sh: {}", profitDiff.toPlainString());
                    } else {
                        BigDecimal fee = o.get("fee").getAsBigDecimal();
                        log.debug("\tprofit: {}", profit.subtract(fee).toPlainString());
                        userProfit = userProfit.add(profit).subtract(fee);
                    }
                }
                String reason = new AssertReason.Builder()
                        .msg("Field 'profit_shared' is equal 0.")
                        .req(fc.getLastRequest())
                        .res(fc.getLastResponse())
                        .build();
                assertThat(reason, profitData.profitSharedLog, not(comparesEqualTo(BigDecimal.ZERO)));

                if (!topList.contains(nodeId)) {
                    // vip node, which doesn't belong to top, doesn't share profitToShare
                    profitData.profitToShare = BigDecimal.ZERO;
                } else {
                    profitData.profitToShare = userProfit;
                }
                userProfitMap.put(nodeId, profitData);
            } else {
                Assert.fail("Cannot find event with block id.");
            }
        }

        computeShare(userProfitMap);
    }

    @Then("^profit shared is as expected$")
    public void profit_shared_is_as_expected() {
        List<String> nodeIds = new ArrayList<>(userProfitMap.keySet());
        Collections.sort(nodeIds);

        log.debug("--------------------------------------------------------");
        for (String nodeId : nodeIds) {
            StringBuilder sb = new StringBuilder();
            sb.append(nodeId);
            sb.append(":\n");

            ProfitData profitData = userProfitMap.get(nodeId);
            BigDecimal expectedProfitShared = profitData.share.subtract(EscConst.getSharedProfitForTopVip(profitData.profitToShare));
            sb.append(String.format("\texp : %s-%s=%s\n", profitData.share.toPlainString(), profitData.profitToShare.toPlainString(), expectedProfitShared.toPlainString()));
            sb.append(String.format("\tact : %s\n", profitData.profitSharedLog));

            BigDecimal diff = expectedProfitShared.subtract(profitData.profitSharedLog);
            sb.append(String.format("\tdiff: %s\n", diff.toPlainString()));

            log.debug(sb.toString());
            assertThat(sb.toString(), profitData.profitSharedLog, comparesEqualTo(expectedProfitShared));
        }
        log.debug("--------------------------------------------------------");
    }

    private class ProfitData {
        /**
         * Amount which will be added to pool
         */
        BigDecimal profitToShare = BigDecimal.ZERO;
        /**
         * Value of profit_shared in get_log response
         */
        BigDecimal profitSharedLog = BigDecimal.ZERO;
        /**
         * Amount which will be taken from pool
         */
        BigDecimal share = BigDecimal.ZERO;
    }

    private void computeShare(Map<String, ProfitData> userProfitMap) {
        BigDecimal totalProfitToShare = BigDecimal.ZERO;
        List<ProfitData> profits = new ArrayList<>(userProfitMap.values());
        for (ProfitData profitData : profits) {
            totalProfitToShare = totalProfitToShare.add(profitData.profitToShare);
        }
        int shareCount = topList.size() + vipList.size();
        log.debug("totalProfitToShare: {}", totalProfitToShare.toPlainString());
        BigDecimal sharePerUser = totalProfitToShare.divide(new BigDecimal(shareCount), 11, BigDecimal.ROUND_FLOOR);
        log.debug("sharePerUser1     : {}", sharePerUser.toPlainString());
        sharePerUser = EscConst.getSharedProfitForTopVip(sharePerUser);
        log.debug("sharePerUser2     : {}", sharePerUser.toPlainString());

        for (String user : topList) {
            ProfitData profitData = userProfitMap.get(user);
            profitData.share = profitData.share.add(sharePerUser);
        }
        for (String user : vipList) {
            ProfitData profitData = userProfitMap.get(user);
            profitData.share = profitData.share.add(sharePerUser);
        }
    }

    /**
     * @param u user data
     * @return block id in which transfer was accepted
     */
    private String getBlockId(UserData u) {
        FunctionCaller fc = FunctionCaller.getInstance();
        LogChecker lc = new LogChecker();

        LogFilter smLf = new LogFilter(true);
        smLf.addFilter("type", "send_many");
        smLf.addFilter("confirmed", "yes");

        String blockId = null;
        int attempt = 0;
        final long delay = 3000L;
        final int attemptMax = 1 + (int) (4 * EscConst.BLOCK_PERIOD_MS / delay);
        while (attempt++ < attemptMax) {
            lc.setResp(fc.getLog(u, blockTime));

            JsonArray arr = lc.getFilteredLogArray(smLf);
            if (arr.size() > 0) {
                String time = arr.get(0).getAsJsonObject().get("time").getAsString();

                LogFilter biLf = new LogFilter(true);
                biLf.addFilter("time", time);
                biLf.addFilter("type", "bank_profit");
                JsonArray arrBi = lc.getFilteredLogArray(biLf);
                for (JsonElement el : arrBi) {
                    if (el.getAsJsonObject().has("block_id")) {
                        blockId = el.getAsJsonObject().get("block_id").getAsString();
                        break;
                    }
                }
                break;
            }
            assertThat("Cannot get confirmation in expected time.", attempt < attemptMax);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return blockId;
    }

    /**
     * @return list of vip node ids
     */
    private List<String> getVipList() {
        List<String> list = new ArrayList<>();

        Map<String, Integer> map = EscUtils.getNodeStatusMap(UserDataProvider.getInstance().getUserDataList(1).get(0));

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (EscUtils.isStatusVip(entry.getValue())) {
                list.add(entry.getKey());
            }
        }
        Collections.sort(list);

        return list;
    }

    /**
     * @return list of top node ids
     */
    private List<String> getTopList(boolean isTopAndVipSeparated, boolean isEveryVipInTop) {
        // create top list
        Map<String, Integer> map = EscUtils.getNodeStatusMap(UserDataProvider.getInstance().getUserDataList(1).get(0));
        List<String> nodeIds = new ArrayList<>(map.keySet());
        Collections.sort(nodeIds);
        // remove "0000" node id
        nodeIds.remove(0);

        List<String> list = new ArrayList<>();
        if (!isTopAndVipSeparated) {
            if (isEveryVipInTop) {
                // add all vips
                list.addAll(vipList);
            } else {
                // add some vips
                int nodeCount = nodeIds.size();
                int vipCount = vipList.size();
                int otherCount = nodeCount - vipCount;

                // at least half of vip group will be added
                int vipNeeded = Integer.max(EscConst.TOP_MAX - otherCount, vipCount / 2);
                assertThat("Not enough vip", vipNeeded, lessThanOrEqualTo(vipCount));

                for (int i = 0; i < vipNeeded; i++) {
                    list.add(vipList.get(i));
                }
            }
        }
        // complete list with no vip nodes
        for (String nodeId : nodeIds) {
            if (!"0000".equals(nodeId) && !list.contains(nodeId) && !vipList.contains(nodeId)) {
                list.add(nodeId);
            }
            if (list.size() >= EscConst.TOP_MAX) {
                break;
            }
        }
        return list;
    }
}
