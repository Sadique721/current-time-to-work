package com.xcess.ocs.service;

import com.xcess.ocs.dto.BillingSchedulerAuditLogDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.BillingSchedulerAuditLogSearchDTO;
import com.xcess.ocs.entity.BillingSchedulerAuditLog;
import com.xcess.ocs.mapper.BillingSchedulerAuditLogMapper;
import com.xcess.ocs.repository.BillingSchedulerAuditLogRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for managing billing scheduler audit logs.
 * Provides business logic for retrieving, searching, and saving audit logs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BillingSchedulerAuditLogService {

    private final BillingSchedulerAuditLogRepository auditLogRepository;
    private final BillingSchedulerAuditLogMapper auditLogMapper;

    /**
     * Event types for billing scheduler.
     */
    public static final String EVENT_BILLING_SCHEDULER_STARTED = "BILLING_SCHEDULER_STARTED";
    public static final String EVENT_BILLING_SCHEDULER_COMPLETED = "BILLING_SCHEDULER_COMPLETED";
    public static final String EVENT_BILLING_SCHEDULER_FAILED = "BILLING_SCHEDULER_FAILED";
    public static final String EVENT_BILLING_SCHEDULER_INTERRUPTED = "BILLING_SCHEDULER_INTERRUPTED";

    /**
     * Retrieves all billing scheduler audit logs with pagination.
     *
     * @param pageable Pagination information
     * @return Paginated response containing audit log DTOs
     */
    public PageResponseDTO<BillingSchedulerAuditLogDTO> getAuditLogsInPage(Pageable pageable) {
        log.debug("Fetching billing scheduler audit logs in pages");
        Page<BillingSchedulerAuditLog> auditLogsPage = auditLogRepository.findAllByOrderByCreatedDateDesc(pageable);
        List<BillingSchedulerAuditLogDTO> auditLogDTOs = auditLogsPage.getContent().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(auditLogDTOs, auditLogsPage);
    }

    /**
     * Searches billing scheduler audit logs with optional filters.
     *
     * @param searchDTO Search criteria containing optional filters
     * @param pageable Pagination information
     * @return Paginated response containing matching audit log DTOs
     */
    public PageResponseDTO<BillingSchedulerAuditLogDTO> searchAuditLogs(BillingSchedulerAuditLogSearchDTO searchDTO,
                                                                        Pageable pageable) {
        log.debug("Searching billing scheduler audit logs with criteria: {}", searchDTO);

        Long schedulerStatusId = searchDTO != null ? searchDTO.getSchedulerStatusId() : null;
        String eventType = searchDTO != null ? searchDTO.getEventType() : null;
        LocalDateTime createdAtFrom = searchDTO != null ? searchDTO.getCreatedAtFrom() : null;
        LocalDateTime createdAtTo = searchDTO != null ? searchDTO.getCreatedAtTo() : null;

        Page<BillingSchedulerAuditLog> auditLogsPage = auditLogRepository.searchAuditLogs(
                schedulerStatusId,
                eventType,
                createdAtFrom,
                createdAtTo,
                pageable
        );

        List<BillingSchedulerAuditLogDTO> auditLogDTOs = auditLogsPage.getContent().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Found {} audit logs matching criteria", auditLogDTOs.size());
        return PaginationUtils.buildGetResponseDTO(auditLogDTOs, auditLogsPage);
    }

    /**
     * Saves a billing scheduler audit log entry.
     *
     * @param schedulerStatusId The scheduler status ID
     * @param eventType The event type
     * @param eventMessage The event message
     * @param noOfAgreementsProcessed Number of agreements processed
     * @param noOfFailedInvoices Number of failed invoices
     * @param executionTimeMs Execution time in milliseconds
     * @param errorDetails Error details (optional, for failed events)
     * @return The saved audit log DTO
     */
    @Transactional
    public BillingSchedulerAuditLogDTO saveAuditLog(Long schedulerStatusId, String eventType,
                                                   String eventMessage, Long noOfAgreementsProcessed,
                                                   Long noOfFailedInvoices, Long executionTimeMs,
                                                   String errorDetails) {
        log.info("Saving billing scheduler audit log: eventType={}, agreements={}, failed={}",
                eventType, noOfAgreementsProcessed, noOfFailedInvoices);

        BillingSchedulerAuditLog auditLog = new BillingSchedulerAuditLog();
        auditLog.setSchedulerStatusId(schedulerStatusId);
        auditLog.setEventType(eventType);
        auditLog.setEventMessage(eventMessage);
        auditLog.setNoOfAgreementsProcessed(noOfAgreementsProcessed != null ? noOfAgreementsProcessed : 0L);
        auditLog.setNoOfFailedInvoices(noOfFailedInvoices != null ? noOfFailedInvoices : 0L);
        auditLog.setExecutionTimeMs(executionTimeMs != null ? executionTimeMs : 0L);
        auditLog.setErrorDetails(errorDetails);

        BillingSchedulerAuditLog saved = auditLogRepository.save(auditLog);
        log.info("Billing scheduler audit log saved with id: {}", saved.getAuditId());

        return auditLogMapper.toDto(saved);
    }

    /**
     * Saves a BILLING_SCHEDULER_STARTED audit log.
     *
     * @param schedulerStatusId The scheduler status ID
     * @param eventMessage The event message
     * @return The saved audit log DTO
     */
    @Transactional
    public BillingSchedulerAuditLogDTO logSchedulerStarted(Long schedulerStatusId, String eventMessage) {
        log.info("Logging BILLING_SCHEDULER_STARTED event");
        return saveAuditLog(schedulerStatusId, EVENT_BILLING_SCHEDULER_STARTED,
                eventMessage, 0L, 0L, 0L, null);
    }

    /**
     * Saves a BILLING_SCHEDULER_COMPLETED audit log.
     *
     * @param schedulerStatusId The scheduler status ID
     * @param noOfAgreementsProcessed Number of agreements processed
     * @param noOfFailedInvoices Number of failed invoices
     * @param executionTimeMs Execution time in milliseconds
     * @param eventMessage The event message
     * @return The saved audit log DTO
     */
    @Transactional
    public BillingSchedulerAuditLogDTO logSchedulerCompleted(Long schedulerStatusId, Long noOfAgreementsProcessed,
                                                            Long noOfFailedInvoices, Long executionTimeMs,
                                                            String eventMessage) {
        log.info("Logging BILLING_SCHEDULER_COMPLETED event: agreements={}, failed={}, time={}ms",
                noOfAgreementsProcessed, noOfFailedInvoices, executionTimeMs);
        return saveAuditLog(schedulerStatusId, EVENT_BILLING_SCHEDULER_COMPLETED,
                eventMessage, noOfAgreementsProcessed, noOfFailedInvoices, executionTimeMs, null);
    }

    /**
     * Saves a BILLING_SCHEDULER_FAILED audit log.
     *
     * @param schedulerStatusId The scheduler status ID
     * @param noOfAgreementsProcessed Number of agreements processed
     * @param noOfFailedInvoices Number of failed invoices
     * @param executionTimeMs Execution time in milliseconds
     * @param errorDetails Error details
     * @return The saved audit log DTO
     */
    @Transactional
    public BillingSchedulerAuditLogDTO logSchedulerFailed(Long schedulerStatusId, Long noOfAgreementsProcessed,
                                                         Long noOfFailedInvoices, Long executionTimeMs,
                                                         String errorDetails) {
        log.error("Logging BILLING_SCHEDULER_FAILED event: agreements={}, failed={}, time={}ms, error={}",
                noOfAgreementsProcessed, noOfFailedInvoices, executionTimeMs, errorDetails);
        return saveAuditLog(schedulerStatusId, EVENT_BILLING_SCHEDULER_FAILED,
                "Billing scheduler execution failed", noOfAgreementsProcessed, noOfFailedInvoices,
                executionTimeMs, errorDetails);
    }

    /**
     * Saves a BILLING_SCHEDULER_INTERRUPTED audit log.
     *
     * @param schedulerStatusId The scheduler status ID
     * @param noOfAgreementsProcessed Number of agreements processed before interruption
     * @param noOfFailedInvoices Number of failed invoices
     * @param executionTimeMs Execution time in milliseconds
     * @param errorDetails Error details
     * @return The saved audit log DTO
     */
    @Transactional
    public BillingSchedulerAuditLogDTO logSchedulerInterrupted(Long schedulerStatusId, Long noOfAgreementsProcessed,
                                                              Long noOfFailedInvoices, Long executionTimeMs,
                                                              String errorDetails) {
        log.warn("Logging BILLING_SCHEDULER_INTERRUPTED event: agreements={}, failed={}, time={}ms",
                noOfAgreementsProcessed, noOfFailedInvoices, executionTimeMs);
        return saveAuditLog(schedulerStatusId, EVENT_BILLING_SCHEDULER_INTERRUPTED,
                "Billing scheduler interrupted by application shutdown",
                noOfAgreementsProcessed, noOfFailedInvoices, executionTimeMs, errorDetails);
    }
}
