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