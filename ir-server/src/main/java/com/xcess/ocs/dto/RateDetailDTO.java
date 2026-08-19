package com.xcess.ocs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.xcess.ocs.entity.RateDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "9. Rate Detail", description = "Schema to hold Rate details for a rate package")
public class RateDetailDTO {
        @Schema(description = "Rate details ID", accessMode = Schema.AccessMode.READ_ONLY)
        private Long rateDetailsId;

        @Schema(description = "Destination prefix", example = "919428")
        @Pattern(regexp = "^\\d{1,10}$", message = "Invalid destination prefix format")
        private String destinationPrefix;

        @Schema(description = "Destination prefix name", example = "India-BSNL")
        private String destinationPrefixName;

        @Schema(description = "Source prefix (required for source-destination based)", example = "1934")
        @Pattern(regexp = "^\\d{1,10}$", message = "Invalid source prefix format")
        private String sourcePrefix;

        @Schema(description = "Source prefix name (required for source-destination based)", example = "USA Texas")
        private String sourcePrefixName;

        @Schema(description = "Rate for a Rate Package Detail", example = "20")
        @NotNull(message = "Rate is required")
        @Positive(message = "Rate must be greater than 0")
        private Double rate;

        @Schema(description = "Start time for a Rate Package Detail", type = "string", example = "2023-01-01 06:30:30")
        @NotNull(message = "Start time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @Schema(description = "End time for a Rate Package Detail", type = "string", example = "2023-03-01 06:30:30")
        @NotNull(message = "End time is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        @Schema(description = "Rate Package ID — set automatically on create", accessMode = Schema.AccessMode.READ_ONLY)
        private Long ratePackageId;

        // Include currentVersion in GET response but not in create/update
        @Schema(description = "Current Version of Rate Package Detail", example = "1")
        private Integer currentVersion;

        @Schema(description = "Source country ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
        private Long sourceCountryId;

        @Schema(description = "Source country code", example = "1",accessMode = Schema.AccessMode.READ_ONLY)
        private String sourceCountryCode;

        @Schema(description = "Source country name", example = "United States",accessMode = Schema.AccessMode.READ_ONLY)
        private String sourceCountryName;

        @Schema(description = "Destination country ID", example = "91",accessMode = Schema.AccessMode.READ_ONLY)
        private Long destinationCountryId;

        @Schema(description = "Destination country code", example = "91",accessMode = Schema.AccessMode.READ_ONLY)
        private String destinationCountryCode;

        @Schema(description = "Destination country name", example = "India",accessMode = Schema.AccessMode.READ_ONLY)
        private String destinationCountryName;

        @Schema(description = "ROAMING: zone name this rate applies to (e.g. South Asia, Europe). Null for INTERCONNECT.", example = "South Asia")
        private String zoneName;

        public static RateDetailDTO fromEntity(RateDetails rateDetails) {
                if (rateDetails == null) return null;

                return RateDetailDTO.builder()
                        .rateDetailsId(rateDetails.getRateDetailsId())
                        .destinationPrefix(rateDetails.getDestinationPrefix())
                        .destinationPrefixName(rateDetails.getDestinationPrefixName())
                        .sourcePrefix(rateDetails.getSourcePrefix())
                        .sourcePrefixName(rateDetails.getSourcePrefixName())
                        .rate(rateDetails.getRate())
                        .ratePackageId(rateDetails.getRatePackage().getRatePackageId())
                        .currentVersion(rateDetails.getCurrentVersion())
                        .sourceCountryId(rateDetails.getSourceCountryId() != null ? rateDetails.getSourceCountryId().getCountryId() : null)
                        .sourceCountryCode(rateDetails.getSourceCountryCode())
                        .sourceCountryName(rateDetails.getSourceCountryName())
                        .destinationCountryId(rateDetails.getDestinationCountryId() != null ? rateDetails.getDestinationCountryId().getCountryId() : null)
                        .destinationCountryCode(rateDetails.getDestinationCountryCode())
                        .destinationCountryName(rateDetails.getDestinationCountryName())
                        .zoneName(rateDetails.getZoneName())
                        .startTime(rateDetails.getStartTime())
                        .endTime(rateDetails.getEndTime())
                        .build();
        }


}