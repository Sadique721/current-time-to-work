package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.RoamingIotRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoamingIotRateRepository extends JpaRepository<RoamingIotRate, Long> {

    @Query("SELECT r FROM RoamingIotRate r WHERE r.partner.partnerId = :partnerId " +
           "AND r.serviceType = :serviceType AND r.isDeleted = false " +
           "AND r.effectiveFrom <= :date AND (r.effectiveTo IS NULL OR r.effectiveTo >= :date) " +
           "ORDER BY LENGTH(r.destinationPrefix) DESC")
    List<RoamingIotRate> findActiveRates(@Param("partnerId") Long partnerId,
                                         @Param("serviceType") ServiceType serviceType,
                                         @Param("date") LocalDate date);
}
