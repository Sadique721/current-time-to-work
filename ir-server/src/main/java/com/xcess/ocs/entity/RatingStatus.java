package com.xcess.ocs.entity;

/**
 * Shared rating status for all CDR types (Voice, SMS, Usage).
 * Each CDR has two independent sides — incoming and outgoing — each carrying this status.
 */
public enum RatingStatus {

    /** Initial state — not yet attempted. */
    PENDING,

    /** Rate found and charge calculated successfully. */
    RATED,

    /** No matching rate found — data or configuration issue. */
    UNRATED,

    /** Hard failure — missing required field or unexpected exception. */
    FAILED
}
