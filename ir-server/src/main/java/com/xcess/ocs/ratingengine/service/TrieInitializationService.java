package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.RatePackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for initializing the RadixTrie data structures with rate packages.
 * This service is used during cache initialization and refresh operations.
 *
 * Handles two trie types:
 *   1. RateTrie (RadixTrie)    — for INTERCONNECT line of business (prefix-based lookup)
 *   2. ZonePrefixTrie          — for ROAMING line of business (PLMN zone-based lookup)
 *                                ZoneLookupService handles its own warm-up via @PostConstruct.
 *                                This service only triggers INTERCONNECT trie initialization.
 */
@Service
public class TrieInitializationService {
    private final RateTrieService rateTrieService;

    @Autowired
    public TrieInitializationService(RateTrieService rateTrieService) {
        this.rateTrieService = rateTrieService;
    }

    /**
     * Initialize RadixTrie for all INTERCONNECT rate packages.
     * Called at application startup via RatePackageCache and on cache refresh.
     *
     * ROAMING zone tries are initialized separately by ZoneLookupService.warmUpTrieCache()
     * which is triggered automatically via @PostConstruct.
     *
     * @param ratePackages the INTERCONNECT rate packages to build tries for
     */
    public void initialize(List<RatePackage> ratePackages) {
        rateTrieService.initializeTries(ratePackages);
    }
} 