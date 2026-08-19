package com.xcess.ocs.controller;

import com.xcess.ocs.dto.CdrQueryConfigDTO;
import com.xcess.ocs.dto.ErrorConfigCheckStatusDTO;
import com.xcess.ocs.dto.ReRateRequestDTO;
import com.xcess.ocs.service.ReRateRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rerate-requests")
@RequiredArgsConstructor
@Slf4j
public class ReRateRequestController {

    private final ReRateRequestService service;

    // ── ReRateRequest CRUD ────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ReRateRequestDTO> create(@RequestBody ReRateRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.createReRateRequest(dto));
        } catch (Exception e) {
            log.error("Exception occurred on creating re-rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on creating re-rate request: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ReRateRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.updateReRateRequest(dto, id));
        } catch (Exception e) {
            log.error("Exception occurred on updating re-rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on updating re-rate request: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.deleteReRateRequest(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Exception occurred on deleting re-rate request: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on deleting re-rate request: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> fetchAll() {
        return ResponseEntity.ok(service.fetchReRateRequestsWithoutPagination());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReRateRequestDTO> fetchById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.fetchReRateRequestById(id));
        } catch (Exception e) {
            log.error("Exception occurred on fetching re-rate request by id: {}", e.getMessage(), e);
            throw new RuntimeException("Exception occurred on fetching re-rate request by id: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    public ResponseEntity<?> fetchPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReRateRequestDTO> p = service.fetchReRateRequestsWithPagination(page, size);
        return ResponseEntity.ok(p);
    }

    @PutMapping("/editStatusAs/{status}/for/{reRateRequestId}")
    public ResponseEntity<?> editReRateStatus(
            @PathVariable("reRateRequestId") String reRateRequestId,
            @PathVariable("status") String status) {
        service.editReRateStatus(reRateRequestId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fetchErrorConfigCheckResult/{page}/{size}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> fetchErrorConfigCheckResult(
            @PathVariable("page") Integer page,
            @PathVariable("size") Integer size) {
        Map<String, Object> response = service.fetchErrorConfigCheckResult(page, size);
        return ResponseEntity.ok(response);
    }

    // ── CdrQueryConfig CRUD ───────────────────────────────────────────────────

    @PutMapping("/query-configs/{id}")
    public ResponseEntity<CdrQueryConfigDTO> updateQueryConfig(@PathVariable Long id,
            @RequestBody CdrQueryConfigDTO dto) {
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
