package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ErrorRateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ErrorRateRequestRepository extends JpaRepository<ErrorRateRequest, Long> {

    @Query("SELECT e FROM ErrorRateRequest e WHERE e.status = 'PENDING' OR e.status = 'NEW' AND e.isDelete = false AND e.isActive = true ORDER BY e.requestedAt ASC")
    List<ErrorRateRequest> findPendingRequests();

    Optional<ErrorRateRequest> findByRequestIdAndStatus(String requestId, String status);

    org.springframework.data.domain.Page<ErrorRateRequest> findByIsDeleteFalse(org.springframework.data.domain.Pageable pageable);
}
