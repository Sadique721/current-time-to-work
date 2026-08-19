package com.xcess.ocs.repository;

import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface UsageRatedCdrRepository extends JpaRepository<UsageRatedCdr, Long> {

    @Query("SELECT rc FROM UsageRatedCdr rc WHERE rc.incomingRatingStatus = :status OR rc.outgoingRatingStatus = :status")
    Page<UsageRatedCdr> findByIncomingOrOutgoingRatingStatus(@Param("status") com.xcess.ocs.entity.RatingStatus status, Pageable pageable);

    // ── INTERCONNECT summary — mark queries (split by side) ──────────────────

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UsageRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markIncomingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UsageRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markOutgoingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(rc) FROM UsageRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    long countUnsummarizedCdrs(@Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MAX(rc.modifiedDate) FROM UsageRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime " +
            "AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    LocalDateTime findLastModifiedDateBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    // ── ROAMING TAP OUT queries ───────────────────────────────────────────────

    @Query("SELECT rc FROM UsageRatedCdr rc " +
            "WHERE rc.homePlmn = :homePlmn " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.ratedAt >= :startTime AND rc.ratedAt <= :endTime " +
            "AND rc.isDeleted = false " +
            "AND rc.isTapOutGenerated = :isTapOutGenerated " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING " +
            "ORDER BY rc.ratedAt ASC")
    List<UsageRatedCdr> findRatedByHomePlmnAndDateRange(
            @Param("homePlmn") String homePlmn,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("isTapOutGenerated") Boolean isTapOutGenerated
    );

    @Query("SELECT rc FROM UsageRatedCdr rc " +
           "WHERE rc.tapFileRecord.tapFileId = :tapFileId " +
           "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
           "AND rc.isDeleted = false")
    List<UsageRatedCdr> findRatedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("""
    UPDATE UsageRatedCdr rc
    SET rc.isTapOutGenerated = true,
        rc.tapFileRecord = :tapFileRecord
    WHERE rc.ratedCdrId = :ratedCdrId
    """)
    int markAsTapOutGenerated(
            @Param("ratedCdrId") Long ratedCdrId,
            @Param("tapFileRecord") TapFileRecord tapFileRecord);

    @Query("""
    SELECT rc FROM UsageRatedCdr rc
    WHERE rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND rc.isDeleted = false
      AND rc.isTapOutGenerated = true
      AND (rc.isSummarized = false OR rc.isSummarized IS NULL)
      AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
      AND rc.ratedAt >= :startTime AND rc.ratedAt <= :endTime
    """)
    List<UsageRatedCdr> findUnsummarizedTapOutCdrs(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
    SELECT rc FROM UsageRatedCdr rc
    WHERE rc.tapFileRecord.tapFileId = :tapFileId
      AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND rc.isDeleted = false
      AND rc.isTapOutGenerated = true
      AND (rc.isSummarized = false OR rc.isSummarized IS NULL)
      AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
    """)
    List<UsageRatedCdr> findUnsummarizedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("UPDATE UsageRatedCdr rc SET rc.isSummarized = true WHERE rc.ratedCdrId IN :ids")
    int markAsSummarizedByIds(@Param("ids") List<Long> ids);
}
