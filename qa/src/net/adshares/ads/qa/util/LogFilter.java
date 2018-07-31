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

package net.adshares.ads.qa.util;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Used for selecting/excluding events from log (get_log response)
 */
public class LogFilter {
    /**
     * filter type: true - require, false - exclude
     */
    private boolean isRequired;
    /**
     * filter criterions:<br />
     * - key is Json object field name, <br />
     * - value is value of that field.
     */
    private Map<String, String> filterMap;

    /**
     * @param isRequired filter type: true - require, false - exclude
     */
    public LogFilter(boolean isRequired) {
        this.isRequired = isRequired;
        this.filterMap = new HashMap<>();
    }

    /**
     * Adds filter criterion.
     *
     * @param key   Json object field name
     * @param value value of Json field
     */
    public void addFilter(String key, String value) {
        filterMap.put(key, value);
    }

    /**
     * Matches log entry with criterions.
     *
     * @param o log entry
     * @return true if entry should be processed, false if entry should be skipped
     */
    public boolean processEntry(JsonObject o) {
        boolean isMatch = false;

        if (o != null) {
            for (String key : filterMap.keySet()) {
                if (o.has(key)) {
                    isMatch = o.get(key).getAsString().matches(filterMap.get(key));
                } else {
                    isMatch = false;
                }
                if (!isMatch) {
                    break;
                }
            }
        }

        return (isMatch && isRequired) || (!isMatch && !isRequired);
    }

}
