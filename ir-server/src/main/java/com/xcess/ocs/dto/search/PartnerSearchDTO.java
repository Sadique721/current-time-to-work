package com.xcess.ocs.dto.search;

import com.xcess.ocs.entity.InterconnectType;
import com.xcess.ocs.entity.PartnerType;
import com.xcess.ocs.entity.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "18. Partner SearchDTO",
        description = "Schema to hold Partner search details"
)
public class PartnerSearchDTO {
        @Schema(
                description = "Partner name to search",
                example = "Telecom",
                nullable = true
        )
        private String partnerName;

        @Schema(
                description = "Partner code to search",
                example = "TP001",
                nullable = true
        )
        private String partnerCode;

        @Schema(
                description = "Partner type to filter",
                example = "CUSTOMER",
                nullable = true
        )
        private PartnerType partnerType;

        @Schema(
                description = "Status to filter",
                example = "ACTIVE",
                nullable = true
        )
        private Status status;

        @Schema(
                description = "Country to filter",
                example = "USA",
                nullable = true
        )
        private String country;
}