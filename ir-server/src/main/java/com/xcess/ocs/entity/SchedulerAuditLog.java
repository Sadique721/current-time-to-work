package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "scheduler_audit_log")
@SQLDelete(sql = "UPDATE scheduler_audit_log SET is_deleted = true, deleted_at = NOW() WHERE audit_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerAuditLog extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "scheduler_status_id")
    private Long schedulerStatusId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_message", columnDefinition = "TEXT")
    private String eventMessage;

    @Column(name = "cdr_start_time")
    private LocalDateTime cdrStartTime;

    @Column(name = "cdr_end_time")
    private LocalDateTime cdrEndTime;

    @Column(name = "records_processed")
    private Long recordsProcessed;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
