package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

import java.util.List;

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

    @Override
    public String getName() {
        return "create_account";
    }

    @Override
    public List<String> getParameters() {
        List<String> list = super.getParameters();
        // append node
        if (node >= 1) {
            list.add(String.format("\"node\":%d", node));
        }
        // append key and signature
        if (publicKey != null && signature != null) {
            list.add(String.format("\"public_key\":\"%s\", \"confirm\":\"%s\"", publicKey, signature));
        }
        return list;
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
    public String toStringLogger() {
        return "create_account";
    }
}
