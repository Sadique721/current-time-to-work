package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ReRateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReRateRequestRepository extends JpaRepository<ReRateRequest, Long> {

    @Query("SELECT r FROM ReRateRequest r WHERE r.status = 'PENDING' OR r.status = 'NEW' AND r.isDelete = false AND r.isActive = true ORDER BY r.requestedAt ASC")
    List<ReRateRequest> findPendingRequests();

    Optional<ReRateRequest> findByRequestIdAndStatus(String requestId, String status);

    Optional<ReRateRequest> findByRequestId(String requestId);

    org.springframework.data.domain.Page<ReRateRequest> findByIsDeleteFalse(org.springframework.data.domain.Pageable pageable);
}
