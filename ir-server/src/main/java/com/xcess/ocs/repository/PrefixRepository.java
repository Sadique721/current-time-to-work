package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Prefix;
import com.xcess.ocs.entity.PrefixType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Prefix entity.
 * Provides database operations for prefix management.
 *
 * @see Prefix
 */
@Repository
public interface PrefixRepository extends JpaRepository<Prefix, Long> {

    /**
     * Find all prefixes for a specific country by country ID.
     *
     * @param countryId ID of the country
     * @return List of prefixes for the given country
     */
    List<Prefix> findByCountryCountryId(Long countryId);

    /**
     * Check if any prefix exists for a specific country (active records only).
     * Used to check if country is referenced by any prefix.
     *
     * @param countryId the country ID
     * @return true if any active prefix exists for the country
     */
    boolean existsByCountry_CountryIdAndIsDeletedFalse(Long countryId);

    /**
     * Check if a prefix exists for a specific country (active records only).
     * Uses the @Where clause to automatically filter out deleted records.
     *
     * @param prefix the prefix value
     * @param countryId the country ID
     * @return true if an active prefix with this value exists for the country
     */
    boolean existsByPrefixAndCountryCountryIdAndIsDeletedFalse(String prefix, Long countryId);

    /**
     * Check if a prefix name exists for a specific country (active records only).
     * Uses the @Where clause to automatically filter out deleted records.
     *
     * @param prefixName the prefix name
     * @param countryId the country ID
     * @return true if an active prefix with this name exists for the country
     */
    boolean existsByPrefixNameAndCountryCountryIdAndIsDeletedFalse(String prefixName, Long countryId);

    /**
     * Search prefixes by search term matching prefix, prefixName, or country name.
     * Only returns active (non-deleted) prefixes.
     *
     * @param searchTerm the search term (case-insensitive partial match)
     * @param pageable pagination information
     * @return Page of matching prefixes
     */
    @Query("SELECT p FROM Prefix p WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(p.prefix) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.prefixName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.country.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "p.isDeleted = false")
    Page<Prefix> searchPrefixes(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if any active rate detail uses this prefix value for the given country.
     * Used to prevent deletion of prefix referenced by rate details.
     *
     * @param prefix the prefix value
     * @param countryId the country ID
     * @return true if referenced by any rate detail
     */
    @Query("SELECT COUNT(rd) > 0 FROM RateDetails rd WHERE " +
           "((rd.destinationPrefix = :prefix AND rd.destinationCountryId.countryId = :countryId) OR " +
           "(rd.sourcePrefix = :prefix AND rd.sourceCountryId.countryId = :countryId))")
    boolean isReferencedByRateDetails(@Param("prefix") String prefix, @Param("countryId") Long countryId);

    /**
     * Load all active prefixes for the zone prefix dropdown.
     * Optionally filter by prefixType (INTERCONNECT or ROAMING).
     * Also supports optional search term matching prefix value, prefix name, or country name.
     */
    @Query("SELECT p FROM Prefix p WHERE " +
           "(:prefixType IS NULL OR p.prefixType = :prefixType) AND " +
           "(:search IS NULL OR " +
           "LOWER(p.prefix) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.prefixName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.country.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Prefix> findForZoneDropdown(@Param("prefixType") PrefixType prefixType,
                                     @Param("search") String search);

    /**
     * Find all active prefixes by their IDs — used during DROPDOWN mode resolution.
     */
    @Query("SELECT p FROM Prefix p WHERE p.prefixId IN :ids")
    List<Prefix> findAllByIds(@Param("ids") List<Long> ids);
}
