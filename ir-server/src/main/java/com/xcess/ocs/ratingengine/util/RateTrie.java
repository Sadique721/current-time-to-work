package com.xcess.ocs.ratingengine.util;

import com.xcess.ocs.entity.RateDetails;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Trie structure for a single RatePackage, supporting both source-destination and destination-only lookups.
 * Also supports PLMN-based lookups for ROAMING scenarios.
 */
public class RateTrie {
    // Trie for source-destination based rates: sourcePrefix -> (destinationPrefix -> List<RateDetails>)
    private final RadixTrie<RadixTrie<List<RateDetails>>> sourceDestTrie = new RadixTrie<>();
    // Trie for destination-only based rates: destinationPrefix -> List<RateDetails>
    private final RadixTrie<List<RateDetails>> destOnlyTrie = new RadixTrie<>();
    // Trie for PLMN-based rates: plmnCode -> List<RateDetails>
    private final RadixTrie<List<RateDetails>> plmnTrie = new RadixTrie<>();

    /**
     * Insert a RateDetails into the appropriate trie.
     */
    public void insert(RateDetails rateDetails) {
        String destPrefix = rateDetails.getDestinationPrefix();
        String srcPrefix = rateDetails.getSourcePrefix();
        
        if (srcPrefix != null && !srcPrefix.trim().isEmpty()) {
            // Source-Destination based
            RadixTrie<List<RateDetails>> destTrie = sourceDestTrie.searchLongestPrefix(srcPrefix);
            if (destTrie == null) {
                destTrie = new RadixTrie<>();
                sourceDestTrie.insert(srcPrefix, destTrie);
            }
            List<RateDetails> detailsList = destTrie.searchLongestPrefix(destPrefix);
            if (detailsList == null) {
                detailsList = new ArrayList<>();
                destTrie.insert(destPrefix, detailsList);
            }
            detailsList.add(rateDetails);
        } else {
            // Destination-only based — skip ZONE_DESTINATION_BASED records (no destPrefix, only zoneName)
            if (destPrefix != null && !destPrefix.trim().isEmpty() && rateDetails.getZoneName() == null) {
                List<RateDetails> detailsList = destOnlyTrie.searchLongestPrefix(destPrefix);
                if (detailsList == null) {
                    detailsList = new ArrayList<>();
                    destOnlyTrie.insert(destPrefix, detailsList);
                }
                detailsList.add(rateDetails);
            }
        }

        // For ROAMING PLMN-based rates: only insert records that have a destPrefix but no zoneName.
        // ZONE_DESTINATION_BASED records are resolved via ZonePrefixTrie, not plmnTrie.
        if (destPrefix != null && !destPrefix.trim().isEmpty() && rateDetails.getZoneName() == null) {
            List<RateDetails> plmnDetailsList = plmnTrie.searchLongestPrefix(destPrefix);
            if (plmnDetailsList == null) {
                plmnDetailsList = new ArrayList<>();
                plmnTrie.insert(destPrefix, plmnDetailsList);
            }
            plmnDetailsList.add(rateDetails);
        }
    }

    /**
     * Find the best matching RateDetails for the given source, destination, and time.
     * First tries source-destination, then destination-only.
     * Used for INTERCONNECT line of business.
     */
    public RateDetails findBestRate(String source, String destination, LocalDateTime callTime) {
        // 1. Try source-destination
        RadixTrie<List<RateDetails>> destTrie = sourceDestTrie.searchLongestPrefix(source);
        if (destTrie != null) {
            List<RateDetails> detailsList = destTrie.searchLongestPrefix(destination);
            RateDetails match = findValidByTime(detailsList, callTime);
            if (match != null) return match;
        }
        // 2. Try destination-only 
        List<RateDetails> detailsList = destOnlyTrie.searchLongestPrefix(destination);
        return findValidByTime(detailsList, callTime);
    }
    
    /**
     * Find the best matching RateDetails for the given PLMN and time.
     * Used for ROAMING line of business.
     */
    public RateDetails findBestRateByPlmn(String homePlmn, LocalDateTime callTime) {
        if (homePlmn == null || homePlmn.trim().isEmpty()) {
            return null;
        }
        
        // Look up by PLMN code
        List<RateDetails> detailsList = plmnTrie.searchLongestPrefix(homePlmn);
        return findValidByTime(detailsList, callTime);
    }

    /**
     * Helper: Find a RateDetails in the list that is valid for the given time.
     */
    private RateDetails findValidByTime(List<RateDetails> detailsList, LocalDateTime callTime) {
        if (detailsList == null) return null;
        for (RateDetails rd : detailsList) {
            if ((callTime == null || rd.getStartTime() == null || !callTime.isBefore(rd.getStartTime())) &&
                (callTime == null || rd.getEndTime() == null || !callTime.isAfter(rd.getEndTime()))) {
                return rd;
            }
        }
        return null;
    }
} 