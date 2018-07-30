package net.adshares.ads.qa.stepdefs;

import cucumber.api.java.Before;
import net.adshares.ads.qa.caller.FunctionCaller;

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
