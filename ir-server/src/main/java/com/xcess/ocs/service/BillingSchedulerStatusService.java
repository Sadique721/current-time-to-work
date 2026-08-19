package com.xcess.ocs.service;

import com.xcess.ocs.dto.BillingSchedulerStatusDTO;
import com.xcess.ocs.entity.BillingSchedulerStatus;
import com.xcess.ocs.mapper.BillingSchedulerStatusMapper;
import com.xcess.ocs.repository.BillingSchedulerStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service class for managing billing scheduler status.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BillingSchedulerStatusService {

    private final BillingSchedulerStatusRepository billingSchedulerStatusRepository;
    private final BillingSchedulerStatusMapper billingSchedulerStatusMapper;

    /**
     * Retrieves the latest billing scheduler status.
     *
     * @return The latest status DTO or null if not found
     */
    public BillingSchedulerStatusDTO getLatestStatus() {
        log.debug("Fetching latest billing scheduler status");
        return billingSchedulerStatusRepository.findTopByOrderByCreatedDateDesc()
                .map(billingSchedulerStatusMapper::toDto)
                .orElse(null);
    }

    /**
     * Retrieves the billing scheduler status by ID.
     *
     * @param id The status ID
     * @return The status DTO or null if not found
     */
    public BillingSchedulerStatusDTO getStatusById(Long id) {
        log.debug("Fetching billing scheduler status by id: {}", id);
        return billingSchedulerStatusRepository.findById(id)
                .map(billingSchedulerStatusMapper::toDto)
                .orElse(null);
    }

    /**
     * Creates a new billing scheduler status record.
     *
     * @return The created status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO createStatus() {
        log.info("Creating new billing scheduler status");
        BillingSchedulerStatus status = new BillingSchedulerStatus();
        status.setStatus(BillingSchedulerStatus.Status.PENDING);
        BillingSchedulerStatus saved = billingSchedulerStatusRepository.save(status);
        log.info("Billing scheduler status created with id: {}", saved.getSchedulerStatusId());
        return billingSchedulerStatusMapper.toDto(saved);
    }

    /**
     * Gets or creates a billing scheduler status record.
     *
     * @return The existing or newly created status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO getOrCreateStatus() {
        return billingSchedulerStatusRepository.findTopByOrderByCreatedDateDesc()
                .map(billingSchedulerStatusMapper::toDto)
                .orElseGet(this::createStatus);
    }

    /**
     * Gets or creates a billing scheduler status entity.
     *
     * @return The existing or newly created status entity
     */
    @Transactional
    public BillingSchedulerStatus getOrCreateStatusEntity() {
        return billingSchedulerStatusRepository.findTopByOrderByCreatedDateDesc()
                .orElseGet(() -> {
                    log.info("Creating new billing scheduler status entity");
                    BillingSchedulerStatus status = new BillingSchedulerStatus();
                    status.setStatus(BillingSchedulerStatus.Status.PENDING);
                    return billingSchedulerStatusRepository.save(status);
                });
    }

    /**
     * Updates the status to RUNNING.
     *
     * @return The updated status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO updateToRunning() {
        log.info("Updating billing scheduler status to RUNNING");
        BillingSchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(BillingSchedulerStatus.Status.RUNNING);
        BillingSchedulerStatus saved = billingSchedulerStatusRepository.save(status);
        log.info("Billing scheduler status updated to RUNNING, id: {}", saved.getSchedulerStatusId());
        return billingSchedulerStatusMapper.toDto(saved);
    }

    /**
     * Updates the status to SUCCESS with completion details.
     *
     * @return The updated status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO updateToSuccess() {
        log.info("Updating billing scheduler status to SUCCESS");
        BillingSchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(BillingSchedulerStatus.Status.SUCCESS);
        status.setLastBillingRunTime(LocalDateTime.now());
        status.setNextBillingStartDate(LocalDateTime.now().plusDays(1).withHour(2).withMinute(0).withSecond(0));
        BillingSchedulerStatus saved = billingSchedulerStatusRepository.save(status);
        log.info("Billing scheduler status updated to SUCCESS, id: {}", saved.getSchedulerStatusId());
        return billingSchedulerStatusMapper.toDto(saved);
    }

    /**
     * Updates the status to FAILED with completion details.
     *
     * @return The updated status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO updateToFailed() {
        log.info("Updating billing scheduler status to FAILED");
        BillingSchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(BillingSchedulerStatus.Status.FAILED);
        status.setLastBillingRunTime(LocalDateTime.now());
        BillingSchedulerStatus saved = billingSchedulerStatusRepository.save(status);
        log.info("Billing scheduler status updated to FAILED, id: {}", saved.getSchedulerStatusId());
        return billingSchedulerStatusMapper.toDto(saved);
    }

    /**
     * Updates the status to INTERRUPTED (for shutdown handling).
     *
     * @return The updated status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO updateToInterrupted() {
        log.warn("Updating billing scheduler status to INTERRUPTED");
        BillingSchedulerStatus status = getOrCreateStatusEntity();
        status.setStatus(BillingSchedulerStatus.Status.FAILED);
        status.setLastBillingRunTime(LocalDateTime.now());
        BillingSchedulerStatus saved = billingSchedulerStatusRepository.save(status);
        log.warn("Billing scheduler status updated to INTERRUPTED, id: {}", saved.getSchedulerStatusId());
        return billingSchedulerStatusMapper.toDto(saved);
    }

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if status is RUNNING, false otherwise
     */
    public boolean isRunning() {
        return billingSchedulerStatusRepository.findTopByOrderByCreatedDateDesc()
                .map(status -> status.getStatus() == BillingSchedulerStatus.Status.RUNNING)
                .orElse(false);
    }

    /**
     * Creates or updates the billing scheduler status.
     *
     * @param dto the billing scheduler status DTO
     * @return The saved status DTO
     */
    @Transactional
    public BillingSchedulerStatusDTO createOrUpdateStatus(BillingSchedulerStatusDTO dto) {
        log.info("Creating or updating billing scheduler status");

        BillingSchedulerStatus status = billingSchedulerStatusRepository.findTopByOrderByCreatedDateDesc()
                .orElseGet(() -> {
                    BillingSchedulerStatus newStatus = new BillingSchedulerStatus();
                    log.info("Creating new billing scheduler status record");
                    return newStatus;
                });

        status.setStatus(dto.getStatus());
        status.setLastBillingRunTime(dto.getLastBillingRunTime());
        status.setNextBillingStartDate(dto.getNextBillingStartDate());

        BillingSchedulerStatus savedStatus = billingSchedulerStatusRepository.save(status);
        log.info("Billing scheduler status saved with id: {}", savedStatus.getSchedulerStatusId());

        return billingSchedulerStatusMapper.toDto(savedStatus);
    }
}
