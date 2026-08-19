package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.UnitType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "6. Pulse",
        description = "Schema to hold the Pulse details"
)
public class PulseDTO {
    @Schema(description = "ID of the pulse", accessMode = Schema.AccessMode.READ_ONLY)
    private Long pulseId;

    @Schema(
            description = "Name of the pulse",
            example = "Voice_60Sec"
    )
    @NotNull(message = "Pulse name must not be null.")
    private String pulseName;

    @Schema(
            description = "Type of the pulse",
            example = "VOICE",
            allowableValues = {"VOICE", "SMS", "USAGE"}
    )
    @NotNull(message = "Service type must not be null.")
    private ServiceType serviceType; // VOICE, SMS, USAGE

    @Schema(
            description = "Unit of the pulse",
            example = "SECOND",
            allowableValues = {"SECOND", "MINUTE", "EVENT", "KB", "MB", "GB", "BYTE"}
    )
    @NotNull(message = "Unit must not be null.")
    private UnitType unit; // SECOND, MINUTE, EVENT, KB, MB, GB, BYTE

    @Schema(
            description = "No of units",
            example = "60"
    )
    @NotNull(message = "No of units must not be null.")
    @Positive(message = "No of units must be greater than 0.")
    private int noOfUnits;
}
