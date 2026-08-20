package com.xcess.ocs.constants;

public final class AppConstants {
    private AppConstants() {}

    public static final int MAX_PAGE_SIZE = 10000;
    public static final String DEFAULT_CURRENCY = "Rs.";
    public static final String INVOICE_DESC_INTERCONNECT = "Master Interconnect Settlement Agreement";
    public static final String INVOICE_DESC_ROAMING_TAP_OUT = "Roaming TAP OUT Settlement";
    
    // Tax types
    public static final String TAX_TYPE_MULTI = "MULTI";
    
    // Rating / billing status
    public static final String STATUS_GENERATED = "GENERATED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SUCCESS = "SUCCESS";
}
