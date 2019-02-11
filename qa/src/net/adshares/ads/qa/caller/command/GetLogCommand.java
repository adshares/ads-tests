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

package net.adshares.ads.qa.caller.command;

import net.adshares.ads.qa.data.UserData;

import java.util.List;

public class GetLogCommand extends AbstractCommand {

    private long from = 0L;
    private boolean full;
    private String type;

    public GetLogCommand(UserData senderData) {
        this.senderData = senderData;
    }

    /**
     * @return list of parameters (fields and its' values)
     */
    protected List<String> getParameters() {
        List<String> parameters = super.getParameters();
        parameters.add(String.format("\"from\":\"%d\"", from));
        if (full) {
            parameters.add("\"full\":1");
        }
        if (type != null) {
            parameters.add(String.format("\"type\":\"%s\"", type));
        }

        return parameters;
    }

    @Override
    protected String getName() {
        return "get_log";
    }

    @Override
    public String toStringLogger() {
        return "get_log";
    }

    public void setFrom(long timestamp) {
        this.from = timestamp;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public void setType(String type) {
        this.type = type;
    }
}
