package com.xcess.ocs.entity;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageGroup;
import com.xcess.ocs.roaming.entity.CallType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;


import java.time.LocalDateTime;

@Entity
@Table(name = "rate_package_associations", indexes = {
        @jakarta.persistence.Index(name = "idx_rate_package_group_id", columnList = "rate_package_group_id"),
        @jakarta.persistence.Index(name = "idx_rate_package_id", columnList = "rate_package_id")
})
@SQLDelete(sql = "UPDATE rate_package_associations SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatePackageAssociation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_package_group_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft-deleting Rate Package Group
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RatePackageGroup ratePackageGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_package_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft-deleting Rate Package
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RatePackage ratePackage;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(name = "priority")
    private Integer priority;

    /** ROAMING expression: maps a specific callType to this RatePackage within the RPG.
     *  e.g. callType=MO_VOICE → RatePackage_A, callType=MT_VOICE → RatePackage_B.
     *  Null for INTERCONNECT associations. */
    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", columnDefinition = "VARCHAR(20)")
    private CallType callType;

    /** Dynamic expression to select this RatePackage.
     *  e.g. "homePlmn=123076" or "accountCode=123076"
     *  Evaluated via reflection on the CDR object. */
    @Column(name = "expression")
    private String expression;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Start time cannot be after end time");
        }
        
        boolean hasCallType = callType != null;
        boolean hasExpression = expression != null && !expression.trim().isEmpty();
        
        if (hasCallType && hasExpression) {
            throw new IllegalArgumentException("RatePackageAssociation cannot have both callType and expression set at the same time");
        }
    }
}