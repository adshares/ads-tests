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

package net.adshares.ads.qa.data;

import java.util.List;

/**
 * genesis.json schema
 */
public class Genesis {

    private List<Node> nodes;

    public List<Node> getNodes() {
        return nodes;
    }

    public class Node {

        private String public_key;
        private String _secret;
        private String _sign;
        private List<Account> accounts;

        public String getPublicKey() {
            return public_key;
        }

        public String getSecret() {
            return _secret;
        }

        public String getSign() {
            return _sign;
        }

        public List<Account> getAccounts() {
            return accounts;
        }

        public class Account {
            private String _address;
            private String balance;
            private String public_key;
            private String _secret;
            private String _sign;

            public String getAddress() {
                return _address;
            }

            public String getBalance() {
                return balance;
            }

            public String getPublicKey() {
                return public_key;
            }

            public String getSecret() {
                return _secret;
            }

            public String getSign() {
                return _sign;
            }


        }
    }
}
