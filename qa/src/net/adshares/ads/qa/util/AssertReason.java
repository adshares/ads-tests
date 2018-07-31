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

public class AssertReason {

    interface AssertReasonBuilder {
        String build();

        AssertReasonBuilder req(String request);

        AssertReasonBuilder res(String response);

        AssertReasonBuilder msg(String msg);

    }

    private AssertReason() {
    }

    public static class Builder implements AssertReasonBuilder {

        private String request;
        private String response;
        private String message;

        public Builder() {}

        public String build() {
            StringBuilder sb = new StringBuilder();

            // request
            if (request != null) {
                sb.append(System.lineSeparator());
                sb.append("request:");
                sb.append(request);
            }
            // response
            if (response != null) {
                sb.append(System.lineSeparator());
                sb.append("response:");
                sb.append(response);
            }
            if (request != null || response != null) {
                sb.append(System.lineSeparator());
            }
            // message
            if (message != null) {
                sb.append(message);
            }

            return sb.toString();
        }

        public Builder req(String request) {
            this.request = request;
            return this;
        }

        public Builder res(String response) {
            this.response = response;
            return this;
        }

        public Builder msg(String message) {
            if (this.message == null) {
                this.message = message;
            } else {
                this.message = this.message.concat("\n").concat(message);
            }
            return this;
        }
    }
}