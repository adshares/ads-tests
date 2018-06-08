package net.adshares.ads.qa.stepdefs;

import cucumber.api.java.Before;
import net.adshares.ads.qa.util.FunctionCaller;
import net.adshares.ads.qa.util.Utils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Cucumber hooks.
 */
public class Hooks {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static int testCount = 0;

    @Before
    public void beforeTest() {
        if (testCount == 0) {
            // this code will run only once before all tests

            // deletes esc cache
            try {
                Utils.deleteDirectory("log");
                Utils.deleteDirectory("out");
            } catch (IOException e) {
                log.warn("Unable to delete ESC cache");
            }


            FunctionCaller fc = FunctionCaller.getInstance();
            // deletes esc cache in docker
            fc.callFunction("docker exec -i adshares_ads_1 rm -rf /tmp/esc");
            fc.callFunction("docker exec -i adshares_ads_1 mkdir /tmp/esc");
            // waits for esc compilation
            String resp;
            do {
                resp = fc.callFunction("docker exec -i adshares_ads_1 /docker/wait-up.php");
                Assert.assertFalse("Timeout during docker start", resp.contains("timeout"));
                Assert.assertNotEquals("No response from docker", "", resp);
            } while(!resp.contains("started"));
        }
        testCount++;
    }
}