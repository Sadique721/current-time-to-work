package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.TapDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Flat DTO populated during TAP IN decoding by {@code TapCdrExtractorService}.
 *
 * <p>Each field corresponds to an {@code in_target_column} value in the
 * {@code tap_field_mappings} master dictionary. The resolver writes decoded
 * values to this object via reflection using the column name as the property name.
 */
@Schema(description = "Flat CDR record populated during TAP IN decoding. " +
                      "Each field maps to an in_target_column entry in the tap_field_mappings dictionary.")
@Getter
@Setter
@NoArgsConstructor
public class TapCdrDTO {

    @Schema(description = "Direction of the TAP file: IN (received from partner) or OUT (sent to partner)",
            example = "IN")
    private TapDirection tapDirection;

    @Schema(description = "GSMA call event type derived from the CallEventDetail CHOICE", example = "MOC")
    private CallType callType;

    @Schema(description = "International Mobile Subscriber Identity (BCD-decoded)", example = "234150999999999")
    private String imsi;

    @Schema(description = "Mobile Station ISDN Number (BCD-decoded)", example = "447700900000")
    private String msisdn;

    @Schema(description = "Calling party number for MTC events (BCD-decoded)", example = "447700900001")
    private String callingNumber;

    @Schema(description = "Called party number for MOC events (BCD-decoded)", example = "447700900002")
    private String calledNumber;

    @Schema(description = "Call or session start timestamp parsed from TAP CCYYMMDDhhmmss format",
            example = "2024-06-15T14:30:00")
    private LocalDateTime callStartTime;

    @Schema(description = "Total call or session duration in seconds", example = "120")
    private Integer callDurationSec;

    @Schema(description = "Visited PLMN identifier (serving network TADIG/MCC-MNC)", example = "DEU01")
    private String visitedPlmn;

    @Schema(description = "Home PLMN identifier", example = "GBR01")
    private String homePlmn;

    @Schema(description = "Service type derived from the call event type", example = "VOICE")
    private ServiceType serviceType;

    @Schema(description = "TAP charge scaled down by tapDecimalPlaces (actual monetary amount)",
            example = "0.2400")
    private BigDecimal tapCharge;

    @Schema(description = "Settlement currency code from AccountingInfo", example = "EUR")
    private String currency;

    @Schema(description = "Total usage in bytes for data sessions", example = "1048576")
    private BigDecimal totalUsage;

    @Schema(description = "Number of events (e.g. for SMS)", example = "1")
    private Integer eventNos;
}
