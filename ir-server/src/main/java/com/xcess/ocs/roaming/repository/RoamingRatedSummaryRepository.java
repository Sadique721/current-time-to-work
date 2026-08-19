package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.RoamingRatedSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoamingRatedSummaryRepository extends JpaRepository<RoamingRatedSummary, Long>,
        JpaSpecificationExecutor<RoamingRatedSummary> {
    List<RoamingRatedSummary> findByTapFileRecord_TapFileId(Long tapFileId);
    List<RoamingRatedSummary> findByTapFileRecord_TapFileIdAndIsInvoicedFalse(Long tapFileId);
    List<RoamingRatedSummary> findByPartner_PartnerId(Long partnerId);

    @Query("SELECT s FROM RoamingRatedSummary s WHERE s.tapFileRecord.tapFileId IN :tapFileIds")
    List<RoamingRatedSummary> findByTapFileIds(@Param("tapFileIds") List<Long> tapFileIds);
}
