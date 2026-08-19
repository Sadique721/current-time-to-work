package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Schema(
        name = "Invoice SearchDTO",
        description = "Schema to hold Invoice search details"
)
public class InvoiceSearchDTO {
    @Schema(
            description = "Search term for global search (searches agreement code)",
            example = "AGR-001",
            nullable = true
    )
    private String searchTerm;

    @Schema(
            description = "Invoice status to filter",
            example = "GENERATED",
            allowableValues = {"GENERATED", "PENDING", "FAILED"},
            nullable = true
    )
    private String status;

    @Schema(
            description = "Filter by billing cycle start date (from)",
            example = "2024-01-01",
            nullable = true
    )
    private LocalDate billingCycleStartFrom;

    @Schema(
            description = "Filter by billing cycle start date (to)",
            example = "2024-12-31",
            nullable = true
    )
    private LocalDate billingCycleStartTo;

    @Schema(
            description = "Filter by agreement ID",
            example = "1",
            nullable = true
    )
    private Long agreementId;

    @Schema(
            description = "Filter by tax type",
            example = "GST",
            nullable = true
    )
    private String taxType;

}

