package com.xcess.ocs.entity;

/**
 * Enum representing the types of partners in the system.
 * Each partner must have exactly one type.
 */
public enum PartnerType {
    /** Partner who provides services/goods to the organization */
    VENDOR,

    /** Partner who receives services/goods from the organization */
    CUSTOMER,

    /** Partner who acts as a carrier/transporter */
    CARRIER,

    /** Partner who acts as both vendor and carrier */
    BOTH;

    public static PartnerType fromString(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        for (PartnerType pt : values()) {
            if (pt.name().equalsIgnoreCase(type.trim())) {
                return pt;
            }
        }
        return null;
    }
}
