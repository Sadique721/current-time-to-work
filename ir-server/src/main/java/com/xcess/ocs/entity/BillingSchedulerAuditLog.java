package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Entity representing the billing scheduler audit log table.
 * Stores immutable records of all billing scheduler events for audit and tracking purposes.
 */
@Getter
@Setter
@Entity
@Table(name = "billing_scheduler_audit_log")
@SQLDelete(sql = "UPDATE billing_scheduler_audit_log SET is_deleted = true, deleted_at = NOW() WHERE audit_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class BillingSchedulerAuditLog extends BaseEntity {

    /**
     * Primary key for the audit log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    /**
     * Reference to the billing scheduler status.
     */
    @Column(name = "scheduler_status_id")
    private Long schedulerStatusId;

    /**
     * Type of event (e.g., BILLING_SCHEDULER_STARTED, BILLING_SCHEDULER_COMPLETED, BILLING_SCHEDULER_FAILED).
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * Human-readable description of the event.
     */
    @Column(name = "event_message", columnDefinition = "TEXT")
    private String eventMessage;

    /**
     * Number of agreements processed during this scheduler run.
     */
    @Column(name = "no_of_agreements_processed")
    private Long noOfAgreementsProcessed;

    /**
     * Number of failed invoices during this scheduler run.
     */
    @Column(name = "no_of_failed_invoices")
    private Long noOfFailedInvoices;

    /**
     * Execution time in milliseconds for this scheduler run.
     */
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    /**
     * Error details or stack trace if the scheduler run failed.
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
}
