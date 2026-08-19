package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.PartnerType;
import com.xcess.ocs.entity.Status;
import com.xcess.ocs.roaming.entity.TapProfile;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository interface for Partner entity.
 * Provides methods for CRUD operations and custom queries.
 * Supports soft delete functionality.
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long> {
    
    /**
     * Check if a partner with the given name exists among non-deleted partners.
     * Used for duplicate name validation during create/update.
     */
    boolean existsByPartnerNameAndIsDeletedFalse(String partnerName);
    
    /**
     * Check if a partner with the given code exists among non-deleted partners.
     * Used for duplicate code validation during create/update.
     */
    boolean existsByPartnerCodeAndIsDeletedFalse(String partnerCode);

    /**
     * Count all partners (including deleted) with the given code.
     * Used for unique code generation validation.
     * @return count of all partners with matching code (deleted and non-deleted)
     */
    @Query(value = "SELECT COUNT(*) FROM partners WHERE LOWER(partner_code) = LOWER(:code)", nativeQuery = true)
    long countByPartnerCode(@Param("code") String partnerCode);

    /**
     * Search partners with multiple filter criteria.
     * Only returns non-deleted partners.
     * All parameters are optional and support partial matching.
     */
    @Query("SELECT p FROM Partner p WHERE " +
            "(:partnerName IS NULL OR LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :partnerName, '%'))) AND " +
            "(:partnerCode IS NULL OR LOWER(p.partnerCode) LIKE LOWER(CONCAT('%', :partnerCode, '%'))) AND " +
            "(:partnerType IS NULL OR p.partnerType = :partnerType) AND " +
            "(:status IS NULL OR p.status = :status) AND  " +
            "(:country IS NULL OR LOWER(p.country) LIKE LOWER(CONCAT('%', :country, '%'))) AND " +
            "p.isDeleted = false")
    Page<Partner> searchPartners(
            @Param("partnerName") String partnerName,
            @Param("partnerCode") String partnerCode,
            @Param("partnerType") PartnerType partnerType,
            @Param("status") Status status,
            @Param("country") String country,
            Pageable pageable
    );

    List<Partner> findByPartnerTypeAndIsDeletedFalse(PartnerType partnerType);

    boolean existsByClearingHouse_IdAndIsDeletedFalse(Long clearingHouseId);

    /** Find a roaming partner by their HPLMN code. Used during Kafka CDR roaming rating. */
    java.util.Optional<Partner> findByHplmnAndIsDeletedFalse(String hplmn);

    /**
     * Fetch all partners with tapProfileGroup eagerly loaded via JOIN FETCH.
     * Used by PartnerCache to avoid LazyInitializationException during @PostConstruct.
     */
    @Query("SELECT p FROM Partner p LEFT JOIN FETCH p.tapProfileGroup")
    List<Partner> findAllWithTapProfile();

    /**
     * Fetch all ROAMING partners with tapProfileGroup eagerly loaded.
     * Used by TapOutScheduler and TapFileWatcherScheduler.
     */
    @Query("SELECT DISTINCT p FROM Partner p " +
            "LEFT JOIN FETCH p.tapProfileGroup tpg " +
            "WHERE p.lineOfBusiness = com.xcess.ocs.entity.LineOfBusiness.ROAMING " +
            "AND p.tadigCode IS NOT NULL")
    List<Partner> findRoamingPartnersWithFullProfile();
}