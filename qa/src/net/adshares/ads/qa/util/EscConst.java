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

import java.math.BigDecimal;

/**
 * ESC constants from default.hpp
 */
public class EscConst {
    /**
     * BLOCKDIV number of blocks for dividend update
     */
    public static final int BLOCK_DIVIDEND = 4;
    /**
     * BLOCKSEC block period in seconds
     */
    public static final int BLOCK_PERIOD = 32;
    /**
     * block period in milliseconds
     */
    public static final long BLOCK_PERIOD_MS = 1000L * BLOCK_PERIOD;
    /**
     * Maximum message size in bytes that can be broadcast
     */
    public static final int BROADCAST_MESSAGE_MAX_SIZE = 32000;
    /**
     * TXS_KEY_FEE change account key fee
     */
    public static final BigDecimal CHANGE_ACCOUNT_KEY_FEE = new BigDecimal("0.00010000000");
    /**
     * TXS_BKY_FEE change bank key fee
     */
    public static final BigDecimal CHANGE_BANK_KEY_FEE = new BigDecimal("0.10000000000");
    /**
     * TXS_USR_FEE create user fee (local)
     */
    public static final BigDecimal CREATE_ACCOUNT_LOCAL_FEE = new BigDecimal("0.00100000000");
    /**
     * TXS_RUS_FEE create user fee (additional fee for remote)
     */
    public static final BigDecimal CREATE_ACCOUNT_REMOTE_FEE = new BigDecimal("0.00100000000");
    /**
     * TXS_BNK_FEE create new bank fee
     */
    public static final BigDecimal CREATE_BANK_FEE = new BigDecimal("1000.00000000000");
    /**
     * TXS_MIN_FEE minimum transfer fee
     */
    public static final BigDecimal MIN_TX_FEE = new BigDecimal("0.00000010000");
    /**
     * TXS_PUT_FEE local transfer coefficient
     */
    public static final BigDecimal LOCAL_TX_FEE_COEFFICIENT = new BigDecimal("0.0005");
    /**
     * TXS_LNG_FEE remote transfer coefficient
     */
    public static final BigDecimal REMOTE_TX_FEE_COEFFICIENT = new BigDecimal("0.0005");
    /**
     * TXS_MPT_FEE local multiple transfer coefficient
     */
    public static final BigDecimal MULTI_TX_FEE_COEFFICIENT = new BigDecimal("0.0005");
    /**
     * ~TXS_MINMPT_FEE minimum fee for send_many is bigger for every recipient above 10
     */
    public static final BigDecimal MIN_MULTI_TX_PER_RECIPIENT = new BigDecimal("0.00000001000");
    /**
     * TXS_GET_FEE retrieve funds from remote/dead bank request fee
     */
    public static final BigDecimal RETRIEVE_REQUEST_FEE = new BigDecimal("0.00001000000");
    /**
     * TXS_GOK_FEE(x) retrieve funds from remote/dead bank fee, x is retrieved amount
     */
    public static final BigDecimal RETRIEVE_FEE = new BigDecimal("0.001");
    /**
     * ~TXS_BRO_FEE(x) broadcast message fee per every byte above 32 bytes of message size
     */
    public static final BigDecimal BROADCAST_FEE_PER_BYTE = new BigDecimal("0.00000001000");
    /**
     * USER_MIN_MASS minimum user balance after outgoing transfer
     */
    public static final BigDecimal USER_MIN_MASS = new BigDecimal("0.00020000000");
    /**
     * BANK_MIN_UMASS minimum bank balance after outgoing transfer
     */
    public static final BigDecimal BANK_MIN_UMASS = new BigDecimal("1.00000000000");
    /**
     * TXS_SUS_FEE set bank status bits fee
     */
    public static final BigDecimal SET_BANK_STATUS_FEE = new BigDecimal("0.00010000000");
    /**
     * TXS_SUS_FEE set user status bits fee
     */
    public static final BigDecimal SET_USER_STATUS_FEE = new BigDecimal("0.00010000000");
    /**
     * TXS_UUS_FEE unset bank status bits fee
     */
    public static final BigDecimal UNSET_BANK_STATUS_FEE = new BigDecimal("0.00010000000");
    /**
     * TXS_UUS_FEE unset user status bits fee
     */
    public static final BigDecimal UNSET_USER_STATUS_FEE = new BigDecimal("0.00010000000");
    /**
     * TXS_SAV_FEE log account fee
     */
    public static final BigDecimal LOG_ACCOUNT_FEE = new BigDecimal("0.00010000000");
//    /**
//     * Part of fee that will be add to dividend account
//     */
//    public static final BigDecimal FEE_SHARE_DIVIDEND = new BigDecimal("0.8");
//    /**
//     * Part of fee that will be add to node account
//     */
//    public static final BigDecimal FEE_SHARE_NODE = BigDecimal.ONE.subtract(FEE_SHARE_DIVIDEND);
    /**
     * Part of node fee that will be add to top and vip nodes accounts
     */
    public static final BigDecimal FEE_SHARE_NODE_TOP_VIP = new BigDecimal("0.4");
//    /**
//     * Part of node fee that will be add to own account
//     */
//    public static final BigDecimal FEE_SHARE_NODE_OWN = BigDecimal.ONE.subtract(FEE_SHARE_NODE_TOP_VIP);
    /**
     * VIP_MAX maximum number of vip nodes
     */
    public static final int VIP_MAX = 7;
    /**
     * TOP_MAX maximum number of top nodes
     */
    public static final int TOP_MAX = 10;

    /**
     * Error messages
     */
    public class Error {
        public static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be positive";
        //        public static final String BAD_LENGTH = "Bad length";
        public static final String BROADCAST_NOT_READY = "Broadcast not ready, try again later";
        // Error below is inactive because field 'broadcast_count' was introduced to show no broadcast message.
        //        public static final String BROADCAST_NO_FILE_TO_SEND = "No broadcast file to send";
        public static final String CREATE_ACCOUNT_BAD_TIMING = "Bad timing for remote account request, try again later.";
        public static final String FAILED_TO_LOAD_HASH = "Failed to load hash for block. Try perform get_blocks command to resolve.";
        public static final String FAILED_TO_PROVIDE_TX_INFO = "Failed to provide transaction info. Try again later.";
        public static final String GET_GLOBAL_USER_FAILED = "Failed to get global user info";
        public static final String GET_BLOCK_INFO_FAILED = "Block info is unavailable";
        public static final String MATCH_SECRET_KEY_NOT_FOUND = "Matching secret key not found";
        public static final String NO_MESSAGE_LIST_FILE = "No message list file";
        // Error below is inactive, because field 'updated_blocks' was introduced to show no new blocks.
        //        public static final String NO_NEW_BLOCKS = "No new blocks to download";
        public static final String CHANGE_NODE_STATUS_FAILED = "Not authorized to change node status";
        public static final String CHANGE_STATUS_FAILED = "Not authorized to change bits";
        public static final String CHANGE_STATUS_REMOTE_FAILED = "Changing account status on remote node not allowed";
        public static final String COMMAND_PARSE_ERROR = "Parse error, check input data";
        public static final String DUPLICATED_TARGET = "Duplicated target";
        public static final String GET_SIGNATURE_UNAVAILABLE = "Signature is unavailable";
        public static final String TOO_LOW_BALANCE = "Too low balance";
    }

    /**
     * Result messages
     */
    public class Result {
        public static final String NODE_KEY_CHANGED = "Node key changed";
    }
}
