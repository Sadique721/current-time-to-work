package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SmsRatedCdrRepository extends JpaRepository<SmsRatedCdr, Long> {

    @Query("SELECT rc FROM SmsRatedCdr rc WHERE rc.incomingRatingStatus = :status OR rc.outgoingRatingStatus = :status")
    Page<SmsRatedCdr> findByIncomingOrOutgoingRatingStatus(@Param("status") com.xcess.ocs.entity.RatingStatus status, Pageable pageable);

    // ── INTERCONNECT summary — mark queries (split by side) ──────────────────

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SmsRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markIncomingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SmsRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markOutgoingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(rc) FROM SmsRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    long countUnsummarizedCdrs(@Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MAX(rc.modifiedDate) FROM SmsRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime " +
            "AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    LocalDateTime findLastModifiedDateBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    // ── ROAMING TAP OUT queries ───────────────────────────────────────────────

    @Query("SELECT rc FROM SmsRatedCdr rc " +
            "WHERE rc.homePlmn = :homePlmn " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.ratedAt >= :startTime AND rc.ratedAt <= :endTime " +
            "AND rc.isDeleted = false " +
            "AND rc.isTapOutGenerated = :isTapOutGenerated " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING " +
            "ORDER BY rc.ratedAt ASC")
    List<SmsRatedCdr> findRatedByHomePlmnAndDateRange(
            @Param("homePlmn") String homePlmn,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("isTapOutGenerated") Boolean isTapOutGenerated
    );

    @Query("SELECT rc FROM SmsRatedCdr rc " +
           "WHERE rc.tapFileRecord.tapFileId = :tapFileId " +
           "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
           "AND rc.isDeleted = false")
    List<SmsRatedCdr> findRatedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("""
    UPDATE SmsRatedCdr rc
    SET rc.isTapOutGenerated = true,
        rc.tapFileRecord = :tapFileRecord
    WHERE rc.smsRatedCdrId = :ratedCdrId
    """)
    int markAsTapOutGenerated(
            @Param("ratedCdrId") Long ratedCdrId,
            @Param("tapFileRecord") TapFileRecord tapFileRecord);

    @Query("""
    SELECT rc FROM SmsRatedCdr rc
    WHERE rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND rc.isDeleted = false
      AND rc.isTapOutGenerated = true
      AND (rc.isSummarized = false OR rc.isSummarized IS NULL)
      AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
      AND rc.ratedAt >= :startTime AND rc.ratedAt <= :endTime
    """)
    List<SmsRatedCdr> findUnsummarizedTapOutCdrs(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
    SELECT rc FROM SmsRatedCdr rc
    WHERE rc.tapFileRecord.tapFileId = :tapFileId
      AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND rc.isDeleted = false
      AND rc.isTapOutGenerated = true
      AND (rc.isSummarized = false OR rc.isSummarized IS NULL)
      AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
    """)
    List<SmsRatedCdr> findUnsummarizedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("UPDATE SmsRatedCdr rc SET rc.isSummarized = true WHERE rc.smsRatedCdrId IN :ids")
    int markAsSummarizedByIds(@Param("ids") List<Long> ids);
}
