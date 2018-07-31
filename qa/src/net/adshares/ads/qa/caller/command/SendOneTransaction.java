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

public class SendOneTransaction extends AbstractTransaction {

    private String receiverAddress;
    private String amount;
    private String message;

    /**
     * @param senderData      senderData data
     * @param receiverAddress receiver address
     * @param amount          transfer amount
     */
    public SendOneTransaction(UserData senderData, String receiverAddress, String amount) {
        this.senderData = senderData;
        this.receiverAddress = receiverAddress;
        this.amount = amount;
    }

    @Override
    public String getName() {
        return "send_one";
    }

    @Override
    public List<String> getParameters() {
        List<String> list = super.getParameters();
        list.add(String.format("\"address\":\"%s\"", receiverAddress));
        list.add(String.format("\"amount\":\"%s\"", amount));
        if (message != null) {
            list.add(String.format("\"message\":\"%s\"", message));
        }
        return list;
    }

    /**
     * @param message message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toStringLogger() {
        return String.format("sendOne %s->%s: %s", senderData.getAddress(), receiverAddress, amount);
    }
}
