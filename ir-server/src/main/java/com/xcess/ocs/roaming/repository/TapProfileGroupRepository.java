package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Hidden
@Repository
public interface TapProfileGroupRepository extends JpaRepository<TapProfileGroup, Long> {

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    @Query("SELECT COUNT(p) > 0 FROM Partner p WHERE p.tapProfileGroup.id = :groupId AND p.isDeleted = false")
    boolean existsActivePartnerByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT g FROM TapProfileGroup g WHERE " +
           "(:name IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:description IS NULL OR LOWER(g.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:isActive IS NULL OR g.isActive = :isActive)")
    Page<TapProfileGroup> search(
            @Param("name") String name,
            @Param("description") String description,
            @Param("isActive") Boolean isActive,
            Pageable pageable);
}
