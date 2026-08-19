package com.xcess.ocs.repository;

import com.xcess.ocs.entity.BillingSchedulerAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for billing scheduler audit log database operations.
 */
@Repository
public interface BillingSchedulerAuditLogRepository extends JpaRepository<BillingSchedulerAuditLog, Long> {

    /**
     * Retrieves all audit logs ordered by creation date descending.
     *
     * @return List of audit logs
     */
    List<BillingSchedulerAuditLog> findBySchedulerStatusIdOrderByCreatedDateDesc(Long schedulerStatusId);

    /**
     * Retrieves all audit logs with pagination ordered by creation date descending.
     *
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<BillingSchedulerAuditLog> findAllByOrderByCreatedDateDesc(Pageable pageable);

    /**
     * Searches audit logs with optional filters for schedulerStatusId, eventType, and date range.
     *
     * @param schedulerStatusId Filter by scheduler status ID (optional)
     * @param eventType Filter by event type (optional, case-insensitive partial match)
     * @param createdAtFrom Filter by start date (optional)
     * @param createdAtTo Filter by end date (optional)
     * @param pageable Pagination information
     * @return Page of matching audit logs
     */
    @Query("SELECT b FROM BillingSchedulerAuditLog b WHERE " +
            "(:schedulerStatusId IS NULL OR b.schedulerStatusId = :schedulerStatusId) AND " +
            "(:eventType IS NULL OR LOWER(b.eventType) LIKE LOWER(CONCAT('%', :eventType, '%'))) AND " +
            "(:createdAtFrom IS NULL OR b.createdDate >= :createdAtFrom) AND " +
            "(:createdAtTo IS NULL OR b.createdDate <= :createdAtTo) " +
            "ORDER BY b.createdDate DESC")
    Page<BillingSchedulerAuditLog> searchAuditLogs(
            @Param("schedulerStatusId") Long schedulerStatusId,
            @Param("eventType") String eventType,
            @Param("createdAtFrom") LocalDateTime createdAtFrom,
            @Param("createdAtTo") LocalDateTime createdAtTo,
            Pageable pageable
    );
}
