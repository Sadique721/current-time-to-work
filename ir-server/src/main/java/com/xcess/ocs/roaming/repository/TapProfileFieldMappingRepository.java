package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.TapProfileFieldMapping;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TapProfileFieldMapping}.
 *
 * <p>Manages the intersection records that link TAP profiles to master field
 * mappings with optional per-profile overrides.
 * Soft-delete filtering ({@code is_deleted = false}) is applied automatically
 * via the {@code @Where} annotation on the entity.
 */
@Hidden
@Repository
public interface TapProfileFieldMappingRepository extends JpaRepository<TapProfileFieldMapping, Long> {

    @Modifying
    @Query(value = "UPDATE tap_profile_field_mappings SET is_deleted = true, deleted_at = NOW() WHERE profile_id = :profileId", nativeQuery = true)
    void deleteAllByProfileId(@Param("profileId") Long profileId);
}
