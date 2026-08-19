package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for failed invoices.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Failed Invoice",
        description = "Schema to hold details of a failed invoice"
)
public class FailedInvoiceDTO {

    @Schema(description = "ID of the failed invoice record", example = "1")
    private Long id;

    @Schema(description = "Agreement ID", example = "1")
    private Long agreementId;

    @Schema(description = "Billing period start date")
    private LocalDate billingStartDate;

    @Schema(description = "Billing period end date")
    private LocalDate billingEndDate;

    @Schema(description = "Error message describing the failure")
    private String errorMessage;

    @Schema(description = "Date when billing was attempted")
    private LocalDate billingDate;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;
}
