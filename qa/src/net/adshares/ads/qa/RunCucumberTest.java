package net.adshares.ads.qa;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"})
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer_local")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@transfer_remote")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@non_existent")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@broadcast")
//@CucumberOptions(plugin = {"pretty", "cucumber.runtime.formatter.Slf4jFormatter"}, tags = "@retrieve_funds")
public class RunCucumberTest {
}