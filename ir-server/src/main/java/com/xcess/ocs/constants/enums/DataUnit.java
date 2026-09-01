package com.xcess.ocs.constants.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Type-safe Enum for data volume measurement units and unit conversions.
 * Supports standard telecommunication units (BYTE, KB, MB, GB) and alias representations.
 */
public enum DataUnit {
    BYTE(BigDecimal.valueOf(1L)),
    KB(BigDecimal.valueOf(1024L)),
    MB(BigDecimal.valueOf(1024L * 1024L)),
    GB(BigDecimal.valueOf(1024L * 1024L * 1024L));

    private final BigDecimal multiplier;

    DataUnit(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    /**
     * Resolves a case-insensitive unit string (or alias) to a DataUnit enum.
     * Defaults to BYTE if input is null or unrecognised.
     */
    public static DataUnit fromString(String unit) {
        if (unit == null || unit.isBlank()) {
            return BYTE;
        }
        String clean = unit.trim().toUpperCase();
        return switch (clean) {
            case "KB", "KILOBYTE", "KILOBYTES" -> KB;
            case "MB", "MEGABYTE", "MEGABYTES" -> MB;
            case "GB", "GIGABYTE", "GIGABYTES" -> GB;
            default -> BYTE;
        };
    }

    /**
     * Converts a given volume in a specific unit to total Bytes.
     */
    public static BigDecimal toBytes(BigDecimal volume, String unit) {
        if (volume == null) {
            return BigDecimal.ZERO;
        }
        DataUnit dataUnit = fromString(unit);
        return volume.multiply(dataUnit.multiplier);
    }

    /**
     * Normalizes a data volume from one unit to another with 4 decimal places rounding.
     */
    public static BigDecimal normalize(BigDecimal volume, String fromUnit, String toUnit) {
        if (volume == null) {
            return BigDecimal.ZERO;
        }
        if (fromUnit == null || toUnit == null || fromUnit.equalsIgnoreCase(toUnit)) {
            return volume;
        }
        DataUnit from = fromString(fromUnit);
        DataUnit to = fromString(toUnit);
        BigDecimal inBytes = volume.multiply(from.multiplier);
        if (to == BYTE) {
            return inBytes;
        }
        return inBytes.divide(to.multiplier, 4, RoundingMode.CEILING);
    }
}
