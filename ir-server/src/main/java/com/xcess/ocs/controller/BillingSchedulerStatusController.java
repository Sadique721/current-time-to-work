package com.xcess.ocs.controller;

import com.xcess.ocs.dto.BillingSchedulerStatusDTO;
import com.xcess.ocs.service.BillingSchedulerStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing billing scheduler status.
 * Provides endpoints to retrieve and update the current status of the billing scheduler.
 */
@RestController
@RequestMapping("/api/billing-scheduler")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Scheduler Status", description = "Endpoints for managing billing scheduler status")
public class BillingSchedulerStatusController {

    private final BillingSchedulerStatusService billingSchedulerStatusService;
    private final com.xcess.ocs.summaryengine.cron.BillingCycleScheduler billingCycleScheduler;

    /**
     * Retrieves the latest billing scheduler status.
     *
     * @return ResponseEntity containing the latest billing scheduler status DTO
     */
    @Operation(summary = "Get billing scheduler status", description = "Retrieves the latest billing scheduler status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK - Returns the latest status"),
            @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT - No status found",
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<BillingSchedulerStatusDTO> getBillingSchedulerStatus() {
        log.info("REST request to get billing scheduler status");
        BillingSchedulerStatusDTO status = billingSchedulerStatusService.getLatestStatus();
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.noContent().build();
    }

    /**
     * Creates or updates the billing scheduler status.
     *
     * @param dto the billing scheduler status DTO containing the status details
     * @return ResponseEntity containing the saved billing scheduler status DTO
     */
    @Operation(summary = "Create or update billing scheduler status",
            description = "Creates a new billing scheduler status or updates the existing one")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK - Status saved successfully")
    })
    @PostMapping("/status")
    public ResponseEntity<BillingSchedulerStatusDTO> saveBillingSchedulerStatus(
            @RequestBody BillingSchedulerStatusDTO dto) {
        log.info("REST request to save billing scheduler status");
        BillingSchedulerStatusDTO savedStatus = billingSchedulerStatusService.createOrUpdateStatus(dto);
        return ResponseEntity.ok(savedStatus);
    }

    /**
     * Triggers the daily billing cycle scheduler manually.
     *
     * @return ResponseEntity containing a success message
     */
    @Operation(summary = "Trigger billing scheduler", description = "Manually triggers the daily billing cycle scheduler to generate invoices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK - Scheduler triggered successfully")
    })
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerBillingScheduler() {
        log.info("Manual trigger for billing scheduler received");
        // Triggering synchronously similar to the tap-out scheduler
        billingCycleScheduler.generateInvoices();
        return ResponseEntity.ok("Billing scheduler triggered successfully");
    }
}
