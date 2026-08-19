package com.xcess.ocs.repository;

import com.xcess.ocs.entity.ClearingHouse;
import com.xcess.ocs.entity.ClearingHouseStatus;
import com.xcess.ocs.entity.ClearingHouseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ClearingHouseRepository extends JpaRepository<ClearingHouse, Long> {

    boolean existsByNameAndIsDeletedFalse(String name);

    Optional<ClearingHouse> findByIdAndIsDeletedFalse(Long id);

    List<ClearingHouse> findByIsDeletedFalse();

    @Query("SELECT ch FROM ClearingHouse ch " +
           "WHERE ch.isDeleted = false " +
           "AND (:name IS NULL OR LOWER(ch.name) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:type IS NULL OR ch.type = :type) " +
           "AND (:status IS NULL OR ch.status = :status)")
    Page<ClearingHouse> search(
            @Param("name") String name,
            @Param("type") ClearingHouseType type,
            @Param("status") ClearingHouseStatus status,
            Pageable pageable);

    @Query("SELECT ch.id AS id, ch.name AS name FROM ClearingHouse ch WHERE ch.isDeleted = false AND ch.status = 'ACTIVE'")
    List<Map<String, Object>> findIdAndNameActive();
}
