package net.adshares.ads.qa.stepdefs;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.command.SendOneTransaction;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.AssertReason;
import net.adshares.ads.qa.util.EscUtils;
import net.adshares.ads.qa.caller.FunctionCaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;

/**
 * Cucumber steps definitions for transaction to non-existent user/node tests
 */
public class NonExistentUserNodeStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private TransferUser sender;
    private String lastResp;

    @Given("^1 user$")
    public void one_user() {
        UserData u = UserDataProvider.getInstance().getUserDataList(1).get(0);

        FunctionCaller fc = FunctionCaller.getInstance();
        BigDecimal balance = fc.getUserAccountBalance(u);

        sender = new TransferUser();
        sender.setUserData(u);
        sender.setStartBalance(balance);
    }

    @When("^sends 0.00000000001 ADS to user in non-existent node$")
    public void sender_sends_to_non_existent_node() {
        sendOneWrapper(getUserAddressInNonExistentNode());
    }

    @When("^sends 0.00000000001 ADS to non-existent user in node$")
    public void sender_sends_to_non_existent_user() {
        sendOneWrapper(getNonExistentUserAddressInNode());
    }

    @When("^retrieves from user in non-existent node$")
    public void retrieves_from_non_existent_node() {
        retrieveFundsWrapper(getUserAddressInNonExistentNode());
    }

    @When("^retrieves from non-existent user in node$")
    public void retrieves_from_non_existent_user() {
        retrieveFundsWrapper(getNonExistentUserAddressInNode());
    }

    @Then("^transfer to invalid address is rejected$")
    public void transfer_is_rejected() {
        final FunctionCaller fc = FunctionCaller.getInstance();
        log.debug("Error: \"{}\"", EscUtils.getErrorDescription(lastResp));

        String reason;
        // check balance
        reason = new AssertReason.Builder().msg("Sender balance changed.")
                .req(fc.getLastRequest()).res(lastResp).build();
        assertThat(reason, fc.getUserAccountBalance(sender), comparesEqualTo(sender.getExpBalance()));

        // check, if transaction was accepted
        reason = new AssertReason.Builder().msg("Transfer to invalid address was accepted.")
                .req(fc.getLastRequest()).res(lastResp).build();
        assertThat(reason, !EscUtils.isTransactionAcceptedByNode(lastResp));
        log.debug("Transaction is rejected.");
    }

    private void sendOneWrapper(String address) {
        FunctionCaller fc = FunctionCaller.getInstance();
        sender.setExpBalance(sender.getStartBalance());
        lastResp = fc.sendOne(new SendOneTransaction(sender.getUserData(), address, "0.00000000001"));
    }

    private void retrieveFundsWrapper(String address) {
        FunctionCaller fc = FunctionCaller.getInstance();
        sender.setExpBalance(sender.getStartBalance());
        lastResp = fc.retrieveFunds(sender.getUserData(), address);
    }

    private String getNonExistentUserAddressInNode() {
        return sender.getUserData().getNodeId() + "-FFFFFFFF-XXXX";
    }

    private String getUserAddressInNonExistentNode() {
        return "FFFF-00000000-XXXX";
    }

}