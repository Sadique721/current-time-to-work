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
@Table(name = "rate_package_groups", uniqueConstraints = {
    @UniqueConstraint(name = "uk_rate_package_groups_name", columnNames = {"name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE rate_package_groups SET is_deleted = true, deleted_at = NOW() WHERE rate_package_group_id = ?")
@Where(clause = "is_deleted = false")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatePackageGroup extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratePackageGroupId;

    @Column(name = "name", nullable = false)
    @NotEmpty(message = "Rate package group name is required")
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

    /** For ROAMING groups: the service type this group handles (VOICE, SMS, USAGE).
     *  Combined with callType to uniquely select the RatePackageGroup from the ProductPlan.
     *  Null for INTERCONNECT groups. */
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;



    /** Determines how a RatePackage is selected within this group.
     *  PRIORITY: ordered fallback by priority number.
     *  CALL_TYPE: matched by call type (MO_VOICE, MT_VOICE, MO_SMS, MT_SMS) for ROAMING. */
    @Enumerated(EnumType.STRING)
    @Column(name = "rate_package_selection_type", nullable = false)
    private RatePackageSelectionType ratePackageSelectionType;

    @Builder.Default
    @OneToMany(mappedBy = "ratePackageGroup", cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<RatePackageAssociation> ratePackageAssociations = new ArrayList<>();

    public enum RatePackageSelectionType {
        PRIORITY, CALL_TYPE, EXPRESSION
    }

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
    public void addRatePackageAssociation(RatePackageAssociation association) {
        ratePackageAssociations.add(association);
        association.setRatePackageGroup(this);
    }

    public void removeRatePackageAssociation(RatePackageAssociation association) {
        ratePackageAssociations.remove(association);
        association.setRatePackageGroup(null);
    }
}
