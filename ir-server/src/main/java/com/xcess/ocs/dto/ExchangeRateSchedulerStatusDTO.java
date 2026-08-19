package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ExchangeRateSchedulerStatus;
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
        name = "Exchange Rate Scheduler Status",
        description = "Schema to hold details of exchange rate scheduler status"
)
public class ExchangeRateSchedulerStatusDTO {

    @Schema(description = "ID of the scheduler status", example = "1")
    private Long id;

    @Schema(description = "Current status", example = "SUCCESS",
            allowableValues = {"PENDING", "RUNNING", "SUCCESS", "FAILED"})
    private ExchangeRateSchedulerStatus.Status status;

    @Schema(description = "Execution start time")
    private LocalDateTime startTime;

    @Schema(description = "Execution end time")
    private LocalDateTime endTime;

    @Schema(description = "Number of rates saved", example = "31")
    private Integer recordsProcessed;

    @Schema(description = "Error details if failed")
    private String errorMessage;

    @Schema(description = "Number of API attempts", example = "1")
    private Integer retryCount;

    @Schema(description = "API source used", example = "FRANKFURTER")
    private String apiSourceUsed;

    @Schema(description = "Created at timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Modified at timestamp")
    private LocalDateTime modifiedAt;
}
