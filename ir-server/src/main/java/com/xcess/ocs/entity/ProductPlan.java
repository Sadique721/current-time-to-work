package com.xcess.ocs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_plans", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_plans_name", columnNames = {"name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE product_plans SET is_deleted = true, deleted_at = NOW() WHERE product_plan_id = ?")
@Where(clause = "is_deleted = false")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPlan extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productPlanId;

    @Column(name = "name", nullable = false)
    @NotEmpty(message = "Product plan name is required")
    @Size(min = 6, max = 100, message = "Name must be between 6 and 100 characters")
    private String name;

    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    @Size(min = 6, max = 100, message = "Description must be between 6 and 100 characters")
    private String description;

    @Column(nullable = false)
    @NotNull(message = "Package type is required")
    @Enumerated(EnumType.STRING)
    private PackageType packageType;

    @Builder.Default
    @OneToMany(mappedBy = "productPlan", cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductPlanAssociation> ratePackageGroups = new ArrayList<>();

    public enum PackageType {
        SELLING("SELLING"),
        BUYING("BUYING");

        private final String value;

        PackageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static PackageType fromString(String value) {
            for (PackageType type : PackageType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid PackageType: " + value);
        }
    }

    // Helper methods for managing associations
    public void addRatePackageGroupAssociation(ProductPlanAssociation association) {
        ratePackageGroups.add(association);
        association.setProductPlan(this);
    }

    public void removeRatePackageGroupAssociation(ProductPlanAssociation association) {
        ratePackageGroups.remove(association);
        association.setProductPlan(null);
    }
}
