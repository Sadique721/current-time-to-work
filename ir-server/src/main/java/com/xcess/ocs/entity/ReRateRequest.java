package com.xcess.ocs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "re_rate_request")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReRateRequest implements Comparable<ReRateRequest> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", unique = true, nullable = false, length = 100)
    private String requestId;

    @Column(name = "audit_name", nullable = false, length = 200)
    private String auditName;

    @Column(name = "request_parameters", length = 1000)
    private String requestParameters;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_query_id")
    private CdrQueryConfig voiceQueryConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sms_query_id")
    private CdrQueryConfig smsQueryConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usage_query_id")
    private CdrQueryConfig usageQueryConfig;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // NEW, IN_PROGRESS, COMPLETED, FAILED, INTERRUPTED

    @Column(name = "enable", nullable = false)
    private Boolean enable = true;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_delete", nullable = false)
    private Boolean isDelete = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "modified_by", length = 100)
    private String modifiedBy;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        modifiedAt = LocalDateTime.now();
        if (requestedAt == null) requestedAt = LocalDateTime.now();
        if (status == null) status = com.xcess.ocs.constants.enums.RequestStatus.NEW.name();
        if (enable == null) enable = true;
        if (isActive == null) isActive = true;
        if (isDelete == null) isDelete = false;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    @Override
    public int compareTo(@NotNull ReRateRequest o) {
        if (o == null) return -1;
        if (this.requestedAt == null && o.requestedAt == null) return 0;
        if (this.requestedAt == null) return -1;
        if (o.requestedAt == null) return +1;
        return this.requestedAt.compareTo(o.requestedAt);
    }
}
