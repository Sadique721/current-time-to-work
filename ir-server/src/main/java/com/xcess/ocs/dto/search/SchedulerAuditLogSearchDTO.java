package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(
        name = "Scheduler Audit Log SearchDTO",
        description = "Schema to hold Scheduler Audit Log search details"
)
public class SchedulerAuditLogSearchDTO {
    
    @Schema(description = "Config ID to filter", example = "1", nullable = true)
    private Long configId;

    @Schema(description = "Event type to filter", example = "STARTED", nullable = true)
    private String eventType;

    @Schema(description = "Start date for created_at filter", nullable = true)
    private LocalDateTime createdAtFrom;

    @Schema(description = "End date for created_at filter", nullable = true)
    private LocalDateTime createdAtTo;

    @Schema(description = "Scheduler status ID to filter", example = "1", nullable = true)
    private Long schedulerStatusId;
}
