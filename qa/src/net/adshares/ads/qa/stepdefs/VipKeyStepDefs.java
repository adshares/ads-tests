package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VipKeyStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserData userData;
    private List<String> expectedVipKeyList;
    private List<String> previousVipKeyList;
    private List<String> vipKeyList;

    @Given("^user, who wants to check vip keys$")
    public void user_who_wants_to_check_keys() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
    }

    @When("^(after delay )?user checks vip keys$")
    public void user_checks_vip_keys(String delay) {
        previousVipKeyList = vipKeyList;
        boolean isDelay = "after delay ".equals(delay);
        if (isDelay) {
            try {
                Thread.sleep(EscConst.BLOCK_PERIOD_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        FunctionCaller fc = FunctionCaller.getInstance();

        String resp;
        JsonObject o;
        JsonArray arr;

        resp = fc.getBlock(userData);
        o = Utils.convertStringToJsonObject(resp);

        JsonObject block = o.getAsJsonObject("block");
        String vipHash = block.get("viphash").getAsString();
        resp = fc.getVipKeys(userData, vipHash);
        o = Utils.convertStringToJsonObject(resp);
        vipKeyList = new ArrayList<>();
        arr = o.getAsJsonArray("vipkeys");
        for (JsonElement je : arr) {
            vipKeyList.add(je.getAsJsonObject().get("public_key").getAsString());
        }

        // special node 0 is on the list but it is shouldn't be counted in total
        expectedVipKeyList = new ArrayList<>();
        arr = block.getAsJsonArray("nodes");
        for (JsonElement je : arr) {
            JsonObject entry = je.getAsJsonObject();
            int status = entry.get("status").getAsInt();
            if (EscUtils.isStatusVip(status)) {
                // vip node
                expectedVipKeyList.add(entry.get("public_key").getAsString());
            }
        }
    }

    @Then("^all vip keys are correct$")
    public void all_vip_keys_are_correct() {
        int vipKeyCount = vipKeyList.size();
        int expectedVipKeyCount = expectedVipKeyList.size();

        String reason;
        reason = new AssertReason.Builder().msg("Incorrect number of vip keys.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(FunctionCaller.getInstance().getLastResponse())
                .build();
        assertThat(reason, vipKeyCount, equalTo(expectedVipKeyCount));

        reason = new AssertReason.Builder().msg("Unexpected vip keys.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(FunctionCaller.getInstance().getLastResponse())
                .build();
        assertThat(reason, vipKeyList, containsInAnyOrder(expectedVipKeyList.toArray(new String[expectedVipKeyCount])));
    }

    @Then("^vip keys are changed$")
    public void vip_keys_are_changed() {
        log.trace("actual keys:");
        for (String s : vipKeyList) {
            log.trace(s);
        }
        log.trace("previous keys:");
        for (String s : previousVipKeyList) {
            log.trace(s);
        }
        String reason;
        // check, if key changed
        reason = new AssertReason.Builder().msg("Vip keys was not changed.")
                .req(FunctionCaller.getInstance().getLastRequest()).res(FunctionCaller.getInstance().getLastResponse())
                .build();
        assertThat(reason, vipKeyList, not(containsInAnyOrder(previousVipKeyList.toArray(new String[0]))));
    }
}
