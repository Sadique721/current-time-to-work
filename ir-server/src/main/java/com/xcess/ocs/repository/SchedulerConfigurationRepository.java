package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SchedulerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchedulerConfigurationRepository extends JpaRepository<SchedulerConfiguration,Long> {
}
