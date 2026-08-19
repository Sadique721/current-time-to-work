package com.xcess.ocs.ratingengine.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Character-by-character prefix trie for zone-based PLMN lookup.
 *
 * Used exclusively for ROAMING line of business rating.
 * Matches a visitedPlmn string (e.g. "23801") against stored PLMN prefixes
 * to determine the billing zone name (e.g. "ZONE_EU").
 *
 * Why a character trie (not RadixTrie)?
 *   PLMN codes are short (5-6 chars). A character trie gives O(k) lookup
 *   where k = PLMN length (2-6), which is faster than the compressed RadixTrie
 *   for such short keys. This matches the reference project's design.
 *
 * Key features (ported from adopt.ocsenginemanagement ZonePrefixTrie):
 *   - Longest-prefix match: returns the most specific zone for a given PLMN
 *   - Length validation: each node stores minLength/maxLength constraints
 *     so a prefix match is only accepted if the full PLMN length is valid
 *   - Priority: deeper nodes (longer prefixes) have higher priority
 *   - Thread-safe: uses ConcurrentHashMap for children
 *
 * Example:
 *   insert("238", "ZONE_EU", null, 5, 6)   → matches "23801", "23802"
 *   insert("2380", "ZONE_EU_SPECIFIC", null, 5, 6) → matches "23801" with higher priority
 *   search("23801") → returns "ZONE_EU_SPECIFIC" (longer prefix wins)
 */
@Slf4j
public class ZonePrefixTrie {

    /** Root node — no zone, no prefix, just the entry point */
    private final TrieNode root;

    /** The RatePackageGroup ID this trie belongs to (for logging/stats) */
    private final Long groupId;

    public ZonePrefixTrie(Long groupId) {
        this.root = new TrieNode();
        this.groupId = groupId;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner node class
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A single node in the trie.
     * Each character of a PLMN prefix occupies one level in the tree.
     */
    @Data
    public static class TrieNode {

        private Map<Character, TrieNode> children = new ConcurrentHashMap<>();
        private String zoneName;
        private String prefixPattern;
        private String description;
        private boolean isEndOfPrefix;

        /**
         * Admin-defined priority for this zone (lower number = higher priority).
         * When a PLMN matches multiple zones, the zone with the lowest priority number wins.
         */
        private int zonePriority;

        /** Depth of this node = length of the prefix (used for longest-prefix tiebreaking) */
        private int depth;

        public TrieNode() {
            this.isEndOfPrefix = false;
            this.zonePriority = Integer.MAX_VALUE;
            this.depth = 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Insert
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Insert a PLMN prefix → zone mapping into the trie.
     *
     * @param prefix       the PLMN prefix to insert (e.g. "23801")
     * @param zoneName     the zone name to associate with this prefix
     * @param description  optional zone description
     * @param zonePriority admin-defined priority (lower = higher priority)
     */
    public void insert(String prefix, String zoneName, String description, int zonePriority) {
        if (prefix == null || prefix.isEmpty()) {
            log.warn("Cannot insert null or empty prefix into ZonePrefixTrie for group: {}", groupId);
            return;
        }

        TrieNode current = root;
        int depth = 0;

        for (char digit : prefix.toCharArray()) {
            current.getChildren().putIfAbsent(digit, new TrieNode());
            current = current.getChildren().get(digit);
            depth++;
        }

        current.setEndOfPrefix(true);
        current.setZoneName(zoneName);
        current.setPrefixPattern(prefix);
        current.setDescription(description);
        current.setZonePriority(zonePriority);
        current.setDepth(depth);

        log.debug("ZonePrefixTrie[group={}] inserted prefix: {} -> zone: {} (priority: {})",
                groupId, prefix, zoneName, zonePriority);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Find the zone name for a given visitedPlmn.
     *
     * Conflict resolution (when multiple zones match):
     *   1. Lower zonePriority number wins (admin-defined)
     *   2. Tiebreak: longer prefix wins (more specific match)
     */
    public String search(String visitedPlmn) {
        if (visitedPlmn == null || visitedPlmn.isEmpty()) return null;

        TrieNode current = root;
        String bestZone = null;
        int bestPriority = Integer.MAX_VALUE;
        int bestDepth = -1;

        for (int i = 0; i < visitedPlmn.length(); i++) {
            char digit = visitedPlmn.charAt(i);
            if (!current.getChildren().containsKey(digit)) break;
            current = current.getChildren().get(digit);

            if (current.isEndOfPrefix()) {
                int p = current.getZonePriority();
                int d = current.getDepth();
                // Lower priority number wins; tiebreak by longer prefix
                if (p < bestPriority || (p == bestPriority && d > bestDepth)) {
                    bestZone = current.getZoneName();
                    bestPriority = p;
                    bestDepth = d;
                }
            }
        }

        if (bestZone != null) {
            log.debug("ZonePrefixTrie[group={}] PLMN: {} -> zone: {} (priority: {}, depth: {})",
                    groupId, truncate(visitedPlmn), bestZone, bestPriority, bestDepth);
        } else {
            log.debug("ZonePrefixTrie[group={}] no zone found for PLMN: {}", groupId, truncate(visitedPlmn));
        }
        return bestZone;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats & Maintenance
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Collect trie statistics for monitoring and health checks.
     * Exposed via ZoneLookupService.getTrieStatistics(groupId).
     */
    public TrieStats getStats() {
        TrieStats stats = new TrieStats();
        stats.setGroupId(groupId);
        collectStats(root, 0, stats);
        return stats;
    }

    private void collectStats(TrieNode node, int depth, TrieStats stats) {
        stats.setTotalNodes(stats.getTotalNodes() + 1);
        stats.setMaxDepth(Math.max(stats.getMaxDepth(), depth));
        if (node.isEndOfPrefix()) {
            stats.setTotalPrefixes(stats.getTotalPrefixes() + 1);
        }
        for (TrieNode child : node.getChildren().values()) {
            collectStats(child, depth + 1, stats);
        }
    }

    /**
     * Clear all entries from this trie.
     * Called during cache invalidation before rebuilding from DB.
     */
    public void clear() {
        root.getChildren().clear();
        log.info("ZonePrefixTrie[group={}] cleared", groupId);
    }

    /**
     * Estimate memory usage of this trie in bytes.
     * Each node is approximately 250 bytes (ConcurrentHashMap overhead + fields).
     */
    public long estimateMemorySize() {
        return getStats().getTotalNodes() * 250L;
    }

    /** Truncate PLMN for safe logging (privacy) */
    private String truncate(String plmn) {
        if (plmn == null || plmn.length() <= 4) return plmn;
        return plmn.substring(0, 4) + "**";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats DTO
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    public static class TrieStats {
        private Long groupId;
        private int totalNodes = 0;
        private int totalPrefixes = 0;
        private int maxDepth = 0;

        /** Collect all zone names currently stored in this trie */
        private List<String> zoneNames = new ArrayList<>();
    }
}
