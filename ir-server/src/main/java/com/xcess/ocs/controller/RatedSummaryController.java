package com.xcess.ocs.controller;

import com.xcess.ocs.dto.RatedSummaryDTO;
import com.xcess.ocs.summaryengine.service.RatedSummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rated-summary")
public class RatedSummaryController {

    @Autowired
    private RatedSummaryService ratedSummaryService;

    @GetMapping("/account/{accountCode}")
    public ResponseEntity<List<RatedSummaryDTO>> getSummariesByAccount(
            @PathVariable String accountCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ratedSummaryService.getSummariesByAccount(accountCode, startDate, endDate));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<RatedSummaryDTO>> getSummariesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(ratedSummaryService.getSummariesByDate(date, direction));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<RatedSummaryDTO>> getSummariesByPartner(
            @PathVariable Long partnerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ratedSummaryService.getSummariesByPartner(partnerId, startDate, endDate));
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateSummaries(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        long count = ratedSummaryService.generateDailySummaries(startTime, endTime);
        return ResponseEntity.ok(Map.of(
                "message", "Summaries generated successfully",
                "processedCount", count));
    }
}
