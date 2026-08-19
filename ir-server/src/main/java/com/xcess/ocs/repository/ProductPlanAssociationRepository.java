package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ProductPlanAssociation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductPlanAssociationRepository extends JpaRepository<ProductPlanAssociation, Long> {
    boolean existsByProductPlan_ProductPlanIdAndIsDeletedFalse(Long productPlanId);

    boolean existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(Long ratePackageGroupId);

    @Query("SELECT pa FROM ProductPlanAssociation pa WHERE pa.productPlan.productPlanId = :productPlanId AND pa.isDeleted = false")
    List<ProductPlanAssociation> findByProductPlanId(@Param("productPlanId") Long productPlanId);

    @Query("SELECT pa FROM ProductPlanAssociation pa LEFT JOIN FETCH pa.ratePackageGroup WHERE pa.productPlan.productPlanId = :productPlanId AND pa.isDeleted = false")
    List<ProductPlanAssociation> findByProductPlanIdWithGroup(@Param("productPlanId") Long productPlanId);

    @Modifying
    @Query("UPDATE ProductPlanAssociation pa SET pa.isDeleted = true, pa.deletedAt = :deletedAt WHERE pa.id IN :ids")
    void softDeleteAllByIds(@Param("ids") List<Long> ids, @Param("deletedAt") LocalDateTime deletedAt);
}
