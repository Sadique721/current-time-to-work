package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "billing_scheduler_status")
@SQLDelete(sql = "UPDATE billing_scheduler_status SET is_deleted = true, deleted_at = NOW() WHERE scheduler_status_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class BillingSchedulerStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduler_status_id")
    private Long schedulerStatusId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "last_billing_run_time")
    private LocalDateTime lastBillingRunTime;

    @Column(name = "next_billing_start_date")
    private LocalDateTime nextBillingStartDate;

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }
}
