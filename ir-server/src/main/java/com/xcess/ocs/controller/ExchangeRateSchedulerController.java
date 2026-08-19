package com.xcess.ocs.controller;

import com.xcess.ocs.dto.ExchangeRateDTO;
import com.xcess.ocs.dto.ExchangeRateSchedulerStatusDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PaginationDetailsDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.dto.search.ExchangeRateSearchDTO;
import com.xcess.ocs.service.ExchangeRateSchedulerStatusService;
import com.xcess.ocs.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/exchange-rate-scheduler")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Exchange Rate Scheduler", description = "Endpoints for exchange rate scheduler management")
public class ExchangeRateSchedulerController {

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateSchedulerStatusService statusService;

    @Operation(summary = "Get latest scheduler status",
            description = "Retrieves the latest exchange rate scheduler execution status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT - No status found",
                    content = @Content(schema = @Schema(implementation = Void.class)))
    })
    @GetMapping("/status")
    public ResponseEntity<ExchangeRateSchedulerStatusDTO> getStatus() {
        log.info("REST request to get exchange rate scheduler status");
        ExchangeRateSchedulerStatusDTO status = statusService.getLatestStatus();
        return status != null ? ResponseEntity.ok(status) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "Manually trigger exchange rate fetch",
            description = "Triggers the exchange rate fetch job immediately")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "200", description = "Scheduler is already running")
    })
    @PostMapping("/trigger")
    public ResponseEntity<ResponseDTO> trigger() {
        log.info("REST request to manually trigger exchange rate fetch");

        Long executionId = statusService.acquireRunningStatus();
        if (executionId == null) {
            return ResponseEntity.ok(ResponseDTO.failed("Scheduler is already running. Try again later."));
        }

        try {
            int recordsSaved = exchangeRateService.fetchAndSaveRates();
            statusService.updateToSuccess(executionId, recordsSaved, "FRANKFURTER");
            return ResponseEntity.ok(ResponseDTO.ok(
                    String.format("Exchange rates fetched successfully. Saved %d rates.", recordsSaved)));
        } catch (Exception e) {
            log.error("Manual exchange rate fetch failed: {}", e.getMessage(), e);
            statusService.updateToFailed(executionId, e.getMessage());
            return ResponseEntity.ok(ResponseDTO.failed("Exchange rates fetch failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "Health check",
            description = "Returns the health status of the exchange rate scheduler")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        log.info("REST request to check exchange rate scheduler health");
        ExchangeRateSchedulerStatusDTO lastStatus = statusService.getLatestStatus();

        if (lastStatus == null) {
            return ResponseEntity.ok(new HealthResponse(false, null, null, null, null, "No runs yet", null));
        }

        boolean healthy = lastStatus.getStatus() == com.xcess.ocs.entity.ExchangeRateSchedulerStatus.Status.SUCCESS;
        long daysSinceLastSuccess = lastStatus.getEndTime() != null
                ? ChronoUnit.DAYS.between(lastStatus.getEndTime().toLocalDate(), LocalDate.now())
                : 0;

        return ResponseEntity.ok(new HealthResponse(
                healthy,
                lastStatus.getEndTime() != null ? lastStatus.getEndTime().toLocalDate().toString() : null,
                lastStatus.getEndTime(),
                (int) daysSinceLastSuccess,
                LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).plusDays(1),
                lastStatus.getErrorMessage(),
                lastStatus.getApiSourceUsed()
        ));
    }

    @Operation(summary = "Get paginated exchange rates",
            description = "Returns exchange rates sorted by valid_from descending (latest first)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @PostMapping("/rates")
    public ResponseEntity<PageResponseDTO<ExchangeRateDTO>> getRatesPaginated(
            @Valid @RequestBody PageRequestDTO<ExchangeRateSearchDTO> pageRequest) {
        log.info("REST request to get exchange rates page {} with size {}",
                pageRequest.getPage(), pageRequest.getPageSize());

        PageRequest pageable = PageRequest.of(pageRequest.getPage() - 1, pageRequest.getPageSize());
        Page<ExchangeRateDTO> ratePage = pageRequest.getSearchCriteria() != null
                ? exchangeRateService.searchExchangeRates(pageRequest.getSearchCriteria(), pageable)
                : exchangeRateService.getRatesPaginated(pageable);

        PaginationDetailsDTO pageDetails = new PaginationDetailsDTO(
                ratePage.getTotalPages(),
                ratePage.getTotalElements(),
                ratePage.getSize(),
                ratePage.getNumber() + 1
        );

        PageResponseDTO<ExchangeRateDTO> response = new PageResponseDTO<>();
        response.setPageDetails(pageDetails);
        response.setContent(ratePage.getContent());

        return ResponseEntity.ok(response);
    }

    private record HealthResponse(
            boolean healthy,
            String lastSuccessfulUpdate,
            LocalDateTime lastSuccessfulUpdateTime,
            Integer daysSinceLastSuccess,
            LocalDateTime nextScheduledRun,
            String errorMessage,
            String apiSourceUsed
    ) {}
}
