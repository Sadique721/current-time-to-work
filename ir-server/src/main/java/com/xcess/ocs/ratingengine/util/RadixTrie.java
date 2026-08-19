package com.xcess.ocs.ratingengine.util;

import java.util.*;

/**
 * Generic Radix Trie (compressed prefix tree) for fast prefix-based lookups.
 * @param <V> The value type stored at the leaves (e.g., List<RateDetails> or another RadixTrie)
 */
public class RadixTrie<V> {
    private final Map<String, RadixTrie<V>> children = new HashMap<>();
    private V value = null;

    /**
     * Insert a value with the given key (prefix).
     */
    public void insert(String key, V value) {
        insert(key, 0, value);
    }

    private void insert(String key, int index, V value) {
        if (index == key.length()) {
            this.value = value;
            return;
        }
        for (Map.Entry<String, RadixTrie<V>> entry : children.entrySet()) {
            String edge = entry.getKey();
            int matchLen = commonPrefixLength(key, index, edge);
            if (matchLen > 0) {
                if (matchLen == edge.length()) {
                    // Continue down this edge
                    entry.getValue().insert(key, index + matchLen, value);
                } else {
                    // Split the edge
                    String common = edge.substring(0, matchLen);
                    String remainingEdge = edge.substring(matchLen);
                    RadixTrie<V> child = entry.getValue();
                    RadixTrie<V> splitNode = new RadixTrie<>();
                    splitNode.children.put(remainingEdge, child);
                    children.remove(edge);
                    children.put(common, splitNode);
                    splitNode.insert(key, index + matchLen, value);
                }
                return;
            }
        }
        // No matching edge, create new
        String remaining = key.substring(index);
        RadixTrie<V> node = new RadixTrie<>();
        node.insert(key, key.length(), value);
        children.put(remaining, node);
    }

    /**
     * Find the value for the longest prefix match of the given key.
     */
    public V searchLongestPrefix(String key) {
        return searchLongestPrefix(key, 0, null);
    }

    private V searchLongestPrefix(String key, int index, V lastValue) {
        V result = (this.value != null) ? this.value : lastValue;
        for (Map.Entry<String, RadixTrie<V>> entry : children.entrySet()) {
            String edge = entry.getKey();
            if (key.startsWith(edge, index)) {
                return entry.getValue().searchLongestPrefix(key, index + edge.length(), result);
            }
            int matchLen = commonPrefixLength(key, index, edge);
            if (matchLen > 0 && matchLen < edge.length()) {
                // Partial match, but not a full edge
                break;
            }
        }
        return result;
    }

    private int commonPrefixLength(String key, int index, String edge) {
        int max = Math.min(key.length() - index, edge.length());
        for (int i = 0; i < max; i++) {
            if (key.charAt(index + i) != edge.charAt(i)) {
                return i;
            }
        }
        return max;
    }

    /**
     * Get all values stored in the trie (for debugging/testing).
     */
    public List<V> getAllValues() {
        List<V> result = new ArrayList<>();
        if (value != null) result.add(value);
        for (RadixTrie<V> child : children.values()) {
            result.addAll(child.getAllValues());
        }
        return result;
    }
} 