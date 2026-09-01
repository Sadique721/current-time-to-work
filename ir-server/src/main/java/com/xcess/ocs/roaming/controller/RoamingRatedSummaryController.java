package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.roaming.dto.RoamingRatedSummarySearchDTO;
import com.xcess.ocs.roaming.dto.RoamingRatedSummaryViewDTO;
import com.xcess.ocs.roaming.entity.RoamingRatedSummary;
import com.xcess.ocs.roaming.repository.RoamingRatedSummaryRepository;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/roaming/summary")
@RequiredArgsConstructor
public class RoamingRatedSummaryController {

    private final com.xcess.ocs.roaming.service.UnifiedTapSummaryService unifiedService;
    private final com.xcess.ocs.roaming.scheduler.RoamingSummaryScheduler roamingSummaryScheduler;

    @GetMapping
    public List<RoamingRatedSummaryViewDTO> list(@RequestParam(required = false) Long tapFileId,
                                                  @RequestParam(required = false) Long partnerId) {
        // Build a dummy search DTO for the unified service
        RoamingRatedSummarySearchDTO search = new RoamingRatedSummarySearchDTO();
        search.setPartnerId(partnerId);
        
        PageRequestDTO<RoamingRatedSummarySearchDTO> pageReq = new PageRequestDTO<>();
        pageReq.setPage(1);
        pageReq.setPageSize(10000); // effectively "all"
        pageReq.setSearchCriteria(search);
        
        return unifiedService.getUnifiedSummariesInPage(pageReq).getContent();
    }

    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<RoamingRatedSummaryViewDTO>> getSummariesInPage(
            @Valid @RequestBody PageRequestDTO<RoamingRatedSummarySearchDTO> pageRequestDTO) {

        PageResponseDTO<RoamingRatedSummaryViewDTO> response = unifiedService.getUnifiedSummariesInPage(pageRequestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger Roaming Summary Scheduler", description = "Manually triggers the roaming summary scheduler. Accepts an optional target date (YYYY-MM-DD). If omitted, defaults to yesterday.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK - Scheduler triggered successfully")
    })
    @PostMapping("/trigger-summary-scheduler")
    public ResponseEntity<String> triggerSummaryScheduler(
            @io.swagger.v3.oas.annotations.Parameter(description = "Target processing date in YYYY-MM-DD format (optional, defaults to yesterday)", example = "2026-07-20")
            @RequestParam(name = "date", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        if (date != null) {
            int count = roamingSummaryScheduler.runSummaryForDate(date);
            return ResponseEntity.ok("Roaming summary scheduler triggered successfully for date: " + date + " (created " + count + " summaries)");
        } else {
            roamingSummaryScheduler.runDailySummary();
            return ResponseEntity.ok("Roaming summary scheduler triggered successfully");
        }
    }
}
