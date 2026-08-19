package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.RoamingCdr;
import com.xcess.ocs.roaming.entity.RoamingRatingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoamingCdrRepository extends JpaRepository<RoamingCdr, Long> {
    List<RoamingCdr> findByTapFileRecord_TapFileId(Long tapFileId);
    List<RoamingCdr> findByTapFileRecord_TapFileIdAndRatingStatus(Long tapFileId, RoamingRatingStatus status);
    List<RoamingCdr> findByTapFileRecord_TapFileIdAndIsSummarizedFalse(Long tapFileId);
    List<RoamingCdr> findByTapFileRecord_Partner_PartnerId(Long partnerId);
    List<RoamingCdr> findByRatingStatus(RoamingRatingStatus status);
    List<RoamingCdr> findByTapFileRecord_TapFileIdAndServiceType(Long tapFileId, com.xcess.ocs.entity.ServiceType serviceType);
}
