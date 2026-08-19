package com.xcess.ocs.controller;

import com.xcess.ocs.entity.SchedulerStatus;
import com.xcess.ocs.summaryengine.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedulerStatus")
@RequiredArgsConstructor
@Slf4j
public class SchedulerStatusController {

    private final SchedulerService schedulerService;

    @GetMapping
    public ResponseEntity<SchedulerStatus> getSchedulerStatus() {
        log.info("REST request to get latest running scheduler for NORMAL interconnect");
        SchedulerStatus scheduler = schedulerService.getLastScheduler();
        return scheduler != null ? ResponseEntity.ok(scheduler) : ResponseEntity.noContent().build();
    }
}
