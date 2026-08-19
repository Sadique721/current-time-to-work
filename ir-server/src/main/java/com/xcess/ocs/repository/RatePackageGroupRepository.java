package com.xcess.ocs.repository;


import com.xcess.ocs.entity.RatePackageGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface RatePackageGroupRepository extends JpaRepository<RatePackageGroup, Long> {
    boolean existsByNameAndIsDeletedFalse(
            @NotBlank(message = "Name is required")
            @Size(max = 100, message = "Name must be less than 100 characters")
            String name
    );

    @EntityGraph(attributePaths = {"ratePackageAssociations", "ratePackageAssociations.ratePackage"})
    @Query("SELECT g FROM RatePackageGroup g LEFT JOIN FETCH g.ratePackageAssociations a LEFT JOIN FETCH a.ratePackage")
    List<RatePackageGroup> findAll();

    @Query("SELECT rpg FROM RatePackageGroup rpg WHERE " +
            "(:searchTerm IS NULL OR LOWER(rpg.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rpg.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:packageType IS NULL OR rpg.packageType = :packageType) AND " +
            "rpg.isDeleted = false")
    Page<RatePackageGroup> searchRatePackageGroups(
            @Param("searchTerm") String searchTerm,
            @Param("packageType") RatePackageGroup.PackageType packageType,
            Pageable pageable
    );

    @Query("SELECT rp.ratePackageGroupId as ratePackageGroupId, rp.name as ratePackageGroupName, rp.serviceType as serviceType " +
            "FROM RatePackageGroup rp WHERE rp.packageType = :type AND rp.isDeleted = false")
    List<Map<String, Object>> findIdAndNameByType(
            @Param("type") RatePackageGroup.PackageType type);
}