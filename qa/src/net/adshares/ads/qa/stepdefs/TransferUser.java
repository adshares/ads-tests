package net.adshares.ads.qa.stepdefs;


import net.adshares.ads.qa.util.LogEventTimestamp;
import net.adshares.ads.qa.data.UserData;

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