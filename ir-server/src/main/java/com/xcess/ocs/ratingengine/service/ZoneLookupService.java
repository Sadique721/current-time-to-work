package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.Zone;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.repository.ZoneRepository;
import com.xcess.ocs.ratingengine.util.ZonePrefixTrie;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Zone lookup service for ROAMING line of business rating.
 *
 * Resolves a calledNumber to a zone name using an in-memory ZonePrefixTrie
 * per RatePackage (keyed by ratePackageId).
 *
 * Trie is keyed by ratePackageId because zone prefixes are defined per RatePackage
 * (each RatePackage has its own RateDetails rows with zoneName + prefixPattern).
 *
 * Performance:
 *   Trie lookup: O(k) where k = number length → ~0.01-0.05ms
 *   DB fallback: ~10-50ms (only on cold start or cache miss)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneLookupService {

    private final ZoneRepository zoneRepository;
    private final RatePackageRepository ratePackageRepository;

    /**
     * L1 Cache: one ZonePrefixTrie per RatePackage.
     * Key   = ratePackageId
     * Value = ZonePrefixTrie pre-loaded with all zone prefixes for that package
     */
    private final Map<Long, ZonePrefixTrie> trieCache = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Startup warm-up
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Warm up the in-memory trie cache at application startup.
     *
     * Loads all active ZoneRateMappings, groups them by RatePackageGroup,
     * then for each group builds a ZonePrefixTrie from the associated Zone's prefixPattern.
     *
     * This ensures ZERO cold-start latency for zone lookups during TAP file rating.
     * Called automatically by Spring after bean construction (@PostConstruct).
     */
    @PostConstruct
    @Transactional(readOnly = true)
    public void warmUpTrieCache() {
        try {
            log.info("Starting ZonePrefixTrie cache warm-up for ROAMING rating...");
            long startTime = System.currentTimeMillis();

            // Load all ZONE-type RatePackages with their rateDetails
            List<RatePackage> zonePackages = ratePackageRepository
                    .findByRatePackageTypeWithDetails(RatePackageType.ZONE_DESTINATION_BASED)
                    .orElse(List.of());

            log.info("Found {} ZONE RatePackages for trie warm-up", zonePackages.size());

            int successCount = 0;
            int totalPrefixes = 0;

            for (RatePackage ratePackage : zonePackages) {
                try {
                    int prefixCount = buildTrieForPackage(ratePackage);
                    successCount++;
                    totalPrefixes += prefixCount;
                    log.debug("Initialized ZonePrefixTrie for package: {} with {} prefixes",
                            ratePackage.getRatePackageId(), prefixCount);
                } catch (Exception e) {
                    log.error("Failed to initialize ZonePrefixTrie for package: {}",
                            ratePackage.getRatePackageId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("ZonePrefixTrie warm-up completed in {}ms — packages: {}/{}, total prefixes: {}",
                    duration, successCount, zonePackages.size(), totalPrefixes);

        } catch (Exception e) {
            log.error("ZonePrefixTrie warm-up failed — zone-based rating will fall back to DB", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Primary lookup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolve a calledNumber to a zone name for a given RatePackage.
     *
     * @param ratePackageId the RatePackage ID resolved from RPG callType expression
     * @param calledNumber  the called number from the CDR
     * @return zone name (e.g. "South Asia"), or null if no matching zone found
     */
    public String getZoneForNumber(Long ratePackageId, String calledNumber) {
        long startTime = System.nanoTime();

        if (calledNumber == null || calledNumber.isEmpty()) {
            log.warn("ZoneLookup: null or empty calledNumber for package: {}", ratePackageId);
            return null;
        }

        ZonePrefixTrie trie = trieCache.get(ratePackageId);

        if (trie != null) {
            String zone = trie.search(calledNumber);
            if (zone != null) {
                long duration = System.nanoTime() - startTime;
                log.debug("ZoneLookup L1 HIT: package={}, number={} -> zone={} in {}μs",
                        ratePackageId, truncate(calledNumber), zone, duration / 1000.0);
                return zone;
            }
            log.debug("ZoneLookup L1 MISS: package={}, number={} — no matching zone in trie",
                    ratePackageId, truncate(calledNumber));
            return null;
        }

        log.info("ZoneLookup: trie not cached for package: {} — rebuilding from DB", ratePackageId);
        boolean rebuilt = rebuildTrieFromDb(ratePackageId);

        if (rebuilt) {
            ZonePrefixTrie rebuiltTrie = trieCache.get(ratePackageId);
            if (rebuiltTrie != null) {
                String zone = rebuiltTrie.search(calledNumber);
                if (zone != null) {
                    long duration = System.nanoTime() - startTime;
                    log.info("ZoneLookup DB rebuild HIT: package={}, number={} -> zone={} in {}ms",
                            ratePackageId, truncate(calledNumber), zone, duration / 1_000_000.0);
                    return zone;
                }
            }
        }

        log.warn("ZoneLookup: no zone found for package={}, number={}", ratePackageId, truncate(calledNumber));
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Invalidate and rebuild the trie cache for a specific RatePackageGroup.
     *
     * Call this after admin updates zone mappings via REST API to ensure
     * the in-memory trie reflects the latest DB state.
     *
     * @param groupId the RatePackageGroup ID whose trie should be refreshed
     */
    @Transactional(readOnly = true)
    public void invalidateZoneCache(Long ratePackageId) {
        log.info("Invalidating ZonePrefixTrie cache for package: {}", ratePackageId);
        trieCache.remove(ratePackageId);
        rebuildTrieFromDb(ratePackageId);
        log.info("ZonePrefixTrie cache rebuilt for package: {}", ratePackageId);
    }

    public ZonePrefixTrie.TrieStats getTrieStatistics(Long ratePackageId) {
        ZonePrefixTrie trie = trieCache.get(ratePackageId);
        if (trie != null) return trie.getStats();
        ZonePrefixTrie.TrieStats empty = new ZonePrefixTrie.TrieStats();
        empty.setGroupId(ratePackageId);
        return empty;
    }

    /**
     * Get statistics for all cached groups.
     * Useful for monitoring dashboards.
     */
    public List<ZonePrefixTrie.TrieStats> getAllTrieStatistics() {
        return trieCache.keySet().stream()
                .map(this::getTrieStatistics)
                .collect(Collectors.toList());
    }

    /**
     * Get total estimated memory usage of all cached tries in bytes.
     */
    public long getTotalMemoryUsage() {
        return trieCache.values().stream()
                .mapToLong(ZonePrefixTrie::estimateMemorySize)
                .sum();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rebuild the ZonePrefixTrie for a group from the database.
     * Called on cache miss during getZoneForNumber().
     *
     * @param groupId the RatePackageGroup ID to rebuild
     * @return true if trie was successfully built and cached, false otherwise
     */
    private boolean rebuildTrieFromDb(Long ratePackageId) {
        try {
            RatePackage ratePackage = ratePackageRepository.findByIdWithRateDetails(ratePackageId)
                    .orElse(null);

            if (ratePackage == null || ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
                log.warn("RatePackage {} not found or not ZONE type — cannot rebuild trie", ratePackageId);
                return false;
            }

            int prefixCount = buildTrieForPackage(ratePackage);
            log.info("ZonePrefixTrie rebuilt from DB for package: {} with {} prefixes",
                    ratePackageId, prefixCount);
            return true;

        } catch (Exception e) {
            log.error("Failed to rebuild ZonePrefixTrie from DB for package: {}", ratePackageId, e);
            return false;
        }
    }

    /**
     * Build a ZonePrefixTrie for a ZONE-type RatePackage.
     *
     * For each RateDetails row on the package:
     *   - reads zoneName (e.g. "South Asia")
     *   - looks up the Zone entity by zoneName to get prefixPattern + priority
     *   - inserts each comma-separated prefix into the trie
     *
     * This is the correct source of truth: RateDetails.zoneName links a rate row
     * to a Zone, and Zone.prefixPattern defines which calledNumber prefixes map to it.
     */
    private int buildTrieForPackage(RatePackage ratePackage) {
        ZonePrefixTrie trie = new ZonePrefixTrie(ratePackage.getRatePackageId());
        int prefixCount = 0;

        // Collect distinct zoneNames from this package's rate details
        List<String> zoneNames = ratePackage.getRateDetails().stream()
                .map(RateDetails::getZoneName)
                .filter(z -> z != null && !z.isBlank())
                .distinct()
                .collect(Collectors.toList());

        for (String zoneName : zoneNames) {
            Zone zone = zoneRepository.findByZoneName(zoneName).orElse(null);
            if (zone == null) {
                log.warn("Zone '{}' referenced in RatePackage {} not found in zones table — skipping",
                        zoneName, ratePackage.getRatePackageId());
                continue;
            }
            prefixCount += insertZoneIntoTrie(trie, zone);
        }

        trieCache.put(ratePackage.getRatePackageId(), trie);
        return prefixCount;
    }

    /**
     * Parse Zone.prefixPattern (comma-separated) and insert each prefix into the trie.
     *
     * Zone.prefixPattern example: "23801,23802,23803,26201"
     * Each prefix is trimmed and inserted with the zone's name and length constraints.
     *
     * @param trie the ZonePrefixTrie to insert into
     * @param zone the Zone entity containing prefixPattern and length constraints
     * @return number of prefixes inserted
     */
    private int insertZoneIntoTrie(ZonePrefixTrie trie, Zone zone) {
        if (zone == null || zone.getPrefixPattern() == null || zone.getPrefixPattern().isEmpty()) {
            log.warn("Zone has null or empty prefixPattern: {}", zone != null ? zone.getZoneName() : "null");
            return 0;
        }

        String[] prefixes = zone.getPrefixPattern().split(",");
        int count = 0;
        int priority = zone.getPriority() != null ? zone.getPriority() : 100;

        for (String prefix : prefixes) {
            String trimmed = prefix.trim();
            if (!trimmed.isEmpty()) {
                trie.insert(trimmed, zone.getZoneName(), zone.getDescription(), priority);
                count++;
            }
        }
        return count;
    }

    /** Truncate PLMN for safe logging */
    private String truncate(String plmn) {
        if (plmn == null || plmn.length() <= 4) return plmn;
        return plmn.substring(0, 4) + "**";
    }
}
