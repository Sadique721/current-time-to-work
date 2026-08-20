package com.xcess.ocs.constants.enums;

import java.util.Arrays;

public enum SettlementType {
    NET, INCOMING, OUTGOING, ROAMING_TAP_OUT, ROAMING_TAP_IN;

    public String label() {
        return this.name();
    }

    public static SettlementType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NET;
        }
        try {
            return SettlementType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NET; // Default fallback for safety
        }
    }
}
