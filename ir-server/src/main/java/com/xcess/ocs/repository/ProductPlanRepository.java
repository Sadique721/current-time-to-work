package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ProductPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductPlanRepository extends JpaRepository<ProductPlan, Long> {
    boolean existsByNameAndIsDeletedFalse(String name);

    @Query("SELECT pp FROM ProductPlan pp WHERE " +
            "(:searchTerm IS NULL OR LOWER(pp.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(pp.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:packageType IS NULL OR pp.packageType = :packageType) AND " +
            "pp.isDeleted = false")
    Page<ProductPlan> searchProductPlans(
            @Param("searchTerm") String searchTerm,
            @Param("packageType") ProductPlan.PackageType packageType,
            Pageable pageable
    );

    List<ProductPlan> findByPackageTypeAndIsDeletedFalse(ProductPlan.PackageType packageType);
}
