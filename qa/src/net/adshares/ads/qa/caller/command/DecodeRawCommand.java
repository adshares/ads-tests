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

public class DecodeRawCommand extends AbstractCommand {

    private String data;
    private String signature;

    /**
     * @param senderData sender data
     * @param data       hexadecimal encoded command data
     */
    public DecodeRawCommand(UserData senderData, String data) {
        this.senderData = senderData;
        this.data = data;
    }

    /**
     * @param senderData sender data
     * @param data       hexadecimal encoded command data
     * @param signature  command signature
     */
    public DecodeRawCommand(UserData senderData, String data, String signature) {
        this.senderData = senderData;
        this.data = data;
        this.signature = signature;
    }

    @Override
    public String getName() {
        return "decode_raw";
    }

    @Override
    public List<String> getParameters() {
        List<String> list = super.getParameters();
        // append data
        list.add(String.format("\"data\":\"%s\"", data));
        // append signature
        if (signature != null) {
            list.add(String.format("\"signature\":\"%s\"", signature));
        }
        return list;
    }

    /**
     * @param signature command signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toStringLogger() {
        return "decode_raw";
    }
}
