package com.xcess.ocs.dto;

import com.xcess.ocs.entity.BillingSchedulerStatus;
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
        name = "Billing Scheduler Status",
        description = "Schema to hold details of billing scheduler status"
)
public class BillingSchedulerStatusDTO {

    @Schema(description = "ID of the billing scheduler status", example = "1")
    private Long schedulerStatusId;

    @Schema(description = "Current status of the billing scheduler", example = "PENDING",
            allowableValues = {"PENDING", "RUNNING", "SUCCESS", "FAILED"})
    private BillingSchedulerStatus.Status status;

    @Schema(description = "Last billing run time")
    private LocalDateTime lastBillingRunTime;

    @Schema(description = "Next billing start date")
    private LocalDateTime nextBillingStartDate;

}
