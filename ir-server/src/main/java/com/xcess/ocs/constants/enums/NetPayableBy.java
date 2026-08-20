package com.xcess.ocs.constants.enums;

public enum NetPayableBy {
    CUSTOMER, HOST;

    public String label() {
        return this.name();
    }

    public static NetPayableBy fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CUSTOMER;
        }
        try {
            return NetPayableBy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOMER; // Default fallback for safety
        }
    }
}
