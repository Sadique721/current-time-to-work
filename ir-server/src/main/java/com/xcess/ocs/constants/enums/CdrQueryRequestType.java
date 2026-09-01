package com.xcess.ocs.constants.enums;

/**
 * Type-safe Enum representing CDR query request types (ErrorRequest vs ReRateRequest).
 */
public enum CdrQueryRequestType {
    ERROR_REQUEST("ErrorRequest"),
    RERATE_REQUEST("ReRateRequest");

    private final String label;

    CdrQueryRequestType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static CdrQueryRequestType fromString(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        for (CdrQueryRequestType req : values()) {
            if (req.label.equalsIgnoreCase(type.trim()) || req.name().equalsIgnoreCase(type.trim())) {
                return req;
            }
        }
        return null;
    }
}
