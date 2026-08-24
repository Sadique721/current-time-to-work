package com.xcess.ocs.constants.enums;

public enum RequestStatus {
    NEW, PROCESSING, IN_PROGRESS, COMPLETED, FAILED, INTERRUPTED;

    public String label() {
        return this.name();
    }

    public static RequestStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NEW;
        }
        try {
            return RequestStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEW; // Safe fallback
        }
    }
}
