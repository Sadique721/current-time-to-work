package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TapFileRecordRepository extends JpaRepository<TapFileRecord, Long>, JpaSpecificationExecutor<TapFileRecord> {
    Optional<TapFileRecord> findByFileNameAndFileType(String fileName, TapFileType fileType);
    List<TapFileRecord> findByStatus(TapFileStatus status);

    @Query("SELECT t FROM TapFileRecord t WHERE t.partner.partnerId = :partnerId " +
           "AND t.fileType = 'TAP_OUT' " +
           "AND t.processedAt >= :from AND t.processedAt <= :to")
    List<TapFileRecord> findTapOutByPartnerAndDateRange(
            @Param("partnerId") Long partnerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
