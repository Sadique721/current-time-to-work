package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SchedulerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchedulerStatusRepository extends JpaRepository<SchedulerStatus, Long> {

    Optional<SchedulerStatus> findTopByOrderByCreatedDateDesc();
}