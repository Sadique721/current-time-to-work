package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Search criteria DTO for filtering billing scheduler audit logs.
 */
@Data
@NoArgsConstructor
@Schema(
        name = "Billing Scheduler Audit Log SearchDTO",
        description = "Schema to hold search criteria for billing scheduler audit logs"
)
public class BillingSchedulerAuditLogSearchDTO {

    @Schema(description = "Scheduler status ID to filter", example = "1", nullable = true)
    private Long schedulerStatusId;

    @Schema(description = "Event type to filter", example = "BILLING_SCHEDULER_STARTED", nullable = true)
    private String eventType;

    @Schema(description = "Start date for created_at filter", nullable = true)
    private LocalDateTime createdAtFrom;

    @Schema(description = "End date for created_at filter", nullable = true)
    private LocalDateTime createdAtTo;
}
