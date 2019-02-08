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

import net.adshares.ads.qa.data.UserData;

import java.util.List;

public class BroadcastTransaction extends AbstractTransaction {

    private String asciiMessage;
    private String hexMessage;

    /**
     * @param senderData senderData data
     */
    public BroadcastTransaction(UserData senderData) {
        this.senderData = senderData;
    }

    @Override
    public String getName() {
        return "broadcast";
    }

    @Override
    public List<String> getParameters() {
        List<String> list = super.getParameters();
        if (asciiMessage != null) {
            list.add(String.format("\"message_ascii\":\"%s\"", asciiMessage));
        }
        if (hexMessage != null) {
            list.add(String.format("\"message\":\"%s\"", hexMessage));
        }

        return list;
    }

    /**
     * @param message hex message
     */
    public void setHexMessage(String message) {
        this.hexMessage = message;
    }

    /**
     * @param message ASCII message
     */
    public void setAsciiMessage(String message) {
        this.asciiMessage = message;
    }

    @Override
    public String toStringLogger() {
        return "broadcast";
    }
}
