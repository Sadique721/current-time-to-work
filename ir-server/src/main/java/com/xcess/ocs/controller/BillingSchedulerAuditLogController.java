package com.xcess.ocs.controller;

import com.xcess.ocs.dto.BillingSchedulerAuditLogDTO;
import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.BillingSchedulerAuditLogSearchDTO;
import com.xcess.ocs.service.BillingSchedulerAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing billing scheduler audit logs.
 * Provides endpoints to retrieve and search audit logs with pagination support.
 */
@RestController
@RequestMapping("/api/billing-scheduler/audit-logs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Billing Scheduler Audit Logs", description = "Endpoints for managing billing scheduler audit logs")
public class BillingSchedulerAuditLogController {

    private final BillingSchedulerAuditLogService auditLogService;

    /**
     * Retrieves a paginated list of billing scheduler audit logs with optional search criteria.
     *
     * @param pageRequestDTO Pagination and search criteria request
     * @return Paginated response containing audit log DTOs
     */
    @Operation(summary = "Get paginated and filtered list of billing scheduler audit logs",
            description = "Returns a paginated list of billing scheduler audit logs with search functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<BillingSchedulerAuditLogDTO>> getAuditLogsInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with billing scheduler audit log search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Search Example",
                                    summary = "Search by scheduler status ID and event type",
                                    value = """
                                            {
                                                "page": 1,
                                                "pageSize": 10,
                                                "searchCriteria": {
                                                    "schedulerStatusId": 1,
                                                    "eventType": "STARTED"
                                                }
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<BillingSchedulerAuditLogSearchDTO> pageRequestDTO) {
        log.info("REST request to get billing scheduler audit logs in page with search criteria");

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<BillingSchedulerAuditLogDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = auditLogService.searchAuditLogs(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered billing scheduler audit logs in a page");
        } else {
            response = auditLogService.getAuditLogsInPage(pageable);
            log.info("Retrieved all billing scheduler audit logs in a page");
        }

        return ResponseEntity.ok(response);
    }
}
