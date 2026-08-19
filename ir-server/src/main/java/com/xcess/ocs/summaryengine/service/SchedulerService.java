package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.entity.*;
import com.xcess.ocs.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SchedulerService {

    @Autowired
    private SchedulerConfigurationRepository configRepository;

    @Autowired
    private SchedulerStatusRepository statusRepository;

    @Autowired
    private SchedulerAuditLogRepository auditRepository;

    @Autowired
    private SchedulerExecutorService schedulerExecutorService;

    private volatile boolean resetPending = false;

    @PostConstruct
    @Transactional
    public void recoverFromCrash() {
        log.info("Checking for scheduler status recovery on startup...");
        Optional<SchedulerStatus> existing = statusRepository.findTopByOrderByCreatedDateDesc();
        if (existing.isPresent()) {
            SchedulerStatus status = existing.get();
            if (status.getStatus() == SchedulerStatus.Status.RUNNING) {
                log.warn("Found RUNNING status from previous session - marking as FAILED for recovery");
                status.setStatus(SchedulerStatus.Status.FAILED);
                statusRepository.save(status);
                String failedMessage = String.format(
                        "Scheduler execution failed. CDR range: %s to %s. Error: Application crash/forced shutdown. Execution time: N/A",
                        status.getTargetedRatedCdrStartTime(), status.getTargetedRatedCdrEndTime());
                logAudit(status.getConfigId(), status.getSchedulerStatusId(),
                        "SCHEDULER_EXECUTED_FAILED", failedMessage,
                        status.getTargetedRatedCdrStartTime(), status.getTargetedRatedCdrEndTime(),
                        null, null, "Application crash/forced shutdown");
                log.info("Recovery complete - scheduler will retry from previous failed point");
            }
        }
    }

    @Transactional
    public void createSchedulerStatusIfNotExists(Long configId) {
        Optional<SchedulerStatus> existing = statusRepository.findTopByOrderByCreatedDateDesc();
        if (existing.isPresent()) {
            log.info("Scheduler status already exists — skipping creation");
            return;
        }
        SchedulerStatus status = new SchedulerStatus();
        status.setConfigId(configId);
        status.setStatus(SchedulerStatus.Status.PENDING);
        statusRepository.save(status);

        log.info("Created initial scheduler status for config: {}", configId);
    }

    @Transactional
    public void markResetPending() {
        SchedulerStatus schedulerStatus = statusRepository.findTopByOrderByCreatedDateDesc().orElse(null);

        if (schedulerStatus == null) {
            log.info("No scheduler status found — nothing to reset");
            return;
        }

        if (schedulerStatus.getStatus() == SchedulerStatus.Status.RUNNING) {
            this.resetPending = true;
            log.info("Scheduler is RUNNING — reset will happen after current run completes");
        } else {
            schedulerStatus.setStatus(SchedulerStatus.Status.PENDING);
            schedulerStatus.setNextSchedulerStartTime(null);
            schedulerStatus.setTargetedRatedCdrStartTime(null);
            statusRepository.save(schedulerStatus);
            log.info("Scheduler status immediately reset after config update");
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void checkAndExecuteSchedulers() {

        List<SchedulerConfiguration> configs = configRepository.findAll();
        if (configs.isEmpty()) {
            log.debug("No scheduler configuration found — skipping");
            return;
        }
        SchedulerConfiguration config = configs.get(0);

        if (!config.isActive()) {
            log.debug("Scheduler is inactive — skipping");
            return;
        }

        SchedulerStatus schedulerStatus = statusRepository.findTopByOrderByCreatedDateDesc()
                .orElseGet(() -> {
                    SchedulerStatus newStatus = new SchedulerStatus();
                    newStatus.setConfigId(config.getConfigId());
                    newStatus.setStatus(SchedulerStatus.Status.PENDING);
                    SchedulerStatus saved = statusRepository.save(newStatus);
                    log.info("Created scheduler status during polling for config: {}", config.getConfigId());
                    return saved;
                });

        if (schedulerStatus.getStatus() == SchedulerStatus.Status.RUNNING) {
            log.debug("Scheduler is already RUNNING — skipping");
            return;
        }

        if (schedulerStatus.getNextSchedulerStartTime() == null) {
            log.info("nextSchedulerStartTime is null — setting from config.startTimestamp: {}",
                    config.getStartTimestamp());
            schedulerStatus.setNextSchedulerStartTime(config.getStartTimestamp());
            statusRepository.save(schedulerStatus);
        }

        if (schedulerStatus.getNextSchedulerStartTime().isAfter(LocalDateTime.now())) {
            log.debug("nextSchedulerStartTime {} is in future — skipping",
                    schedulerStatus.getNextSchedulerStartTime());
            return;
        }

        if (schedulerStatus.getTargetedRatedCdrStartTime() == null) {
            log.info("targetedCdrStartTime is null — setting from config.targetedTimestamp: {}",
                    config.getTargetedTimestamp());
            schedulerStatus.setTargetedRatedCdrStartTime(config.getTargetedTimestamp());
            statusRepository.save(schedulerStatus);
        }

        boolean currentResetPending = this.resetPending;
        if (currentResetPending) {
            this.resetPending = false;
        }
        schedulerExecutorService.executeScheduler(schedulerStatus, config, currentResetPending);
    }

    public SchedulerStatus getLastScheduler() {
        return statusRepository.findTopByOrderByCreatedDateDesc().orElse(null);
    }

    @Transactional
    @PreDestroy
    public void onShutdown() {
        log.warn("Application shutting down — marking running scheduler as failed");
        statusRepository.findTopByOrderByCreatedDateDesc().ifPresent(scheduler -> {
            if (scheduler.getStatus() == SchedulerStatus.Status.RUNNING) {
                scheduler.setStatus(SchedulerStatus.Status.FAILED);
                statusRepository.save(scheduler);
                String failedMessage = String.format(
                        "Scheduler execution failed. CDR range: %s to %s. Error: Application shutdown. Execution time: N/A",
                        scheduler.getTargetedRatedCdrStartTime(), scheduler.getTargetedRatedCdrEndTime());
                logAudit(scheduler.getConfigId(), scheduler.getSchedulerStatusId(),
                        "SCHEDULER_EXECUTED_FAILED", failedMessage,
                        scheduler.getTargetedRatedCdrStartTime(), scheduler.getTargetedRatedCdrEndTime(),
                        null, null, "Application shutdown");
                log.info("Marked running scheduler as failed due to shutdown");
            }
        });
    }

    private void logAudit(Long configId, Long statusId, String eventType, String message,
                          LocalDateTime cdrStart, LocalDateTime cdrEnd, Long records,
                          Long executionTime, String errorDetails) {
        SchedulerAuditLog audit = new SchedulerAuditLog();
        audit.setConfigId(configId);
        audit.setSchedulerStatusId(statusId);
        audit.setEventType(eventType);
        audit.setEventMessage(message);
        audit.setCdrStartTime(cdrStart);
        audit.setCdrEndTime(cdrEnd);
        audit.setRecordsProcessed(records);
        audit.setExecutionTimeMs(executionTime);
        audit.setErrorDetails(errorDetails);
        auditRepository.save(audit);
    }
}