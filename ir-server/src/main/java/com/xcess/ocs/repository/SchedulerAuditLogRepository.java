package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SchedulerAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SchedulerAuditLogRepository extends JpaRepository<SchedulerAuditLog, Long> {

    List<SchedulerAuditLog> findByConfigIdOrderByCreatedDateDesc(Long configId);

    Page<SchedulerAuditLog> findAllByOrderByCreatedDateDesc(Pageable pageable);

    @Query("SELECT s FROM SchedulerAuditLog s WHERE " +
            "(:configId IS NULL OR s.configId = :configId) AND " +
            "(:eventType IS NULL OR LOWER(s.eventType) LIKE LOWER(CONCAT('%', :eventType, '%'))) AND " +
            "(:createdAtFrom IS NULL OR s.createdDate >= :createdAtFrom) AND " +  // createdAt → createdDate
            "(:createdAtTo IS NULL OR s.createdDate <= :createdAtTo) AND " +      // createdAt → createdDate
            "(:schedulerStatusId IS NULL OR s.schedulerStatusId = :schedulerStatusId) " +
            "ORDER BY s.createdDate DESC")                                         // createdAt → createdDate
    Page<SchedulerAuditLog> searchAuditLogs(
            @Param("configId") Long configId,
            @Param("eventType") String eventType,
            @Param("createdAtFrom") LocalDateTime createdAtFrom,
            @Param("createdAtTo") LocalDateTime createdAtTo,
            @Param("schedulerStatusId") Long schedulerStatusId,
            Pageable pageable
    );
}
