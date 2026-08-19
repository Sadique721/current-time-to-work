package com.xcess.ocs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xcess.ocs.roaming.entity.CallType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "8. Rate Package Association", description = "Association between a Rate Package and a Rate Package Group")
public class RatePackageAssociationDTO {
    @Schema(description = "Rate package ID", example = "1")
    @NotNull(message = "Rate package ID is required")
    private Long ratePackage;

    @Schema(description = "Name of the Rate Package", example = "Premium Voice & SMS", accessMode = Schema.AccessMode.READ_ONLY)
    private String packageName;

    @Schema(description = "Start time for a Rate Package",type="string", example = "2023-01-01 06:30:30")
    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime startTime;

    @Schema(description = "End time for a Rate Package",type="string", example = "2023-03-01 06:30:30")
    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private LocalDateTime endTime;

    @Schema(description = "Priority (1 = highest priority, 2 = second, etc.). Required for INTERCONNECT, null for ROAMING.", example = "1")
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority;

    @Schema(description = "ROAMING expression: callType this RatePackage handles within the RPG (MO_VOICE, MT_VOICE, MO_SMS, MT_SMS). Null for INTERCONNECT.", example = "MO_VOICE")
    private CallType callType;

    @Schema(description = "Dynamic expression evaluated via reflection on CDR (e.g. homePlmn=123076)", example = "homePlmn=123076")
    private String expression;
}