package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Represents a billing zone for INTERCONNECT and ROAMING rating.
 *
 * A zone groups one or more PLMN (Public Land Mobile Network) prefixes under a single
 * named zone. During TAP file rating, the visitedPlmn from a RoamingCdr is matched
 * against these prefixes using ZonePrefixTrie (O(k) lookup) to determine the zone name,
 * which is then used to find the applicable RatePackage via ZoneRateMapping.
 *
 * Example:
 *   zoneName     = "ZONE_EU"
 *   prefixPattern = "23801,23802,23803,26201,26202"  (comma-separated PLMN prefixes)
 *   minLength    = 5   (minimum PLMN string length for valid match)
 *   maxLength    = 6   (maximum PLMN string length for valid match)
 *
 * This entity mirrors tblm_zone from the reference project (adopt.ocsenginemanagement).
 */
@Entity
@Table(name = "zones", uniqueConstraints = {
    @UniqueConstraint(name = "uk_zones_name_deleted_at", columnNames = {"zone_name", "deleted_at"})
})
@SQLDelete(sql = "UPDATE zones SET is_deleted = true, deleted_at = NOW() WHERE zone_id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Zone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    /**
     * Human-readable zone name used as the lookup key in ZoneRateMapping.
     * e.g. "ZONE_EU", "ZONE_AFRICA", "ZONE_ASIA_PACIFIC"
     */
    @Column(name = "zone_name", nullable = false, length = 100)
    private String zoneName;

    /**
     * Comma-separated list of PLMN prefixes that belong to this zone.
     * Each prefix is inserted into ZonePrefixTrie at startup.
     * e.g. "23801,23802,26201,26202"
     *
     * During rating: RoamingCdr.visitedPlmn is matched against these prefixes
     * using longest-prefix match in ZonePrefixTrie.
     */
    @Column(name = "prefix_pattern", nullable = false, columnDefinition = "TEXT")
    private String prefixPattern;

    /**
     * Optional description of the zone for administrative purposes.
     */
    @Column(name = "zone_desc", length = 255)
    private String description;

    /**
     * Priority for zone lookup conflict resolution.
     * When a visitedPlmn matches multiple zones, the zone with the lowest priority number wins.
     * e.g. priority=1 beats priority=2.
     * Admins set this explicitly via the Zone management API.
     */
    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 100;

    /**
     * Controls how the admin entered prefixes for this zone.
     * MANUAL   → admin typed a comma-separated string directly.
     * DROPDOWN → admin selected from the prefix table and/or country list;
     *            the resolved values are stored normalized in prefixPattern.
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "prefix_input_mode", nullable = false, columnDefinition = "VARCHAR(20)")
    private PrefixInputMode prefixInputMode = PrefixInputMode.MANUAL;
}
