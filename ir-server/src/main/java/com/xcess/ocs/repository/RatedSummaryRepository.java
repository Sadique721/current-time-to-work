package com.xcess.ocs.repository;

import com.xcess.ocs.entity.RatedSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RatedSummaryRepository extends JpaRepository<RatedSummary, Long> {

    @Query("SELECT rs FROM RatedSummary rs WHERE rs.accountCode = :accountCode " +
            "AND rs.summaryDate >= :startDate AND rs.summaryDate <= :endDate")
    List<RatedSummary> findByAccountCodeAndSummaryDateBetween(
            @Param("accountCode") String accountCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT rs FROM RatedSummary rs WHERE rs.summaryDate = :summaryDate " +
            "AND (:direction IS NULL OR rs.direction = :direction)")
    List<RatedSummary> findBySummaryDateAndDirection(
            @Param("summaryDate") LocalDate summaryDate,
            @Param("direction") String direction);

    @Query("SELECT rs FROM RatedSummary rs WHERE rs.partnerId = :partnerId " +
            "AND rs.summaryDate >= :startDate AND rs.summaryDate <= :endDate")
    List<RatedSummary> findByPartnerIdAndSummaryDateBetween(
            @Param("partnerId") Long partnerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT rs FROM RatedSummary rs WHERE rs.accountCode IN :accountCodes " +
            "AND rs.summaryDate >= :startDate AND rs.summaryDate <= :endDate")
    List<RatedSummary> findByAccountCodesAndDateRange(
            @Param("accountCodes") List<String> accountCodes,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Upsert dedup key.
     * - serviceType separates VOICE / SMS / USAGE rows
     * - ratingType separates SOURCE_DESTINATION_BASED / DESTINATION_BASED / ZONE_DESTINATION_BASED rows
     * - sourcePrefix / destinationPrefix / zoneName are NULL-safe; only one set is populated per ratingType
     * - appliedRate allows multiple rate tiers for the same prefix/zone on the same day
     */
    @Query("SELECT rs FROM RatedSummary rs WHERE rs.summaryDate = :summaryDate " +
            "AND rs.accountCode = :accountCode " +
            "AND rs.direction = :direction " +
            "AND rs.serviceType = :serviceType " +
            "AND rs.ratingType = :ratingType " +
            "AND (rs.sourcePrefix = :sourcePrefix OR (rs.sourcePrefix IS NULL AND :sourcePrefix IS NULL)) " +
            "AND (rs.destinationPrefix = :destinationPrefix OR (rs.destinationPrefix IS NULL AND :destinationPrefix IS NULL)) " +
            "AND (rs.zoneName = :zoneName OR (rs.zoneName IS NULL AND :zoneName IS NULL)) " +
            "AND rs.appliedRate = :appliedRate")
    Optional<RatedSummary> findExistingSummary(
            @Param("summaryDate") LocalDate summaryDate,
            @Param("accountCode") String accountCode,
            @Param("direction") String direction,
            @Param("serviceType") String serviceType,
            @Param("ratingType") String ratingType,
            @Param("sourcePrefix") String sourcePrefix,
            @Param("destinationPrefix") String destinationPrefix,
            @Param("zoneName") String zoneName,
            @Param("appliedRate") BigDecimal appliedRate);

    List<RatedSummary> findBySummaryDate(LocalDate date);
}
