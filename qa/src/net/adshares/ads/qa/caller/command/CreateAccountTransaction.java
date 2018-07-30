package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

public class CreateAccountTransaction extends AbstractTransaction {

    private String publicKey;
    private String signature;
    private int node;

    /**
     * @param senderData sender data
     */
    public CreateAccountTransaction(UserData senderData) {
        this.senderData = senderData;
    }

    /**
     * @param publicKey new public key. Null is allowed. If key is null, key will be the same as creator key.
     * @param signature empty String signed with new private key
     */
    public void setPublicKey(String publicKey, String signature) {
        this.publicKey = publicKey;
        this.signature = signature;
    }

    /**
     * @param node node in which account should be created (decimal). If node is lesser than 1, local account
     *             will be created
     */
    public void setNode(int node) {
        this.node = node;
    }

    @Override
    public String toStringCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append("echo '{\"run\":\"get_me\"}';");

        sb.append("echo '{\"run\":\"create_account\"");

        // append node
        if (node >= 1) {
            sb.append(String.format(", \"node\":\"%d\"", node));
        }

        // append key and signature
        if (publicKey != null && signature != null) {
            sb.append(String.format(", \"public_key\":\"%s\", \"confirm\":\"%s\"", publicKey, signature));
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
        return "create_account";
    }
}
