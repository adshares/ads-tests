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
     * TXS_USR_FEE create user fee (local)
     */
    public static final BigDecimal CREATE_ACCOUNT_LOCAL_FEE = new BigDecimal("0.00100000000");
    /**
     * TXS_RUS_FEE create user fee (additional fee for remote)
     */
    public static final BigDecimal CREATE_ACCOUNT_REMOTE_FEE = new BigDecimal("0.00100000000");
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
     * ~TXS_MINMPT_FEE minumum fee for send_many is bigger for every recipient above 10
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
     * Error messages
     */
    public class Error {
        public static final String BROADCAST_NOT_READY = "Broadcast not ready, try again later";
        public static final String BROADCAST_NO_FILE_TO_SEND = "No broadcast file to send";
        public static final String GET_GLOBAL_USER_FAIL = "Failed to get global user info";
    }
}
