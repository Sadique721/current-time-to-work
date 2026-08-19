package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.RateDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service that provides high-level rate lookup functionality.
 * This is a facade over the RateTrieService, simplifying the rate lookup interface.
 * Supports both INTERCONNECT (calling/called number based) and ROAMING (PLMN based) lookups.
 */
@Service
public class RateLookupService {
    private final RateTrieService rateTrieService;

    public RateLookupService(RateTrieService rateTrieService) {
        this.rateTrieService = rateTrieService;
    }


    /**
     * Find the best matching RateDetails for a given rate package, source, destination, and call time.
     * Used for INTERCONNECT line of business.
     * 
     * @param ratePackageId The rate package ID to search within
     * @param source The source phone number
     * @param destination The destination phone number
     * @param callTime The timestamp of the call
     * @return The best matching RateDetails, or null if no match found
     */
    public RateDetails findBestRate(Long ratePackageId, String source, String destination, LocalDateTime callTime) {
        return rateTrieService.findBestRate(ratePackageId, source, destination, callTime);
    }
    
    /**
     * Find the best matching RateDetails for a given rate package and home PLMN.
     * Used for ROAMING line of business.
     * 
     * @param ratePackageId The rate package ID to search within
     * @param homePlmn The home PLMN code
     * @param callTime The timestamp of the call
     * @return The best matching RateDetails, or null if no match found
     */
    public RateDetails findBestRateByPlmn(Long ratePackageId, String homePlmn, LocalDateTime callTime) {
        return rateTrieService.findBestRateByPlmn(ratePackageId, homePlmn, callTime);
    }
} 