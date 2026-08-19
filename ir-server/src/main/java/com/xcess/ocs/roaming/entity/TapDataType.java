package com.xcess.ocs.roaming.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Encoding and decoding strategy applied to a TAP field value.
 *
 * <p>Used by {@link com.xcess.ocs.roaming.config.TapFieldPathResolver} to determine
 * how to format a value during TAP OUT generation and how to decode it during TAP IN extraction.
 */
@Schema(description = "Data type controlling how a TAP field value is encoded (TAP OUT) and decoded (TAP IN)")
public enum TapDataType {

    /** Two digits per octet, high nibble first, 0xF filler for odd-length numbers (e.g. MSISDN, IMSI). */
    BCD_STRING,

    /** Raw UTF-8 byte array (e.g. TADIG codes, network identifiers). */
    ASCII_STRING,

    /** Whole number encoded as {@code BigInteger} (e.g. call duration in seconds). */
    INTEGER,

    /** Scaled integer: actual value × 10^decimalPlaces stored as {@code BigInteger} (e.g. charges). */
    DECIMAL,

    /** CCYYMMDDhhmmss 14-byte timestamp per TAP-0312.asn LocalTimeStamp definition. */
    DATE_TIME
}
