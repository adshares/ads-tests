/*
 * Copyright (C) 2018 Adshares sp. z. o.o.
 *
 * This file is part of ADS Tests
 *
 * ADS Tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ADS Tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ADS Tests. If not, see <https://www.gnu.org/licenses/>.
 */

package net.adshares.ads.qa.stepdefs;

import com.google.gson.JsonObject;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import net.adshares.ads.qa.caller.FunctionCaller;
import net.adshares.ads.qa.caller.command.CustomCommand;
import net.adshares.ads.qa.data.UserData;
import net.adshares.ads.qa.data.UserDataProvider;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ErrorStepDefs {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private UserData userData;

    @Given("^user, who wants to test errors$")
    public void user_who_wants_to_change_key() {
        userData = UserDataProvider.getInstance().getUserDataList(1).get(0);
    }

    @When("^user sends (.*)$")
    public void userSends(String command) {
        FunctionCaller fc = FunctionCaller.getInstance();
        CustomCommand customCommand = new CustomCommand(this.userData, command);
        fc.callCustomCommand(customCommand);
    }

    @Then("^error code (\\d+) is returned$")
    public void error_code_is_returned(String errorCode) {
        String response = FunctionCaller.getInstance().getLastResponse();
        JsonObject o = Utils.convertStringToJsonObject(response);

        Assert.assertTrue("Missing 'error_code' field.", o.has("error_code"));
        assertThat("Unexpected error_code", o.get("error_code").getAsString(), equalTo(errorCode));
    }

    @Then("^error \"([^\"]*)\" is returned$")
    public void error_is_returned(String error) {
        String response = FunctionCaller.getInstance().getLastResponse();
        JsonObject o = Utils.convertStringToJsonObject(response);

        Assert.assertTrue("Missing 'error' field.", o.has("error"));
        assertThat("Unexpected error", o.get("error").getAsString(), equalTo(error));
    }
}
