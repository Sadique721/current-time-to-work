package com.xcess.ocs.controller;

import com.xcess.ocs.dto.CdrQueryConfigDTO;
import com.xcess.ocs.dto.ErrorRateRequestDTO;
import com.xcess.ocs.service.ErrorRateRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/error-rate-requests")
@RequiredArgsConstructor
@Slf4j
public class ErrorRateRequestController {

    private final ErrorRateRequestService service;

    // ── ErrorRateRequest CRUD ─────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ErrorRateRequestDTO> create(@RequestBody ErrorRateRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.createErrorRateRequest(dto));
        } catch (Exception e) {
            log.error("Exception occurred on creating error rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on creating error rate request: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ErrorRateRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.updateErrorRateRequest(dto, id));
        } catch (Exception e) {
            log.error("Exception occurred on updating error rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on updating error rate request: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteErrorRateRequest(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Exception occurred on deleting error rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on deleting error rate request: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(service.fetchErrorRateRequestsWithoutPagination());
    }

    @GetMapping("/page")
    public ResponseEntity<?> fetchPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ErrorRateRequestDTO> p = service.fetchErrorRateRequestsWithPagination(page, size);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/editStatusAs/{status}/for/{errorRateRequestId}")
    public ResponseEntity<?> editErrorRateStatus(
            @PathVariable("errorRateRequestId") String errorRateRequestId,
            @PathVariable("status") String status) {
        service.editErrorRateStatus(errorRateRequestId, status);
        return ResponseEntity.ok().build();
    }


    @PutMapping("/query-configs/{id}")
    public ResponseEntity<CdrQueryConfigDTO> updateQueryConfig(@PathVariable Long id, @RequestBody CdrQueryConfigDTO dto) {
        try {
            return ResponseEntity.ok(service.updateQueryConfig(id, dto));
        } catch (Exception e) {
            log.error("Exception occurred on updating query config: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on updating query config: " + e.getMessage());
        }
    }

    @DeleteMapping("/query-configs/{id}")
    public ResponseEntity<?> deleteQueryConfig(@PathVariable Long id) {
        try {
            service.deleteQueryConfig(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Exception occurred on deleting query config: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on deleting query config: " + e.getMessage());
        }
    }

    @GetMapping("/query-configs")
    public ResponseEntity<List<CdrQueryConfigDTO>> fetchAllQueryConfigs() {
        return ResponseEntity.ok(service.fetchAllQueryConfigs());
    }

    @GetMapping("/query-configs/{id}")
    public ResponseEntity<CdrQueryConfigDTO> fetchQueryConfigById(@PathVariable Long id) {
        return ResponseEntity.ok(service.fetchQueryConfigById(id));
    }
}
