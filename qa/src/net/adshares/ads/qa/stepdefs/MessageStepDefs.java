package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;

/**
 * Cucumber steps definitions for message tests.
 */
public class MessageStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Allowed message type set
     */
    private final String[] ALLOWED_MSG_TYPE_SET = new String[]{
            "account_created",
            "broadcast",
            "change_account_key",
            "change_node_key",
            "connection",
            "create_account",
            "create_node",
            "empty",
            "log_account",
            "retrieve_funds",
            "send_many",
            "send_one",
            "set_account_status",
            "set_node_status",
            "unset_account_status",
            "unset_node_status"
    };
    private UserData userData;

    @Given("^user, who will check message$")
    public void user_who_will_check_message() {
        List<UserData> userDataList = UserDataProvider.getInstance().getUserDataList();

        userData = userDataList.get(new Random().nextInt(userDataList.size()));
        log.debug("Selected user {}", userData.getAddress());
    }

    @When("^user checks all messages from last (\\d+) block\\(s\\)$")
    public void user_checks_messages(int blockNum) {
        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        JsonObject o;

        resp = fc.getMe(userData);
        o = Utils.convertStringToJsonObject(resp);
        String blockTime = Integer.toHexString(o.get("previous_block_time").getAsInt()).toUpperCase();

        for (int i = blockNum; i > 0; i--) {
            resp = fc.getMessageList(userData, blockTime);
            o = Utils.convertStringToJsonObject(resp);

            if (o.has("error")) {
                String errorDesc = o.get("error").getAsString();

                assertThat("Unexpected error for get_message_list request.", errorDesc,
                        equalTo(EscConst.Error.NO_MESSAGE_LIST_FILE));
            } else {
                JsonElement messages = o.get("messages");
                // If message list is empty, "message "field is missing in previous version it's value was empty String.
                // Need to check type of "messages" field
                if (messages != null && messages.isJsonArray()) {
                    JsonArray arr = (JsonArray) messages;
                    for (JsonElement el : arr) {
                        String messageId = el.getAsString();
                        String messageResp = fc.getMessage(userData, messageId);
                        JsonObject messageObj = Utils.convertStringToJsonObject(messageResp);
                        if (messageObj.has("error")) {
                            String reason = new AssertReason.Builder()
                                    .msg("Unexpected error for get_message: " + messageObj.get("error").getAsString())
                                    .req(fc.getLastRequest()).res(fc.getLastResponse()).build();
                            Assert.fail(reason);
                        } else {
                            JsonArray txArr = messageObj.getAsJsonArray("transactions");
                            for (JsonElement txEl : txArr) {
                                JsonObject txObj = txEl.getAsJsonObject();
                                String type = txObj.get("type").getAsString();

                                assertThat("Unknown type: " + type, ALLOWED_MSG_TYPE_SET, hasItemInArray(type));
                                String txId = txObj.get("id").getAsString();
                                TransactionIdChecker.getInstance().addTransactionId(txId);
                            }
                        }
                    }
                }
            }

            blockTime = EscUtils.getPreviousBlock(blockTime);
        }
    }

    @Then("^he is able to get information about every transaction from message$")
    public void user_able_to_get_message_information() {
        TransactionIdChecker.getInstance().checkAll();
    }
}