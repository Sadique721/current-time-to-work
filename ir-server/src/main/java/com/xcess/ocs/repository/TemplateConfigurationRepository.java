package com.xcess.ocs.repository;

import com.xcess.ocs.entity.TemplateConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateConfigurationRepository extends JpaRepository<TemplateConfiguration, Long> {

    Optional<TemplateConfiguration> findByTemplateIdAndIsDeletedFalse(Long templateId);

    boolean existsByTemplateNameAndIsDeletedFalse(String templateName);

    boolean existsByTemplateContentHashAndIsDeletedFalse(String templateContentHash);

    @Query("SELECT t FROM TemplateConfiguration t WHERE t.isDeleted = false " +
           "AND (:searchTerm IS NULL OR t.templateName LIKE %:searchTerm% OR t.templateDescription LIKE %:searchTerm%)")
    Page<TemplateConfiguration> searchTemplates(@Param("searchTerm") String searchTerm, Pageable pageable);

    long countByTemplateIdAndIsDeletedFalse(Long templateId);
}