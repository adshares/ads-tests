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


import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.util.LogEventTimestamp;

import java.math.BigDecimal;

public class TransferUser {

    private UserData userData;
    private BigDecimal startBalance;
    private BigDecimal expBalance;
    private TransferData transferData;
    private LogEventTimestamp lastEventTimestamp;

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public BigDecimal getStartBalance() {
        return startBalance;
    }

    public void setStartBalance(BigDecimal startBalance) {
        this.startBalance = startBalance;
    }

    public BigDecimal getExpBalance() {
        return expBalance;
    }

    public void setExpBalance(BigDecimal expBalance) {
        this.expBalance = expBalance;
    }

    public TransferData getTransferData() {
        return transferData;
    }

    public void setTransferData(TransferData transferData) {
        this.transferData = transferData;
    }

    public LogEventTimestamp getLastEventTimestamp() {
        return lastEventTimestamp;
    }

    public void setLastEventTimestamp(LogEventTimestamp lastEventTimestamp) {
        this.lastEventTimestamp = lastEventTimestamp;
    }
}