package com.xcess.ocs.controller;

import com.xcess.ocs.dto.CdrAnalysisDTO;
import com.xcess.ocs.dto.CdrAnalysisSummaryDTO;
import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.CdrFilterRequest;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.service.CdrAnalysisService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cdr-analysis")
@Tag(name = "CDR Analysis", description = "Endpoints for compact CDR Analysis table listing and detailed single transaction inspection")
@RequiredArgsConstructor
public class CdrAnalysisController {

    private final CdrAnalysisService cdrAnalysisService;

    /**
     * API 1: Compact Paginated Summary Table (5 Fields: id, serviceType, lineOfBusiness, incomingRatingStatus, outgoingRatingStatus)
     * Excludes FAILED rating status and orders by latest modifiedDate DESC.
     */
    @Operation(
            summary = "Get compact paginated summary list of CDRs (5 fields, excludes FAILED status)",
            description = "Returns a compact paginated list returning ONLY 5 fields (id, serviceType, lineOfBusiness, " +
                          "incomingRatingStatus, outgoingRatingStatus). Automatically excludes records with FAILED rating status. " +
                          "Sorted by latest modifiedDate DESC."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<CdrAnalysisSummaryDTO>> getCdrAnalysisInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with CDR analysis filter criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "CdrAnalysisSummaryPageRequest",
                                    summary = "Example CDR Analysis summary pagination request",
                                    value = """
                                    {
                                        "page": 1,
                                        "pageSize": 10,
                                        "searchCriteria": {}
                                    }
                                    """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<CdrFilterRequest> pageRequestDTO) {

        log.info("REST request for summary CDR analysis page: page={}, pageSize={}", 
                pageRequestDTO.getPage(), pageRequestDTO.getPageSize());

        PageResponseDTO<CdrAnalysisSummaryDTO> response = cdrAnalysisService.getPaginatedCdrSummary(pageRequestDTO);
        return ResponseEntity.ok(response);
    }

    /**
     * API 2: Single Record Full Details View (Pass ID to fetch all transaction fields)
     * Called when a user clicks a row in the UI table.
     */
    @Operation(
            summary = "Get full details of a single CDR transaction by ID",
            description = "Called when a user clicks on a row in the CDR Analysis UI summary table. " +
                          "Passes the CDR record ID and optional serviceType to fetch all transaction fields."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/details/{id}")
    public ResponseEntity<CdrAnalysisDTO> getCdrDetails(
            @PathVariable Long id,
            @RequestParam(required = false) ServiceType serviceType) {

        log.info("REST request for CDR details by ID: id={}, serviceType={}", id, serviceType);
        CdrAnalysisDTO details = cdrAnalysisService.getCdrDetails(id, serviceType);
        return ResponseEntity.ok(details);
    }
}
