package com.xcess.ocs.roaming.service;

import com.xcess.ocs.roaming.entity.RoamingSummarySchedulerStatus;
import com.xcess.ocs.roaming.repository.RoamingSummarySchedulerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class for managing roaming summary scheduler status.
 * Patterned after BillingSchedulerStatusService (singleton-like row updates).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoamingSummarySchedulerStatusService {

    private final RoamingSummarySchedulerStatusRepository repository;

    /**
     * Gets or creates a roaming summary scheduler status entity.
     *
     * @return The existing or newly created status entity
     */
    @Transactional
    public RoamingSummarySchedulerStatus getOrCreateStatusEntity() {
        return repository.findTopByOrderByCreatedDateDesc()
                .orElseGet(() -> {
                    log.info("Creating new roaming summary scheduler status entity");
                    RoamingSummarySchedulerStatus status = new RoamingSummarySchedulerStatus();
                    status.setStatus(RoamingSummarySchedulerStatus.Status.PENDING);
                    return repository.save(status);
                });
    }

    /**
     * Updates the status to RUNNING.
     */
    @Transactional
    public void updateToRunning() {
        log.info("Updating roaming summary scheduler status to RUNNING");
        RoamingSummarySchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(RoamingSummarySchedulerStatus.Status.RUNNING);
        status.setStartTime(LocalDateTime.now());
        status.setErrorMessage(null);
        repository.save(status);
    }

    /**
     * Updates the status to SUCCESS.
     */
    @Transactional
    public void updateToSuccess(int recordsProcessed) {
        log.info("Updating roaming summary scheduler status to SUCCESS");
        RoamingSummarySchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(RoamingSummarySchedulerStatus.Status.SUCCESS);
        status.setEndTime(LocalDateTime.now());
        status.setRecordsProcessed(recordsProcessed);
        repository.save(status);
    }

    /**
     * Updates the status to FAILED.
     */
    @Transactional
    public void updateToFailed(String errorMessage) {
        log.info("Updating roaming summary scheduler status to FAILED");
        RoamingSummarySchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(RoamingSummarySchedulerStatus.Status.FAILED);
        status.setEndTime(LocalDateTime.now());
        status.setErrorMessage(errorMessage);
        repository.save(status);
    }

    /**
     * Updates the status to INTERRUPTED (for shutdown handling).
     */
    @Transactional
    public void updateToInterrupted() {
        log.warn("Updating roaming summary scheduler status to INTERRUPTED");
        RoamingSummarySchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(RoamingSummarySchedulerStatus.Status.FAILED);
        status.setEndTime(LocalDateTime.now());
        status.setErrorMessage("Scheduler interrupted by application shutdown during execution");
        repository.save(status);
    }

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if status is RUNNING, false otherwise
     */
    public boolean isRunning() {
        return repository.findTopByOrderByCreatedDateDesc()
                .map(status -> status.getStatus() == RoamingSummarySchedulerStatus.Status.RUNNING)
                .orElse(false);
    }
}
