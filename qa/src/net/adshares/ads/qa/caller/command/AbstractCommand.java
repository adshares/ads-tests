package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

import java.util.List;

public abstract class AbstractCommand {

    UserData senderData;

    /**
     * @return data of user, who will execute command
     */
    public UserData getSenderData() {
        return senderData;
    }

    /**
     * @return command name, used as value of "run" field
     */
    protected abstract String getName();

    /**
     * @return list of parameters (fields and its' values)
     */
    protected abstract List<String> getParameters();

    /**
     * @return command for ADS client
     */
    public abstract String toStringCommand();

    /**
     * @return parameters description
     */
    public abstract String toStringLogger();
}
