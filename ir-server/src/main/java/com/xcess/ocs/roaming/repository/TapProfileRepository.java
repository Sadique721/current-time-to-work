package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.TapProfile;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Hidden
@Repository
public interface TapProfileRepository extends JpaRepository<TapProfile, Long> {

    Optional<TapProfile> findByProfileName(String profileName);

    @Query("SELECT p FROM TapProfile p LEFT JOIN FETCH p.fieldMappings fm LEFT JOIN FETCH fm.tapFieldMapping WHERE p.id = :profileId")
    Optional<TapProfile> findByIdWithFieldMappings(@Param("profileId") Long profileId);

    boolean existsByProfileNameIgnoreCaseAndIsDeletedFalse(String profileName);

    @Query("SELECT COUNT(g) > 0 FROM TapProfileGroup g JOIN g.tapProfiles p WHERE p.id = :profileId AND g.isDeleted = false")
    boolean existsActiveGroupByProfileId(@Param("profileId") Long profileId);

    @Query("SELECT p FROM TapProfile p WHERE " +
           "(:profileName IS NULL OR LOWER(p.profileName) LIKE LOWER(CONCAT('%', :profileName, '%'))) AND " +
           "(:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%', :description, '%'))) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<TapProfile> search(
            @Param("profileName")  String profileName,
            @Param("description")  String description,
            @Param("isActive")     Boolean isActive,
            Pageable pageable);
}
