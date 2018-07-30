package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SendManyTransaction extends AbstractTransaction {

    private List<String[]> wiresList;

    /**
     * @param senderData sender data
     * @param wiresMap   map of receiver - amount pairs
     */
    public SendManyTransaction(UserData senderData, Map<String, String> wiresMap) {
        this.senderData = senderData;
        // convert map to list
        List<String[]> list = new ArrayList<>();
        if (wiresMap != null) {
            for (Map.Entry<String, String> entry : wiresMap.entrySet()) {
                list.add(new String[]{entry.getKey(), entry.getValue()});
            }
        }
        this.wiresList = list;
    }

    /**
     * @param senderData senderData data
     * @param wiresList  list of receiver - amount pairs
     */
    public SendManyTransaction(UserData senderData, List<String[]> wiresList) {
        this.senderData = senderData;
        this.wiresList = (wiresList != null) ? wiresList : new ArrayList<>();
    }

    @Override
    public String getName() {
        return "send_many";
    }

    @Override
    public List<String> getParameters() {
        List<String> list = super.getParameters();
        list.add(String.format("\"wires\":%s", getWiresString()));
        return list;
    }

    private String getWiresString() {
        StringBuilder wiresSb = new StringBuilder("{");
        if (wiresList.size() > 0) {
            for (String[] element : wiresList) {
                wiresSb.append("\"");
                wiresSb.append(element[0] != null ? element[0] : "");
                wiresSb.append("\":\"");
                wiresSb.append(element[1] != null ? element[1] : "");
                wiresSb.append("\",");
            }
            wiresSb.setLength(wiresSb.length() - 1);
        }
        wiresSb.append("}");
        return wiresSb.toString();
    }

    @Override
    public String toStringLogger() {
        return String.format("sendMany %s->%s", senderData.getAddress(), getWiresString());
    }
}
