package com.xcess.ocs.entity;

/**
 * Enum representing different lines of business for CDR processing.
 * Determines the rating strategy and field usage for CDR processing.
 */
public enum LineOfBusiness {
    /**
     * Traditional carrier-to-carrier interconnect calls.
     * Uses account-based rate package lookup and calling/called number for rate lookup.
     */
    INTERCONNECT,
    
    /**
     * Mobile roaming calls between network operators.
     * Uses account-based rate package lookup and home PLMN for rate lookup.
     */
    ROAMING
}