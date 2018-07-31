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

package net.adshares.ads.qa;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"})
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@account")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@dry-run")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@get_accounts")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@broadcast")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@function")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@log_account")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@message")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@node")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@non_existent")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@retrieve_funds")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@status")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer_local")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer_remote")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@vip_key")
public class RunCucumberTest {
}