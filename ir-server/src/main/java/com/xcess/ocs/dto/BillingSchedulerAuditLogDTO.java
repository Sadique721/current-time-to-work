package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for billing scheduler audit log.
 * Used for API request/response handling.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Billing Scheduler Audit Log",
        description = "Schema to hold details of a billing scheduler audit log"
)
public class BillingSchedulerAuditLogDTO{

    @Schema(description = "ID of the audit log", example = "1")
    private Long auditId;

    @Schema(description = "Scheduler status ID", example = "1")
    private Long schedulerStatusId;

    @Schema(description = "Event type", example = "BILLING_SCHEDULER_STARTED",
            allowableValues = {"BILLING_SCHEDULER_STARTED", "BILLING_SCHEDULER_COMPLETED",
                    "BILLING_SCHEDULER_FAILED", "BILLING_SCHEDULER_INTERRUPTED"})
    private String eventType;

    @Schema(description = "Event message describing the event")
    private String eventMessage;

    @Schema(description = "Number of agreements processed", example = "100")
    private Long noOfAgreementsProcessed;

    @Schema(description = "Number of failed invoices", example = "5")
    private Long noOfFailedInvoices;

    @Schema(description = "Execution time in milliseconds", example = "5000")
    private Long executionTimeMs;

    @Schema(description = "Error details or stack trace")
    private String errorDetails;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;
}
