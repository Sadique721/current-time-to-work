package com.xcess.ocs.tapmodels.tap.internal;

public class BerUtils {
    /**
     * Converts a packed BCD-encoded byte array into a human-readable decimal string.
     * <p>
     * Each byte contains two decimal digits: the high nibble (4 bits) and the low nibble.
     * This method decodes each nibble into its corresponding digit and stops decoding
     * when a nibble exceeds 9, which typically indicates padding (e.g., 0xF).
     * <p>
     * Useful for decoding numeric fields from binary protocols or TLV structures
     * where BCD encoding is used and padded with 0xF.
     *
     * @param value the byte array containing packed BCD digits, possibly with padding
     * @return the decoded numeric string, excluding any padding
     *
     */
    public static String bcdToStringWithPadding(byte[] value) {
        StringBuilder result = new StringBuilder();
        for (byte b : value) {
            int high = (b & 0xF0) >>> 4;
            int low = b & 0x0F;

            if (high <= 9) result.append(high);
            else break; // padding or invalid digit

            if (low <= 9) result.append(low);
            else break; // padding or invalid digit
        }
        return result.toString();
    }
}