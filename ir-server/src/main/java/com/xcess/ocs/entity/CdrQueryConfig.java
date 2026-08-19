package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cdr_query_config")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CdrQueryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_name", nullable = false, length = 200)
    private String queryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, columnDefinition = "ENUM('VOICE','SMS','USAGE')")
    private ServiceType serviceType;

    @Column(name = "fetch_query", nullable = false, columnDefinition = "TEXT")
    private String fetchQuery;

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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        modifiedAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
        if (isDelete == null) isDelete = false;
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }
}
