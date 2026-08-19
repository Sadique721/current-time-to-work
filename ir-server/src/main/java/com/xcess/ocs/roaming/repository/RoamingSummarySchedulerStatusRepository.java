package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.RoamingSummarySchedulerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoamingSummarySchedulerStatusRepository
        extends JpaRepository<RoamingSummarySchedulerStatus, Long> {

    /** Most recent status record — used for crash recovery and lock checks. */
    Optional<RoamingSummarySchedulerStatus> findTopByOrderByCreatedDateDesc();
}
