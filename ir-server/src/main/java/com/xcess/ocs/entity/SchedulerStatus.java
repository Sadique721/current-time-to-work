package com.xcess.ocs.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "scheduler_status")
@SQLDelete(sql = "UPDATE scheduler_status SET is_deleted = true, deleted_at = NOW() WHERE scheduler_status_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduler_status_id")
    private Long schedulerStatusId;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "last_scheduler_run_time")
    private LocalDateTime lastSchedulerRunTime;

    @Column(name = "next_scheduler_start_time")
    private LocalDateTime nextSchedulerStartTime;

    @Column(name = "targeted_rated_cdr_start_time")
    private LocalDateTime targetedRatedCdrStartTime;

    @Column(name = "targeted_rated_cdr_end_time")
    private LocalDateTime targetedRatedCdrEndTime;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", insertable = false, updatable = false)
    private SchedulerConfiguration schedulerConfiguration;

    public enum Status {
        PENDING, RUNNING, SUCCESS, FAILED
    }
}