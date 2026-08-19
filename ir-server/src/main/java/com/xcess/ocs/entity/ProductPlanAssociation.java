package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;


@Entity
@Table(name = "product_plan_associations", indexes = {
        @jakarta.persistence.Index(name = "idx_product_plan_id", columnList = "product_plan_id"),
        @jakarta.persistence.Index(name = "idx_rate_package_group_id", columnList = "rate_package_group_id")
})
@SQLDelete(sql = "UPDATE product_plan_associations SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPlanAssociation extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_plan_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft deleting Product Plan
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ProductPlan productPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_package_group_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft deleting Rate Package Group
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RatePackageGroup ratePackageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;
}
