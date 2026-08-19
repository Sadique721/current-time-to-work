package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

/**
 * Maps a RatePackageGroup + Zone combination to a specific RatePackage for ROAMING rating.
 *
 * This is the bridge table that connects the zone lookup result back to a RatePackage.
 * Used exclusively for ROAMING line of business (lineOfBusiness = ROAMING).
 *
 * Rating flow using this entity:
 *   1. RoamingCdr.visitedPlmn → ZoneLookupService.getZoneForNumber(groupId, visitedPlmn) → zoneName
 *   2. ZoneRepository.findByZoneName(zoneName) → Zone entity → zoneId
 *   3. ZoneRateMappingRepository.findActiveByGroupIdAndZoneId(groupId, zoneId, callTime) → ZoneRateMapping
 *   4. ZoneRateMapping.ratePackage → RatePackage[type=ZONE, lineOfBusiness=ROAMING]
 *   5. RatePackage → calculateTotalCost() using pulse-based billing (same as INTERCONNECT)
 *
 * This entity mirrors tblt_rate_package_group_mapping from the reference project
 * (adopt.ocsenginemanagement), simplified for this project's conventions.
 */
@Entity
@Table(name = "zone_rate_mappings", indexes = {
        @Index(name = "idx_zone_rate_mapping_group", columnList = "rate_package_group_id"),
        @Index(name = "idx_zone_rate_mapping_zone", columnList = "zone_id")
})
@SQLDelete(sql = "UPDATE zone_rate_mappings SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneRateMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * The RatePackageGroup this mapping belongs to.
     * Linked to the roaming partner's Account → ProductPlan → RatePackageGroup chain.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_package_group_id", nullable = false)
    private RatePackageGroup ratePackageGroup;

    /**
     * The Zone that this mapping applies to.
     * Resolved from visitedPlmn via ZonePrefixTrie lookup.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    /**
     * The RatePackage to use when this zone is matched.
     * Must have: lineOfBusiness = ROAMING, ratePackageType = ZONE.
     * Contains the rate, pulse config, and rounding settings for cost calculation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_package_id", nullable = false)
    private RatePackage ratePackage;

    /**
     * Start of the validity period for this mapping.
     * Only mappings where callTime >= effectiveFrom are considered during rating.
     */
    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    /**
     * End of the validity period for this mapping.
     * Null means the mapping is open-ended (no expiry).
     * Only mappings where callTime <= effectiveTo (or effectiveTo IS NULL) are considered.
     */
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("effectiveFrom cannot be after effectiveTo for ZoneRateMapping");
        }
    }
}
