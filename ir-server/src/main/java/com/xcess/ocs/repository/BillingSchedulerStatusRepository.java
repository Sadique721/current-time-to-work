package com.xcess.ocs.repository;

import com.xcess.ocs.entity.BillingSchedulerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingSchedulerStatusRepository extends JpaRepository<BillingSchedulerStatus, Long> {

    Optional<BillingSchedulerStatus> findTopByOrderByCreatedDateDesc();
}
