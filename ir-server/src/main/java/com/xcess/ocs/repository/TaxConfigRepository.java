package com.xcess.ocs.repository;

import com.xcess.ocs.entity.TaxConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, Long> {
    List<TaxConfig> findByIsActiveTrue();

    @Query("SELECT t FROM TaxConfig t WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(t.taxType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.taxName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "t.isDeleted = false")
    Page<TaxConfig> searchTaxConfigs(@Param("searchTerm") String searchTerm, Pageable pageable);
}
