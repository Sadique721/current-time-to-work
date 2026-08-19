package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ExchangeRateSchedulerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRateSchedulerStatusRepository extends JpaRepository<ExchangeRateSchedulerStatus, Long> {

    Optional<ExchangeRateSchedulerStatus> findTopByOrderByCreatedDateDesc();
}
