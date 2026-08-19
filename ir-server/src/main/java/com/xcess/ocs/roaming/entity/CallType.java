package com.xcess.ocs.roaming.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * GSMA TAP call event type, corresponding to the CHOICE alternatives
 * in {@code CallEventDetail} per TAP-0312.asn.
 */
@Schema(description = "GSMA TAP call event type matching the CallEventDetail CHOICE alternatives in TAP-0312.asn")
public enum CallType {
    GPRS,
    MO_VOICE,
    MT_VOICE,
    MO_SMS,
    MT_SMS
}
