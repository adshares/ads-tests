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

import net.adshares.ads.qa.util.EscConst;

import java.math.BigDecimal;

public class UserData {

    private final String port;
    private final String host;
    private final String address;
    private String secret;

    public UserData(String port, String host, String address, String secret) {
        this.port = port;
        this.host = host;
        this.address = address;
        this.secret = secret;
    }

    String getPort() {
        return port;
    }

    String getHost() {
        return host;
    }

    public String getAddress() {
        return address;
    }

    /**
     * @return node ordinal number (decimal)
     */
    public int getNode() {
        return Integer.valueOf(getNodeId(), 16);
    }

    /**
     * @return node id (hexadecimal)
     */
    public String getNodeId() {
        return address.substring(0, 4);
    }

    public String getSecret() {
        return secret;
    }

    /**
     * @return data as ESC client parameters
     */
    public String getDataAsEscParams() {
        return String.format(" -P%s -H%s -A%s -s%s", port, host, address, secret);
    }

    /**
     * Sets account private key.
     *
     * @param secret new secret key
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Checks if it is main account data.
     *
     * @return true, if data belongs to main account in node
     */
    public boolean isMainAccount() {
        return isMainAccount(this.address);
    }

    /**
     * Checks if address belongs to main account.
     *
     * @param address account address
     * @return true, if address belongs to main account in node, false otherwise
     */
    public static boolean isMainAccount(String address) {
        if (address != null && address.length() == 18) {
            return "00000000".equals(address.substring(5, 13));
        } else {
            return false;
        }
    }

    /**
     * Checks if address belongs to account in the same node.
     *
     * @param address account address
     * @return true, if addresses belong to account in the same node, false otherwise
     */
    public boolean isAccountFromSameNode(String address) {
        return isAccountFromSameNode(this.address, address);
    }

    /**
     * Checks if two addresses belong to accounts in the same node.
     *
     * @param address1 first account address
     * @param address2 second account address
     * @return true, if addresses belong to accounts in the same node, false otherwise
     */
    public static boolean isAccountFromSameNode(String address1, String address2) {
        if (address1 != null && address1.length() == 18 && address2 != null && address2.length() == 18) {
            return address1.substring(0, 4).equals(address2.substring(0, 4));
        } else {
            return false;
        }
    }

    /**
     * @return minimal allowed balance after successful transfer
     */
    public BigDecimal getMinAllowedBalance() {
        return isMainAccount() ? EscConst.BANK_MIN_UMASS : EscConst.USER_MIN_MASS;
    }
}
