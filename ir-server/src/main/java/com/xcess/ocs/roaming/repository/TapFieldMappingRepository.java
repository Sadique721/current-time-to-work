package com.xcess.ocs.roaming.repository;

import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.TapDataType;
import com.xcess.ocs.roaming.entity.TapFieldMapping;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link TapFieldMapping}.
 *
 * <p>Provides standard CRUD and paginated search for the master TAP field mapping dictionary.
 * Soft-delete filtering ({@code is_deleted = false}) is applied automatically
 * via the {@code @Where} annotation on the entity.
 */
@Hidden
@Repository
public interface TapFieldMappingRepository extends JpaRepository<TapFieldMapping, Long> {

    /**
     * Paginated search with optional filters on callType, fieldName, asnPath,
     * dataType, and isMandatory. All parameters are optional (null = no filter).
     */
    boolean existsByFieldNameIgnoreCaseAndCallType(String fieldName, com.xcess.ocs.roaming.entity.CallType callType);

    boolean existsByFieldNameIgnoreCaseAndCallTypeAndIdNot(String fieldName, com.xcess.ocs.roaming.entity.CallType callType, Long id);

    @Query("SELECT COUNT(m) > 0 FROM TapProfileFieldMapping m WHERE m.tapFieldMapping.id = :fieldId AND m.isDeleted = false")
    boolean existsActiveProfileMappingByFieldId(@Param("fieldId") Long fieldId);

    @Query("SELECT f FROM TapFieldMapping f WHERE " +
           "(:callType IS NULL OR f.callType = :callType) AND " +
           "(:fieldName IS NULL OR LOWER(f.fieldName) LIKE LOWER(CONCAT('%', :fieldName, '%'))) AND " +
           "(:asnPath IS NULL OR LOWER(f.asnPath) LIKE LOWER(CONCAT('%', :asnPath, '%'))) AND " +
           "(:dataType IS NULL OR f.dataType = :dataType) AND " +
           "(:isMandatory IS NULL OR f.isMandatory = :isMandatory)")
    Page<TapFieldMapping> search(
            @Param("callType")   CallType callType,
            @Param("fieldName")  String fieldName,
            @Param("asnPath")    String asnPath,
            @Param("dataType")   TapDataType dataType,
            @Param("isMandatory") Boolean isMandatory,
            Pageable pageable);
}
