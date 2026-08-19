package com.xcess.ocs.repository;

import com.xcess.ocs.entity.RatePackageAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatePackageAssociationRepository extends JpaRepository<RatePackageAssociation, Long> {
    boolean existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(Long ratePackageGroupId);

    boolean existsByRatePackage_RatePackageIdAndIsDeletedFalse(Long ratePackageId);

    @Query("SELECT rpa FROM RatePackageAssociation rpa WHERE rpa.ratePackageGroup.ratePackageGroupId = :groupId AND rpa.isDeleted = false ORDER BY rpa.priority ASC")
    List<RatePackageAssociation> findByGroupIdOrderByPriority(@Param("groupId") Long groupId);

    /**
     * Fetch RatePackageAssociations with RatePackage and its rateDetails eagerly loaded for a given group.
     * Used by CdrRatingIntegrationService.getRatePackage() as the second query
     * (after loading Account → ProductPlan → RatePackageGroup) to avoid Hibernate multiple-bags error.
     * rateDetails is fetched here to prevent LazyInitializationException when accessed outside the session
     * (e.g. from scheduler threads in ErrorRecoveryPollingScheduler / ReRatePollingScheduler).
     */
    @Query("SELECT DISTINCT rpa FROM RatePackageAssociation rpa " +
           "LEFT JOIN FETCH rpa.ratePackage rp " +
           "LEFT JOIN FETCH rp.rateDetails " +
           "WHERE rpa.ratePackageGroup.ratePackageGroupId = :groupId AND rpa.isDeleted = false " +
           "ORDER BY rpa.priority ASC")
    List<RatePackageAssociation> findByGroupIdWithRatePackage(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(rpa) FROM RatePackageAssociation rpa WHERE rpa.ratePackageGroup.ratePackageGroupId = :groupId AND rpa.priority = :priority AND rpa.isDeleted = false AND rpa.id <> :excludeId")
    long countByGroupIdAndPriorityExcludingId(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(rpa) FROM RatePackageAssociation rpa WHERE rpa.ratePackageGroup.ratePackageGroupId = :groupId AND rpa.priority = :priority AND rpa.isDeleted = false AND rpa.id NOT IN :excludeIds")
    long countByGroupIdAndPriorityExcludingIds(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT rpa FROM RatePackageAssociation rpa WHERE rpa.ratePackage.ratePackageId = :packageId AND rpa.isDeleted = false")
    Optional<RatePackageAssociation> findByPackageId(@Param("packageId") Long packageId);

    @Modifying
    @Query("UPDATE RatePackageAssociation r SET r.isDeleted = true, r.deletedAt = :deletedAt WHERE r.id IN :ids")
    void softDeleteAllByIds(@Param("ids") List<Long> ids, @Param("deletedAt") LocalDateTime deletedAt);
}
