package com.xcess.ocs.controller;

import com.xcess.ocs.dto.SchedulerConfigurationDTO;
import com.xcess.ocs.summaryengine.service.SchedulerConfigurationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/scheduler")
@Slf4j
public class SchedulerConfigurationController {

    @Autowired
    private SchedulerConfigurationService schedulerConfigurationService;

    @PostMapping
    public ResponseEntity<String> createSchedulerConfiguration(
            @Valid @RequestBody SchedulerConfigurationDTO schedulerConfigurationDTO) {
        log.info("REST request to create scheduler configuration");
        schedulerConfigurationService.createOrUpdate(schedulerConfigurationDTO);
        return ResponseEntity.ok("Scheduler configuration created successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateSchedulerConfiguration(
            @PathVariable Long id,
            @Valid @RequestBody SchedulerConfigurationDTO schedulerConfigurationDTO) {
        log.info("REST request to update scheduler configuration: {}", id);
        schedulerConfigurationService.createOrUpdate(schedulerConfigurationDTO);
        return ResponseEntity.ok("Scheduler configuration updated successfully");
    }

    @PatchMapping
    public ResponseEntity<String> updateActiveStatus(@RequestParam boolean isActive) {
        log.info("REST request to change scheduler active status to: {}", isActive);
        schedulerConfigurationService.updateActiveStatus(isActive);
        return ResponseEntity.ok("Active status updated successfully");
    }

    @GetMapping
    public ResponseEntity<SchedulerConfigurationDTO> getSchedulerConfig() {
        log.info("REST request to get scheduler configuration");
        return ResponseEntity.ok(schedulerConfigurationService.getConfig());
    }
}