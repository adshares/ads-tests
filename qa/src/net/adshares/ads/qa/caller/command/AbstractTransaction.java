package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

public abstract class AbstractTransaction implements CommandInterface {
    /**
     * INVALID_TIME means that time is not set and should not be used.
     */
    static final int INVALID_TIME = -1;

    UserData senderData;

    protected int time = INVALID_TIME;

    public UserData getSenderData() {
        return senderData;
    }

    /**
     * @param time user timestamp of transaction
     */
    public void setTime(int time) {
        this.time = time;
    }
}
