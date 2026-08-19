package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Scheduler Audit Log",
        description = "Schema to hold details of a scheduler audit log"
)
public class SchedulerAuditLogDTO {
    
    @Schema(description = "ID of the audit log", accessMode = Schema.AccessMode.READ_ONLY)
    private Long auditId;

    @Schema(description = "Configuration ID", example = "1")
    private Long configId;

    @Schema(description = "Scheduler status ID", example = "1")
    private Long schedulerStatusId;

    @Schema(description = "Event type", example = "STARTED")
    private String eventType;

    @Schema(description = "Event message")
    private String eventMessage;

    @Schema(description = "CDR start time")
    private LocalDateTime cdrStartTime;

    @Schema(description = "CDR end time")
    private LocalDateTime cdrEndTime;

    @Schema(description = "Records processed", example = "1000")
    private Long recordsProcessed;

    @Schema(description = "Execution time in milliseconds", example = "5000")
    private Long executionTimeMs;

    @Schema(description = "Error details")
    private String errorDetails;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;
}
