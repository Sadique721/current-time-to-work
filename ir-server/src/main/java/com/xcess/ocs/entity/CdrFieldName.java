package com.xcess.ocs.entity;

public enum CdrFieldName {
    CALLING_NUMBER,
    CALLED_NUMBER,
    INCOMING_ACCOUNT_ID,
    OUTGOING_ACCOUNT_ID,
    START_TIME,
    END_TIME,
    HOME_PLMN,
    VISITED_PLMN,
    EVENT_NOS,
    USAGE,
    UPLOAD_USAGE,
    DOWNLOAD_USAGE,
    MEASURMENT_UNIT,
    SERVICE_TYPE,
    CALL_TYPE,
    SUBSCRIBER_IDENTITY,
    ACCESS_POINT_NAME;

    public static boolean isValidField(String fieldName) {
        for (CdrFieldName field : values()) {
            if (field.name().equalsIgnoreCase(fieldName)) return true;
        }
        return false;
    }
}
