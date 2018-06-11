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

    /**
     * Number of started tests
     */
    private static int testCount = 0;

    @Before
    public void beforeTest() {
        if (testCount == 0) {
            // this code will run only once before all tests

            FunctionCaller fc = FunctionCaller.getInstance();
            fc.deleteCache();
            fc.waitForCompilation();

        }
        testCount++;
    }
}
