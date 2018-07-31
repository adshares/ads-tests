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

package net.adshares.ads.qa.caller.command;

import java.util.List;

public abstract class AbstractTransaction extends AbstractCommand {
    /**
     * Default value for optional int fields. Default value means that field is not set and should not be used.
     */
    private static final int INVALID = -1;

    private String accountHash;
    private int accountMsid = INVALID;
    private String sender;
    private String signature;
    private int time = INVALID;

    /**
     * @param accountHash account hash
     */
    public void setAccountHash(String accountHash) {
        this.accountHash = accountHash;
    }

    /**
     * @param accountMsid number of last sent message
     */
    public void setAccountMsid(int accountMsid) {
        this.accountMsid = accountMsid;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    /**
     * @param time user timestamp of transaction
     */
    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public List<String> getParameters() {
        List<String> params = super.getParameters();
        if (accountHash != null) {
            params.add(String.format("\"hash\":\"%s\"", accountHash));
        }
        if (accountMsid > INVALID) {
            params.add(String.format("\"msid\":%d", accountMsid));
        }
        if (sender != null) {
            params.add(String.format("\"sender\":\"%s\"", sender));
        }
        if (signature != null) {
            params.add(String.format("\"signature\":\"%s\"", signature));
        }
        if (time > INVALID) {
            params.add(String.format("\"time\":%d", time));
        }
        return params;
    }

    @Override
    public String toStringCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');

        if (accountHash == null || accountMsid <= INVALID) {
            sb.append("echo '{\"run\":\"get_me\"}';");
        }

        sb.append(super.toStringCommand());

        sb.append(')');
        return sb.toString();
    }
}
