package com.xcess.ocs.repository;

import com.xcess.ocs.entity.RateDetailsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateDetailsHistoryRepository extends JpaRepository<RateDetailsHistory, Long> {

    // Fetch all history records for a specific rate detail ID
    List<RateDetailsHistory> findByRateDetailsId(Long rateDetailsId);
}
