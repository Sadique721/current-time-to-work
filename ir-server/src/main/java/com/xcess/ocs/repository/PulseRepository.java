package com.xcess.ocs.repository;


import com.xcess.ocs.entity.Pulse;
import com.xcess.ocs.entity.ServiceType;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PulseRepository extends JpaRepository<Pulse, Long> {
    boolean existsByPulseNameAndIsDeletedFalse(String pulseName);

    @Query("SELECT p FROM Pulse p WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(p.pulseName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.unit) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "CAST(p.noOfUnits AS string) LIKE CONCAT('%', :searchTerm, '%')) AND " +
           "(:serviceType IS NULL OR p.serviceType = :serviceType) AND " +
           "p.isDeleted = false")
    Page<Pulse> searchPulses(
            @Param("searchTerm") String searchTerm,
            @Param("serviceType") ServiceType serviceType,
            Pageable pageable);

     List<Pulse> findByServiceTypeAndIsDeletedFalse(ServiceType serviceType);
}
