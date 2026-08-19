package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.TapDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "TapFieldMappingSearchDTO",
        description = "Search/filter criteria for the TAP field mapping master dictionary. All fields are optional."
)
public class TapFieldMappingSearchDTO {

    @Schema(description = "Filter by call type scope. Null returns all call types including global fields.",
            example = "MO_VOICE", nullable = true,
            allowableValues = {"GPRS", "MO_VOICE", "MT_VOICE", "MO_SMS", "MT_SMS"})
    private CallType callType;

    @Schema(description = "Partial match on fieldName (case-insensitive).", example = "imei", nullable = true)
    private String fieldName;

    @Schema(description = "Partial match on asnPath (case-insensitive).",
            example = "basicCallInformation", nullable = true)
    private String asnPath;

    @Schema(description = "Filter by data type.", example = "BCD_STRING", nullable = true,
            allowableValues = {"BCD_STRING", "ASCII_STRING", "INTEGER", "DECIMAL", "DATE_TIME"})
    private TapDataType dataType;

    @Schema(description = "Filter by mandatory flag. Null returns both mandatory and optional fields.",
            example = "true", nullable = true)
    private Boolean isMandatory;
}
