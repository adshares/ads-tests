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

package net.adshares.ads.qa.data;

import com.google.gson.Gson;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserDataProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());
    /**
     * System property directing to custom genesis file
     */
    private static final String SYSTEM_PROP_GENESIS_FILE = "genesis.file";
    /**
     * Default name of genesis file is genesis.json and location is main esc directory
     */
    private static final String DEFAULT_GENESIS_FILE = "genesis.json";
    private static final int STARTING_PORT_INT = 9001;
    private static final String HOST = "esc.dock";

    private static UserDataProvider instance;
    private List<UserData> users;

    private UserDataProvider() {

    }

    public static UserDataProvider getInstance() {
        if (instance == null) {
            instance = new UserDataProvider();
            instance.init();
        }
        return instance;
    }

    /**
     * Reads accounts data from genesis file.
     */
    private void init() {
        String genesisFile = System.getProperty(SYSTEM_PROP_GENESIS_FILE, DEFAULT_GENESIS_FILE);
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(genesisFile));
        } catch (FileNotFoundException e) {
            log.error(e.toString());
        }

        Assert.assertNotNull("Cannot open network definition file.", bufferedReader);

        Gson gson = new Gson();
        Genesis json = gson.fromJson(bufferedReader, Genesis.class);

        users = new ArrayList<>();

        int portInt = STARTING_PORT_INT;
        for (Genesis.Node gn : json.getNodes()) {
            String port = String.valueOf(portInt);
            for (Genesis.Node.Account acc : gn.getAccounts()) {
                UserData user = new UserData(port, HOST, acc.getAddress(), acc.getSecret());
                users.add(user);
            }
            portInt++;
        }
    }

    /**
     * Returns node count from genesis file.
     *
     * @return node count
     */
    public int getNodeCount() {
        Set<Integer> nodeSet = new HashSet<>();
        for (UserData user : users) {
            nodeSet.add(user.getNode());
        }
        return nodeSet.size();
    }

    public List<UserData> getUserDataList() {
        return getUserDataList(users.size());
    }

    public List<UserData> getUserDataList(int count) {
        return getUserDataList(count, false);
    }

    public List<UserData> getUserDataList(int count, boolean singleNode) {
        log.trace("getUserDataList(count=" + count + ", singleNode=" + singleNode + ")");

        ArrayList<UserData> userData = new ArrayList<>(count);
        UserData firstUser = null;
        for (UserData user : users) {
            if (singleNode) {
                if (firstUser == null) {
                    firstUser = user;
                    userData.add(firstUser);
                } else {
                    if (firstUser.isAccountFromSameNode(user.getAddress())) {
                        userData.add(user);
                    }
                }
            } else {
                userData.add(user);
            }

            if (userData.size() == count) {
                break;
            }
        }

        boolean enoughUsers = count == userData.size();
        if (!enoughUsers) {
            Assert.fail("Not enough users. Needed " + count + " but only " + userData.size() + " available.\n"
                    + "getUserDataList(count=" + count + ", singleNode=" + singleNode + ")");
        }

        return userData;
    }

    public List<UserData> getUserDataFromDifferentNodes(int count) {
        log.trace("getUserDataFromDifferentNodes(count=" + count + ")");

        ArrayList<UserData> userData = new ArrayList<>(count);
        for (UserData user : users) {
            String userAddress = user.getAddress();
            boolean isSameNode = false;
            for (UserData addedUser : userData) {
                isSameNode = addedUser.isAccountFromSameNode(userAddress);
                if (isSameNode) {
                    break;
                }
            }

            if (!isSameNode) {
                userData.add(user);
            }

            if (userData.size() == count) {
                break;
            }
        }

        boolean enoughUsers = count == userData.size();
        if (!enoughUsers) {
            Assert.fail("Not enough users. Needed " + count + " but only " + userData.size() + " available.\n"
                    + "getUserDataFromDifferentNodes(count=" + count + ")");
        }

        return userData;
    }

    /**
     * Makes copy of user data with given address and adds it to list.
     *
     * @param userData user data
     */
    public UserData cloneUser(UserData userData, String address) {
        if (userData != null && address != null) {

            boolean isAddressInList = false;
            for (UserData user : users) {
                if (address.equals(user.getAddress())) {
                    isAddressInList = true;
                    break;
                }
            }
            if (!isAddressInList) {
                String nodeId = address.substring(0, 4);
                for (UserData user : users) {
                    if (nodeId.equals(user.getNodeId())) {
                        String port = user.getPort();
                        String host = user.getHost();

                        UserData u = new UserData(port, host, address, userData.getSecret());
                        users.add(u);
                        return u;
                    }
                }

                // code below works only for local nodes due to default host (HOST constant)
                log.trace("Clone user to new node");
                int node = Integer.valueOf(nodeId, 16);
                int portAsInt = STARTING_PORT_INT + node - 1;
                UserData u = new UserData(String.valueOf(portAsInt), HOST, address, userData.getSecret());
                users.add(u);
                return u;
            }

        }
        return null;
    }

}
