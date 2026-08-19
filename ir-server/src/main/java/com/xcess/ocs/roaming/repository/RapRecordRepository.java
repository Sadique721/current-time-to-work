package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.RapRecord;
import com.xcess.ocs.roaming.entity.RapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapRecordRepository extends JpaRepository<RapRecord, Long> {
    List<RapRecord> findByStatus(RapStatus status);
    List<RapRecord> findByTapFileRecord_TapFileId(Long tapFileId);
}
