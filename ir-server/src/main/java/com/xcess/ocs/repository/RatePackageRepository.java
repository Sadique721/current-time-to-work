package com.xcess.ocs.repository;


import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.ServiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.xcess.ocs.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RatePackageRepository extends JpaRepository<RatePackage, Long> {
    @Query("SELECT p FROM RatePackage p LEFT JOIN FETCH p.rateDetails")
    List<RatePackage> findAll();

    @Query("SELECT p FROM RatePackage p LEFT JOIN FETCH p.rateDetails WHERE p.ratePackageId = :id")
    Optional<RatePackage> findByIdWithRateDetails(@Param("id") Long id);

    @Query("SELECT p FROM RatePackage p LEFT JOIN FETCH p.rateDetails WHERE p.ratePackageType = :type")
    Optional<List<RatePackage>> findByRatePackageTypeWithDetails(@Param("type") RatePackageType type);

    boolean existsByPackageNameAndIsDeletedFalse(String packageName);

    boolean existsByPulse_PulseIdAndIsDeletedFalse(Long pulseId);

    Optional<List<RatePackage>> findByRatePackageType(RatePackageType type);

    @Query("SELECT rp FROM RatePackage rp WHERE " +
            "(:searchTerm IS NULL OR LOWER(rp.packageName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rp.packageDesc) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rp.priceRounding) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rp.rounding) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rp.ratePackageType) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(rp.type) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:serviceType IS NULL OR rp.serviceType = :serviceType) AND " +
            "rp.isDeleted = false")
    Page<RatePackage> searchRatePackages(
            @Param("searchTerm") String searchTerm,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable
    );

    @Query("SELECT p FROM RatePackage p LEFT JOIN FETCH p.rateDetails WHERE p.ratePackageId IN :ids")
    List<RatePackage> findAllByIdWithRateDetails(@Param("ids") List<Long> ids);

    @Query("SELECT rp.ratePackageId as ratePackageId, rp.packageName as packageName " +
            "FROM RatePackage rp WHERE rp.type = :type AND (:serviceType IS NULL OR rp.serviceType = :serviceType) AND rp.isDeleted = false")
    List<Map<String, Object>> findIdAndNameByType(
            @Param("type") Type type,
            @Param("serviceType") ServiceType serviceType);
}