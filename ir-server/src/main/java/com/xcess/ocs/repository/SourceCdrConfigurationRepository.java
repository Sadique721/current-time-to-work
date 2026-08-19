package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SourceCdrConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;


@Repository
public interface SourceCdrConfigurationRepository extends JpaRepository<SourceCdrConfiguration, Long> {
    Optional<SourceCdrConfiguration> findBySourceConfiguration_SourceIdAndFieldNameAndIsDeletedFalse(Long sourceId, String fieldName);
    Optional<SourceCdrConfiguration> findBySourceConfiguration_SourceIdAndSourceCdrConfigurationIdAndIsDeletedFalse(Long sourceId, Long id);
    List<SourceCdrConfiguration> findBySourceConfiguration_SourceIdAndIsDeletedFalse(Long sourceId);
}