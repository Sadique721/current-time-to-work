package com.xcess.ocs.repository;

//import com.partner.partner_management_backend.model.Account;

import com.xcess.ocs.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByPartner_PartnerIdAndIsDeletedFalse(Long partnerId);

    boolean existsByProductPlan_ProductPlanIdAndIsDeletedFalse(Long productPlanId);

    List<Account> findByPartnerPartnerId(Long partnerId);

    /**
     * Find accounts with ProductPlan eagerly fetched.
     * Used by CdrRatingIntegrationService.getRatePackage() for ROAMING CDRs.
     */
    @Query("SELECT DISTINCT a FROM Account a " +
           "LEFT JOIN FETCH a.productPlan pp " +
           "WHERE a.partner.partnerId = :partnerId AND a.isDeleted = false")
    List<Account> findByPartnerPartnerIdWithProductPlan(@Param("partnerId") Long partnerId);

    boolean existsByAccountCodeAndIsDeletedFalse(String accountCode);
    
    // Find account by account code
    Optional<Account> findByAccountCodeAndIsDeletedFalse(String accountCode);

    /**
     * Find account by account code with ProductPlan eagerly loaded.
     * Used by CdrRatingIntegrationService.getRatePackage() to avoid LazyInitializationException.
     */
    @Query("SELECT DISTINCT a FROM Account a " +
           "LEFT JOIN FETCH a.productPlan pp " +
           "WHERE a.accountCode = :accountCode AND a.isDeleted = false")
    Optional<Account> findByAccountCodeWithProductPlan(@Param("accountCode") String accountCode);

    @Query("SELECT a FROM Account a " +
            "LEFT JOIN a.partner p " +
            "LEFT JOIN a.productPlan pp " +
            "WHERE " +
            "(:searchTerm IS NULL OR " +
            "LOWER(a.accountCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pp.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:accountType IS NULL OR a.accountType = :accountType) AND " +
            "a.isDeleted = false")
    Page<Account> searchAccounts(
            @Param("searchTerm") String searchTerm,
            @Param("accountType") String accountType,
            Pageable pageable
    );

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a " +
            "WHERE a.accountCode = :accountCode " +
            "AND a.productPlan.productPlanId = :productPlanId " +
            "AND a.isDeleted = false " +
            "AND (:excludeAccountId IS NULL OR a.accountId != :excludeAccountId)")
    boolean existsByAccountCodeAndProductPlanIdAndIsDeletedFalse(
            @Param("accountCode") String accountCode,
            @Param("productPlanId") Long productPlanId,
            @Param("excludeAccountId") Long excludeAccountId);
}
