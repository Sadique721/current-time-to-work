package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Persistent status record for the RoamingSummaryScheduler.
 *
 * Mirrors ExchangeRateSchedulerStatus — each scheduler invocation
 * creates a new row, progressing from RUNNING → SUCCESS or FAILED.
 * Used by RoamingSummarySchedulerStatusService for concurrency guards
 * and crash-recovery via @PostConstruct.
 */
@Getter
@Setter
@Entity
@Table(name = "roaming_summary_scheduler_status")
@SQLDelete(sql = "UPDATE roaming_summary_scheduler_status SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class RoamingSummarySchedulerStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private Status status = Status.PENDING;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
