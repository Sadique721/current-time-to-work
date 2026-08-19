package com.xcess.ocs.repository;

import com.xcess.ocs.entity.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Country entity.
 * Provides database operations for country management.
 *
 * @see Country
 */
@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    /**
     * Find country by exact name.
     * Uses the @Where clause to automatically filter out deleted records.
     *
     * @param name the country name to search
     * @return Optional containing the country if found (active record only)
     */
    Optional<Country> findByName(String name);

    /**
     * Check if an active (non-deleted) country with the given name exists.
     * Uses the @Where clause to automatically filter out deleted records.
     *
     * @param name the country name to check
     * @return true if an active country with this name exists, false otherwise
     */
    boolean existsByNameAndIsDeletedFalse(String name);

    /**
     * Search countries by search term matching name or country code.
     * Only returns active (non-deleted) countries.
     *
     * @param searchTerm the search term (case-insensitive partial match)
     * @param pageable pagination information
     * @return Page of matching countries
     */
    @Query("SELECT c FROM Country c WHERE " +
           "(:searchTerm IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.countryCode) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "c.isDeleted = false")
    Page<Country> searchCountries(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Check if an active (non-deleted) country with the given ISO code exists.
     *
     * @param isoCode the ISO 3166 alpha-2 code to check
     * @return true if an active country with this ISO code exists, false otherwise
     */
    boolean existsByIsoCodeAndIsDeletedFalse(String isoCode);

    /**
     * Find active countries by ISO code (should return at most one, but returns List for consistency).
     *
     * @param isoCode the ISO 3166 alpha-2 code to search
     * @return List of matching active countries
     */
    List<Country> findByIsoCodeAndIsDeletedFalse(String isoCode);

    /**
     * Find distinct currency codes for all active (non-deleted) countries.
     * Uses the @Where clause to automatically filter out deleted records.
     *
     * @return List of unique currency codes
     */
    @Query("SELECT DISTINCT c.currencyCode FROM Country c WHERE c.currencyCode IS NOT NULL AND c.currencyCode <> ''")
    List<String> findDistinctCurrencyCodes();
}
