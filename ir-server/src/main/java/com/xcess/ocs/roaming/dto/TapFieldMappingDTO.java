package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.TapDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "DTO for TAP field mapping — used for both request and response")
public class TapFieldMappingDTO {

    @Schema(description = "Unique identifier (populated in response only)", example = "1")
    private Long id;

    @Schema(description = "Call type this field applies to. Null means globally applicable.",
            example = "MO_VOICE", allowableValues = {"GPRS", "MO_VOICE", "MT_VOICE", "MO_SMS", "MT_SMS"})
    private CallType callType;

    @NotBlank
    @Schema(description = "Human-readable logical name", example = "dialledDigits",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @NotBlank
    @Schema(description = "Dot-notation ASN.1 path inside the GSMA TAP structure",
            example = "basicCallInformation.destination.dialledDigits",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String asnPath;

    @NotNull
    @Schema(description = "Data type used for encoding/decoding the field value",
            example = "BCD_STRING", allowableValues = {"BCD_STRING", "ASCII_STRING", "INTEGER", "DECIMAL", "DATE_TIME"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private TapDataType dataType;

    @Schema(description = "Property name on RatedCdr to read from during TAP OUT generation", example = "calledNumber")
    private String outSourceColumn;

    @Schema(description = "Property name on TapCdrDTO to write to during TAP IN decoding", example = "calledNumber")
    private String inTargetColumn;

    @Schema(description = "Global fallback value used when the source column resolves to null", example = "00000000000000")
    private String defaultValue;

    @NotNull
    @Schema(description = "If true, file generation or parsing fails when this field cannot be resolved", example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isMandatory;
}
