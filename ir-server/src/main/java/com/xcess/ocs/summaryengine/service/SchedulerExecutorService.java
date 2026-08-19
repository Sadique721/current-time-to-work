package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.entity.*;
import com.xcess.ocs.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class SchedulerExecutorService {

    @Autowired
    private SchedulerStatusRepository statusRepository;

    @Autowired
    private SchedulerAuditLogRepository auditRepository;

    @Autowired
    private RatedSummaryService ratedSummaryService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeScheduler(SchedulerStatus schedulerStatus, SchedulerConfiguration config,
                                 boolean resetPending) {
        long startTimeMs = System.currentTimeMillis();

        LocalDateTime cdrStartTime = schedulerStatus.getTargetedRatedCdrStartTime();
        LocalDateTime endTime = calculateEndTime(cdrStartTime,
                config.getTargetedIntervalType(), config.getTargetedMaxIntervalValue());

        schedulerStatus.setStatus(SchedulerStatus.Status.RUNNING);
        schedulerStatus.setLastSchedulerRunTime(LocalDateTime.now());
        schedulerStatus.setTargetedRatedCdrEndTime(endTime);
        statusRepository.save(schedulerStatus);

        try {
            long recordsProcessed = ratedSummaryService.generateDailySummaries(cdrStartTime, endTime);
            long executionTimeMs = System.currentTimeMillis() - startTimeMs;

            LocalDateTime lastModifiedDate = ratedSummaryService.getLastModifiedDateOfProcessedCdrs(
                    cdrStartTime, endTime);

            if (resetPending) {
                log.info("Config was updated — resetting scheduler status after completed run");
                String successMessage = String.format(
                        "Scheduler executed successfully. CDR range: %s to %s. Records processed: %d. Execution time: %dms. Status reset due to config update.",
                        cdrStartTime, lastModifiedDate, recordsProcessed, executionTimeMs);
                logAudit(schedulerStatus.getConfigId(), schedulerStatus.getSchedulerStatusId(),
                        "SCHEDULER_EXECUTED_SUCCESS", successMessage,
                        cdrStartTime, lastModifiedDate, recordsProcessed, executionTimeMs, null);
                schedulerStatus.setStatus(SchedulerStatus.Status.PENDING);
                schedulerStatus.setNextSchedulerStartTime(null);
                schedulerStatus.setTargetedRatedCdrStartTime(null);
                statusRepository.save(schedulerStatus);
                log.info("Scheduler status reset complete");
                return;
            }

            LocalDateTime effectiveEnd = (lastModifiedDate != null && lastModifiedDate.isBefore(endTime))
                    ? lastModifiedDate : endTime;
            LocalDateTime nextCdrStartTime = effectiveEnd.plus(1, ChronoUnit.MILLIS);

            LocalDateTime nextSchedulerTime = calculateNextTime(
                    schedulerStatus.getNextSchedulerStartTime(),
                    config.getIntervalType(), config.getIntervalValue());
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(nextSchedulerTime)) {
                nextSchedulerTime = now;
            }

            String successMessage = String.format(
                    "Scheduler executed successfully. CDR range: %s to %s. Records processed: %d. Execution time: %dms. Next run scheduled at: %s",
                    cdrStartTime, lastModifiedDate, recordsProcessed, executionTimeMs, nextSchedulerTime);
            logAudit(schedulerStatus.getConfigId(), schedulerStatus.getSchedulerStatusId(),
                    "SCHEDULER_EXECUTED_SUCCESS", successMessage,
                    cdrStartTime, lastModifiedDate, recordsProcessed, executionTimeMs, null);

            schedulerStatus.setStatus(SchedulerStatus.Status.SUCCESS);
            schedulerStatus.setTargetedRatedCdrStartTime(nextCdrStartTime);
            schedulerStatus.setNextSchedulerStartTime(nextSchedulerTime);
            schedulerStatus.setTargetedRatedCdrEndTime(lastModifiedDate);
            statusRepository.save(schedulerStatus);

            log.info("Scheduler completed — nextCdrStart: {}, nextSchedulerTime: {}",
                    nextCdrStartTime, nextSchedulerTime);

        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTimeMs;

            schedulerStatus.setStatus(SchedulerStatus.Status.FAILED);
            schedulerStatus.setNextSchedulerStartTime(LocalDateTime.now().plusMinutes(5));
            statusRepository.save(schedulerStatus);

            String failedMessage = String.format(
                    "Scheduler execution failed. CDR range: %s to %s. Error: %s. Execution time: %dms",
                    cdrStartTime, endTime, e.getMessage(), executionTimeMs);
            logAudit(schedulerStatus.getConfigId(), schedulerStatus.getSchedulerStatusId(),
                    "SCHEDULER_EXECUTED_FAILED", failedMessage,
                    cdrStartTime, endTime, null, executionTimeMs, e.toString());

            log.error("Scheduler execution failed: {}", e.getMessage(), e);
        }
    }

    private LocalDateTime calculateNextTime(LocalDateTime current,
                                            SchedulerConfiguration.IntervalType type, int value) {
        return switch (type) {
            case HOUR -> current.plusHours(value);
            case DAY -> current.plusDays(value);
            case WEEK -> current.plusWeeks(value);
        };
    }

    private LocalDateTime calculateEndTime(LocalDateTime start,
                                           SchedulerConfiguration.IntervalType type, int value) {
        return switch (type) {
            case HOUR -> start.plusHours(value);
            case DAY -> start.plusDays(value);
            case WEEK -> start.plusWeeks(value);
        };
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