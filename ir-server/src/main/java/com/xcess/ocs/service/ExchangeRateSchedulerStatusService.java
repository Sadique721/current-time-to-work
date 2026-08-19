package com.xcess.ocs.service;

import com.xcess.ocs.dto.ExchangeRateSchedulerStatusDTO;
import com.xcess.ocs.entity.ExchangeRateSchedulerStatus;
import com.xcess.ocs.mapper.ExchangeRateSchedulerStatusMapper;
import com.xcess.ocs.repository.ExchangeRateSchedulerStatusRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ExchangeRateSchedulerStatusService {

    private final ExchangeRateSchedulerStatusRepository repository;
    private final ExchangeRateSchedulerStatusMapper mapper;

    @PostConstruct
    @Transactional
    public void recoverFromCrash() {
        repository.findTopByOrderByCreatedDateDesc().ifPresent(status -> {
            if (status.getStatus() == ExchangeRateSchedulerStatus.Status.RUNNING) {
                log.warn("Crash recovery: marking RUNNING status id={} as FAILED", status.getId());
                status.setStatus(ExchangeRateSchedulerStatus.Status.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setErrorMessage("Application crashed or was killed during execution");
                repository.save(status);
            }
        });
    }

    public ExchangeRateSchedulerStatusDTO getLatestStatus() {
        return repository.findTopByOrderByCreatedDateDesc()
                .map(mapper::toDto)
                .orElse(null);
    }

    ExchangeRateSchedulerStatus getOrCreateStatusEntity() {
        return repository.findTopByOrderByCreatedDateDesc()
                .orElseGet(() -> {
                    ExchangeRateSchedulerStatus status = new ExchangeRateSchedulerStatus();
                    status.setStatus(ExchangeRateSchedulerStatus.Status.PENDING);
                    return repository.save(status);
                });
    }

    @Transactional
    public synchronized Long acquireRunningStatus() {
        if (isRunning()) {
            return null;
        }
        ExchangeRateSchedulerStatus status = new ExchangeRateSchedulerStatus();
        status.setStatus(ExchangeRateSchedulerStatus.Status.RUNNING);
        status.setStartTime(LocalDateTime.now());
        return repository.save(status).getId();
    }

    @Transactional
    public ExchangeRateSchedulerStatusDTO updateToSuccess(Long executionId, int recordsProcessed, String apiSource) {
        ExchangeRateSchedulerStatus status = repository.findById(executionId)
                .orElseGet(this::getOrCreateStatusEntity);
        status.setStatus(ExchangeRateSchedulerStatus.Status.SUCCESS);
        status.setEndTime(LocalDateTime.now());
        status.setRecordsProcessed(recordsProcessed);
        status.setApiSourceUsed(apiSource);
        ExchangeRateSchedulerStatus saved = repository.save(status);
        return mapper.toDto(saved);
    }

    @Transactional
    public ExchangeRateSchedulerStatusDTO updateToFailed(Long executionId, String errorMessage) {
        ExchangeRateSchedulerStatus status = executionId != null
                ? repository.findById(executionId).orElseGet(this::getOrCreateStatusEntity)
                : getOrCreateStatusEntity();
        status.setStatus(ExchangeRateSchedulerStatus.Status.FAILED);
        status.setEndTime(LocalDateTime.now());
        status.setErrorMessage(errorMessage);
        ExchangeRateSchedulerStatus saved = repository.save(status);
        return mapper.toDto(saved);
    }

    @Transactional
    public ExchangeRateSchedulerStatusDTO updateToInterrupted(Long executionId) {
        ExchangeRateSchedulerStatus status = executionId != null
                ? repository.findById(executionId).orElseGet(this::getOrCreateStatusEntity)
                : getOrCreateStatusEntity();
        status.setStatus(ExchangeRateSchedulerStatus.Status.FAILED);
        status.setEndTime(LocalDateTime.now());
        status.setErrorMessage("Scheduler interrupted by application shutdown during execution");
        ExchangeRateSchedulerStatus saved = repository.save(status);
        return mapper.toDto(saved);
    }

    @Transactional
    public void markInterruptedIfRunning() {
        repository.findTopByOrderByCreatedDateDesc().ifPresent(status -> {
            if (status.getStatus() == ExchangeRateSchedulerStatus.Status.RUNNING) {
                log.warn("Shutdown: marking RUNNING status id={} as FAILED", status.getId());
                status.setStatus(ExchangeRateSchedulerStatus.Status.FAILED);
                status.setEndTime(LocalDateTime.now());
                status.setErrorMessage("Scheduler interrupted by application shutdown during execution");
                repository.save(status);
            }
        });
    }

    public boolean isRunning() {
        return repository.findTopByOrderByCreatedDateDesc()
                .map(s -> s.getStatus() == ExchangeRateSchedulerStatus.Status.RUNNING)
                .orElse(false);
    }
}
