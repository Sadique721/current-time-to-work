package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ZoneRateMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ZoneRateMapping entity.
 *
 * Used during ROAMING ZoneLookupService warm-up to pre-load zone prefixes
 * into the in-memory ZonePrefixTrie, keyed by ratePackageId.
 */
@Repository
public interface ZoneRateMappingRepository extends JpaRepository<ZoneRateMapping, Long> {

    @Query("SELECT m FROM ZoneRateMapping m " +
           "WHERE m.ratePackageGroup.ratePackageGroupId = :groupId " +
           "AND m.zone.zoneId = :zoneId " +
           "AND m.effectiveFrom <= :callTime " +
           "AND (m.effectiveTo IS NULL OR m.effectiveTo >= :callTime) " +
           "AND m.isDeleted = false")
    Optional<ZoneRateMapping> findActiveByGroupIdAndZoneId(
            @Param("groupId") Long groupId,
            @Param("zoneId") Long zoneId,
            @Param("callTime") LocalDateTime callTime
    );

    /** Find all active ZoneRateMappings for a given RatePackage — used for trie warm-up. */
    @Query("SELECT m FROM ZoneRateMapping m " +
           "WHERE m.ratePackage.ratePackageId = :packageId " +
           "AND m.effectiveFrom <= :callTime " +
           "AND (m.effectiveTo IS NULL OR m.effectiveTo >= :callTime) " +
           "AND m.isDeleted = false")
    List<ZoneRateMapping> findActiveByPackageId(
            @Param("packageId") Long packageId,
            @Param("callTime") LocalDateTime callTime
    );

    /** Find all active ZoneRateMappings for a given RatePackageGroup — kept for legacy use. */
    @Query("SELECT m FROM ZoneRateMapping m " +
           "WHERE m.ratePackageGroup.ratePackageGroupId = :groupId " +
           "AND m.effectiveFrom <= :callTime " +
           "AND (m.effectiveTo IS NULL OR m.effectiveTo >= :callTime) " +
           "AND m.isDeleted = false")
    List<ZoneRateMapping> findActiveByGroupId(
            @Param("groupId") Long groupId,
            @Param("callTime") LocalDateTime callTime
    );

    /** Find all distinct RatePackage IDs that have active ZoneRateMappings — used for trie warm-up. */
    @Query("SELECT DISTINCT m.ratePackage.ratePackageId FROM ZoneRateMapping m " +
           "WHERE m.isDeleted = false")
    List<Long> findAllActivePackageIds();

    @Query("SELECT DISTINCT m.ratePackageGroup.ratePackageGroupId FROM ZoneRateMapping m " +
           "WHERE m.isDeleted = false")
    List<Long> findAllActiveGroupIds();
}
