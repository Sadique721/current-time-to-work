package com.xcess.ocs.repository;

import com.xcess.ocs.entity.RateDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateDetailsRepository extends JpaRepository<RateDetails, Long> {

    // For destination-based rate packages
    boolean existsByDestinationPrefixAndRatePackageRatePackageId(String destinationPrefix, Long ratePackageId);

    // For source-destination based rate packages
    boolean existsBySourcePrefixAndDestinationPrefixAndRatePackageRatePackageId(String sourcePrefix,
            String destinationPrefix, Long ratePackageId);

    List<RateDetails> findByRatePackageRatePackageId(Long ratePackageId);

    @Query("SELECT MAX(r.currentVersion) FROM RateDetails r WHERE r.ratePackage.ratePackageId = :ratePackageId")
    Integer findMaxCurrentVersionByRatePackageId(Long ratePackageId);

    // added clearAutomatically = true so Hibernate evicts its first-level cache
    // after the bulk DELETE. Without this, any entities loaded before the delete
    // remain in the persistence context as "live" objects, and subsequent
    // rateDetailsRepository.save() calls or SELECT queries see the stale pre-delete
    // state — causing rate_details: [] on the create response.
    // flushAutomatically = true ensures any pending INSERTs/UPDATEs are flushed
    // to the DB before the bulk DELETE executes, preventing constraint violations.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RateDetails rd WHERE rd.ratePackage.ratePackageId = :ratePackageId")
    void deleteByRatePackageId(@Param("ratePackageId") Long ratePackageId);

    // For zone-based rate packages
    boolean existsByZoneNameAndRatePackageRatePackageId(String zoneName, Long ratePackageId);

    /**
     * Check if any active rate detail references the given country as source.
     * Used to prevent deletion of country that is still in use.
     * 
     * @param countryId the country ID to check
     * @return true if any active rate detail references this country as source
     */
    boolean existsBySourceCountryId_CountryId(Long countryId);

    /**
     * Check if any active rate detail references the given country as destination.
     * Used to prevent deletion of country that is still in use.
     * 
     * @param countryId the country ID to check
     * @return true if any active rate detail references this country as destination
     */
    boolean existsByDestinationCountryId_CountryId(Long countryId);
}