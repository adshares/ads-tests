package net.adshares.ads.qa.caller.command;

public interface CommandInterface {

    /**
     * @return command for ADS client
     */
    String toStringCommand();

    /**
     * @return parameters description
     */
    String toStringLogger();
}
