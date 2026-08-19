package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database operations for Organization.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    /**
     * Checks if name exists in active records.
     */
    boolean existsByNameAndIsDeletedFalse(String name);

    /**
     * Checks if legalName exists in active records.
     */
    boolean existsByLegalNameAndIsDeletedFalse(String legalName);

    /**
     * Checks if active partners exist for organization.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Partner p WHERE p.organization.organizationId = :organizationId AND p.isDeleted = false")
    boolean existsActivePartnersByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Searches organizations with optional search term.
     */
    @Query("SELECT o FROM Organization o WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(o.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.suffixName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.legalName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.address) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "o.isDeleted = false")
    Page<Organization> searchOrganizations(@Param("searchTerm") String searchTerm, Pageable pageable);
}
