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

/**
 * Extended timestamp for get_log section.
 * Timestamp consist of time in seconds and event number in particular second.
 */
public class LogEventTimestamp {
    private long timestamp;
    private int eventNum;

//    public LogEventTimestamp(long timestamp) {
//        this.timestamp = timestamp;
//        this.eventNum = 0;
//    }

    public LogEventTimestamp(long timestamp, int eventNum) {
        this.timestamp = timestamp;
        this.eventNum = eventNum;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getEventNum() {
        return eventNum;
    }

    /**
     * By incrementing event number, timestamp moves to next event
     */
    public LogEventTimestamp incrementEventNum() {
        ++eventNum;
        return this;
    }

    @Override
    public String toString() {
        return String.format("%d_%d", timestamp, eventNum);
    }
}
