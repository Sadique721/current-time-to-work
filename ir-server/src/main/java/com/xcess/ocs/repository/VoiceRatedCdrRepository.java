package com.xcess.ocs.repository;

import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VoiceRatedCdrRepository extends JpaRepository<VoiceRatedCdr, Long> {

    @Query("SELECT v FROM VoiceRatedCdr v WHERE v.incomingRatingStatus = :status OR v.outgoingRatingStatus = :status")
    Page<VoiceRatedCdr> findByIncomingOrOutgoingRatingStatus(@Param("status") com.xcess.ocs.entity.RatingStatus status, Pageable pageable);

    // ── INTERCONNECT summary — mark queries (split by side) ──────────────────

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VoiceRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markIncomingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE VoiceRatedCdr rc SET rc.isSummarized = true " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    int markOutgoingCdrsAsSummarized(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(rc) FROM VoiceRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false AND rc.isSummarized = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    long countUnsummarizedCdrs(@Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    @Query("SELECT MAX(rc.modifiedDate) FROM VoiceRatedCdr rc " +
            "WHERE rc.modifiedDate >= :startTime " +
            "AND rc.modifiedDate <= :endTime " +
            "AND (rc.incomingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "OR rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED) " +
            "AND rc.isDeleted = false " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT")
    LocalDateTime findLastModifiedDateBetween(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    // ── ROAMING TAP OUT queries ───────────────────────────────────────────────

    @Query("SELECT rc FROM VoiceRatedCdr rc " +
            "WHERE rc.homePlmn = :homePlmn " +
            "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
            "AND rc.ratedAt >= :startTime AND rc.ratedAt <= :endTime " +
            "AND rc.isDeleted = false " +
            "AND rc.isTapOutGenerated = :isTapOutGenerated " +
            "AND rc.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING " +
            "ORDER BY rc.ratedAt ASC")
    List<VoiceRatedCdr> findRatedByHomePlmnAndDateRange(
            @Param("homePlmn") String homePlmn,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("isTapOutGenerated") Boolean isTapOutGenerated
    );

    @Query("SELECT rc FROM VoiceRatedCdr rc " +
           "WHERE rc.tapFileRecord.tapFileId = :tapFileId " +
           "AND rc.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED " +
           "AND rc.isDeleted = false")
    List<VoiceRatedCdr> findRatedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("""
    UPDATE VoiceRatedCdr v
    SET v.isTapOutGenerated = true,
        v.tapFileRecord = :tapFileRecord
    WHERE v.ratedCdrId = :ratedCdrId
    """)
    void markAsTapOutGenerated(
            @Param("ratedCdrId") Long ratedCdrId,
            @Param("tapFileRecord") TapFileRecord tapFileRecord);

    @Query("""
    SELECT v FROM VoiceRatedCdr v
    WHERE v.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND v.isDeleted = false
      AND v.isTapOutGenerated = true
      AND (v.isSummarized = false OR v.isSummarized IS NULL)
      AND v.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
      AND v.ratedAt >= :startTime AND v.ratedAt <= :endTime
    """)
    List<VoiceRatedCdr> findUnsummarizedTapOutCdrs(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("""
    SELECT v FROM VoiceRatedCdr v
    WHERE v.tapFileRecord.tapFileId = :tapFileId
      AND v.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING
      AND v.isDeleted = false
      AND v.isTapOutGenerated = true
      AND (v.isSummarized = false OR v.isSummarized IS NULL)
      AND v.outgoingRatingStatus = com.xcess.ocs.entity.RatingStatus.RATED
    """)
    List<VoiceRatedCdr> findUnsummarizedByTapFileId(@Param("tapFileId") Long tapFileId);

    @Modifying
    @Query("UPDATE VoiceRatedCdr v SET v.isSummarized = true WHERE v.ratedCdrId IN :ids")
    int markAsSummarizedByIds(@Param("ids") List<Long> ids);
}
