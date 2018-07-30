package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

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

    /**
     * @param message message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toStringCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append("echo '{\"run\":\"get_me\"}';");

        sb.append(String.format("echo '{\"run\":\"send_one\", \"address\":\"%s\", \"amount\":\"%s\"", receiverAddress, amount));
        if (null != message) {
            sb.append(String.format(", \"message\":\"%s\"", message));
        }
        if (time > INVALID_TIME) {
            sb.append(String.format(", \"time\":\"%d\"", time));
        }
        sb.append("}'");

        sb.append(')');
        return sb.toString();
    }

    @Override
    public String toStringLogger() {
        return String.format("sendOne %s->%s: %s", senderData.getAddress(), receiverAddress, amount);
    }
}
