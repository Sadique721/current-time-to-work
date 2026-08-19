package com.xcess.ocs.repository;

import com.xcess.ocs.entity.PrefixInputMode;
import com.xcess.ocs.entity.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Zone entity.
 *
 * Zones are used for INTERCONNECT and ROAMING line of business rating.
 * Each zone maps a set of prefixes (phone number prefixes or PLMN prefixes)
 * under a single named zone, resolved to a RatePackage via ZoneRateMapping.
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    /**
     * Find a zone by its exact name.
     * Used after ZonePrefixTrie returns a zone name to load the full Zone entity
     * (needed to get zoneId for ZoneRateMapping lookup).
     */
    Optional<Zone> findByZoneName(String zoneName);

    /**
     * Find all zones ordered by zone name.
     * Used during ZoneLookupService warm-up to load all zones into ZonePrefixTrie.
     */
    @Query("SELECT z FROM Zone z ORDER BY z.zoneName")
    List<Zone> findAllForTrieWarmUp();

    /**
     * Check if a zone with the given name exists and is not soft-deleted.
     * Used during create/update to prevent duplicate zone names.
     */
    boolean existsByZoneNameAndIsDeletedFalse(String zoneName);

    /**
     * Search zones with optional filters on name, description, and priority.
     * Supports partial matching on zone name and description.
     * Only returns non-deleted zones (enforced by @Where clause on Zone entity).
     */
    @Query("SELECT z FROM Zone z WHERE " +
           "(:zoneName IS NULL OR LOWER(z.zoneName) LIKE LOWER(CONCAT('%', :zoneName, '%'))) AND " +
           "(:description IS NULL OR LOWER(z.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:priority IS NULL OR z.priority = :priority) AND " +
           "(:prefixInputMode IS NULL OR z.prefixInputMode = :prefixInputMode)")
    Page<Zone> searchZones(@Param("zoneName") String zoneName,
                           @Param("description") String description,
                           @Param("priority") Integer priority,
                           @Param("prefixInputMode") PrefixInputMode prefixInputMode,
                           Pageable pageable);
}
