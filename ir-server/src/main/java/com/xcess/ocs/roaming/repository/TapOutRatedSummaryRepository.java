package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.TapOutRatedSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TapOutRatedSummaryRepository
        extends JpaRepository<TapOutRatedSummary, Long>,
                JpaSpecificationExecutor<TapOutRatedSummary> {

    /** Fetch the single summary for a specific TAP file (at most one). */
    Optional<TapOutRatedSummary> findByTapFileRecord_TapFileId(Long tapFileId);

    /** Fetch all summaries for a partner within a billing date range (used for roaming invoice generation). */
    List<TapOutRatedSummary> findByPartner_PartnerIdAndSummaryDateBetween(Long partnerId, LocalDate start, LocalDate end);
}
