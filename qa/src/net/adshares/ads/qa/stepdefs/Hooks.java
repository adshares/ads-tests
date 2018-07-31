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
