package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.ratingengine.util.RateTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that manages RadixTries for rate lookups.
 * This service maintains a map of rate package IDs to their corresponding RadixTries,
 * providing O(k) lookup performance where k is the prefix length.
 */
@Service
public class RateTrieService {
    private static final Logger log = LoggerFactory.getLogger(RateTrieService.class);
    
    /**
     * Map of rate package IDs to their corresponding RadixTries.
     * This is the core in-memory data structure that enables fast prefix lookups.
     */
    private final Map<Long, RateTrie> rateTrieMap = new ConcurrentHashMap<>();

    /**
     * Initialize tries for all rate packages.
     * This method is called at application startup and during cache refresh events.
     * 
     * @param ratePackages List of rate packages to initialize tries for
     */
    public void initializeTries(List<RatePackage> ratePackages) {
        log.info("Initializing rate tries for {} packages", ratePackages.size());
        rateTrieMap.clear();
        
        for (RatePackage pkg : ratePackages) {
            log.debug("Building trie for package: {} (ID: {})", 
                      pkg.getPackageName(), pkg.getRatePackageId());
            
            RateTrie trie = new RateTrie();
            for (RateDetails details : pkg.getRateDetails()) {
                trie.insert(details);
            }
            
            rateTrieMap.put(pkg.getRatePackageId(), trie);
        }
        
        log.info("Rate tries initialization complete for {} packages", ratePackages.size());
    }

    /**
     * Add or update a single rate package in the Trie.
     * Used for immediate cache synchronization on create/update.
     * 
     * @param ratePackage The rate package to add/update
     */
    public void addOrUpdatePackage(RatePackage ratePackage) {
        if (ratePackage == null || ratePackage.getRatePackageId() == null) {
            log.warn("Cannot add null rate package to Trie");
            return;
        }
        
        log.info("Adding/updating Trie for package: {} (ID: {})", 
                 ratePackage.getPackageName(), ratePackage.getRatePackageId());
        
        RateTrie trie = new RateTrie();
        for (RateDetails details : ratePackage.getRateDetails()) {
            trie.insert(details);
        }
        
        rateTrieMap.put(ratePackage.getRatePackageId(), trie);
        log.debug("Trie updated for package ID: {}", ratePackage.getRatePackageId());
    }

    /**
     * Remove a rate package from the Trie.
     * Used for immediate cache synchronization on delete.
     * 
     * @param ratePackageId The rate package ID to remove
     */
    public void removePackage(Long ratePackageId) {
        if (ratePackageId == null) {
            log.warn("Cannot remove null rate package ID from Trie");
            return;
        }
        
        RateTrie removed = rateTrieMap.remove(ratePackageId);
        if (removed != null) {
            log.info("Removed Trie for package ID: {}", ratePackageId);
        } else {
            log.warn("No Trie found to remove for package ID: {}", ratePackageId);
        }
    }

    /**
     * Find the best rate for a call based on source, destination, and time.
     * This is the primary method used during CDR rating for INTERCONNECT.
     * 
     * @param ratePackageId ID of the rate package to use
     * @param source Source phone number (calling number)
     * @param destination Destination phone number (called number)
     * @param callTime Timestamp when the call occurred
     * @return The best matching RateDetails or null if no match found
     */
    public RateDetails findBestRate(Long ratePackageId, String source, String destination, LocalDateTime callTime) {
        // Add null safety check
        if (ratePackageId == null) {
            log.error("Null ratePackageId provided. Source={}, Destination={}", source, destination);
            return null;
        }
        
        RateTrie trie = rateTrieMap.get(ratePackageId);
        if (trie == null) {
            log.warn("No rate trie found for package ID: {}", ratePackageId);
            return null;
        }
        
        log.debug("Looking up INTERCONNECT rate: package={}, source={}, destination={}, time={}", 
                 ratePackageId, source, destination, callTime);
                
        return trie.findBestRate(source, destination, callTime);
    }
    
    /**
     * Find the best rate for a ROAMING call based on home PLMN and time.
     * This is the primary method used during CDR rating for ROAMING.
     * 
     * @param ratePackageId ID of the rate package to use
     * @param homePlmn Home PLMN code
     * @param callTime Timestamp when the call occurred
     * @return The best matching RateDetails or null if no match found
     */
    public RateDetails findBestRateByPlmn(Long ratePackageId, String homePlmn, LocalDateTime callTime) {
        // Add null safety check
        if (ratePackageId == null) {
            log.error("Null ratePackageId provided for ROAMING lookup. HomePlmn={}", homePlmn);
            return null;
        }
        
        if (homePlmn == null || homePlmn.trim().isEmpty()) {
            log.error("Null or empty homePlmn provided for ROAMING lookup. PackageId={}", ratePackageId);
            return null;
        }
        
        RateTrie trie = rateTrieMap.get(ratePackageId);
        if (trie == null) {
            log.warn("No rate trie found for ROAMING package ID: {}", ratePackageId);
            return null;
        }
        
        log.debug("Looking up ROAMING rate: package={}, homePlmn={}, time={}", 
                 ratePackageId, homePlmn, callTime);
                
        return trie.findBestRateByPlmn(homePlmn, callTime);
    }
    
    /**
     * Check if a rate package is loaded in memory.
     * 
     * @param ratePackageId The rate package ID to check
     * @return true if the package is loaded, false otherwise
     */
    public boolean isPackageLoaded(Long ratePackageId) {
        return ratePackageId != null && rateTrieMap.containsKey(ratePackageId);
    }
    
    /**
     * Get the number of rate packages currently loaded.
     * 
     * @return Number of rate packages loaded
     */
    public int getLoadedPackageCount() {
        return rateTrieMap.size();
    }
}