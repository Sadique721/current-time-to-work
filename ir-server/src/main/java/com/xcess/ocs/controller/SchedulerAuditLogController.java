package com.xcess.ocs.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.SchedulerAuditLogDTO;
import com.xcess.ocs.dto.search.SchedulerAuditLogSearchDTO;
import com.xcess.ocs.service.SchedulerAuditLogService;
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

@RestController
@RequestMapping("/api/scheduler-audit-logs")
@Tag(name = "Scheduler Audit Logs", description = "Endpoints for managing scheduler audit logs")
@RequiredArgsConstructor
@Slf4j
public class SchedulerAuditLogController {

    private final SchedulerAuditLogService auditLogService;

    @Operation(summary = "Get paginated and filtered list of scheduler audit logs",
            description = "Returns a paginated list of scheduler audit logs with search functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<SchedulerAuditLogDTO>> getAuditLogsInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with scheduler audit log search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Search Example",
                                    summary = "Search by config ID and event type",
                                    value = """
                                            {
                                                "page": 1,
                                                "pageSize": 10,
                                                "searchCriteria": {
                                                    "configId": 1,
                                                    "eventType": "STARTED"
                                                }
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<SchedulerAuditLogSearchDTO> pageRequestDTO) {
        log.info("REST request to get scheduler audit logs in page with search criteria");

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<SchedulerAuditLogDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = auditLogService.searchAuditLogs(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered scheduler audit logs in a page");
        } else {
            response = auditLogService.getAuditLogsInPage(pageable);
            log.info("Retrieved all scheduler audit logs in a page");
        }

        return ResponseEntity.ok(response);
    }
}
