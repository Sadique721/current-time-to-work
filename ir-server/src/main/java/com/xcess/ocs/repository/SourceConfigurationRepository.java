package com.xcess.ocs.repository;

import com.xcess.ocs.entity.SourceConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface SourceConfigurationRepository extends JpaRepository<SourceConfiguration, Long> {
    List<SourceConfiguration> findByIsDeletedFalse();
    Optional<SourceConfiguration> findBySourceIdAndIsDeletedFalse(Long sourceId);


    Boolean existsByTopicNameAndIsDeletedFalse(String topicName);


    @Query("SELECT s FROM SourceConfiguration s WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(s.topicName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(s.sourceName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:status IS NULL OR LOWER(s.status) = LOWER(:status)) AND " +
            "s.isDeleted = false")
    Page<SourceConfiguration> searchSourceConfigurations(
            @Param("searchTerm") String searchTerm,
            @Param("status") String status,
            Pageable pageable);
}
