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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommand {

    UserData senderData;

    /**
     * @return data of user, who will execute command
     */
    public UserData getSenderData() {
        return senderData;
    }

    /**
     * @return command name, used as value of "run" field
     */
    protected abstract String getName();

    /**
     * @return list of parameters (fields and its' values)
     */
    protected List<String> getParameters() {
        return new ArrayList<>();
    }

    /**
     * @return command for ADS client
     */
    public String toStringCommand() {
        StringBuilder sb = new StringBuilder();

        sb.append("echo '{\"run\":\"").append(getName()).append("\"");

        List<String> parameters = getParameters();
        if (parameters != null) {
            for (String parameter : parameters) {
                sb.append(',');
                sb.append(parameter);
            }
        }

        sb.append("}'");

        return sb.toString();
    }

    /**
     * @return parameters description
     */
    public abstract String toStringLogger();
}
