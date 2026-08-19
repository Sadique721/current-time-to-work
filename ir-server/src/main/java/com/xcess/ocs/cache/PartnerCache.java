package com.xcess.ocs.cache;

import com.xcess.ocs.dto.PartnerDTO;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.mapper.PartnerMapper;
import com.xcess.ocs.repository.PartnerRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache for Partner data.
 * Uses ConcurrentHashMap for thread-safe operations.
 * Cache key is partnerName.
 * Auto-refreshes every 10 minutes (configurable via cache.refresh.interval property).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartnerCache {

    private final PartnerRepository partnerRepository;
    private final PartnerMapper partnerMapper;

    /** In-memory cache storing partners with partnerName as key */
    private final Map<String, PartnerDTO> partnerCache = new ConcurrentHashMap<>();

    /**
     * Preloads all partners into cache on application startup.
     * Called after bean initialization via @PostConstruct.
     */
    @PostConstruct
    public void preloadCache() {
        log.info("Preloading partner cache...");
        List<Partner> partners = partnerRepository.findAllWithTapProfile();
        for (Partner partner : partners) {
            PartnerDTO dto = partnerMapper.toDto(partner);
            partnerCache.put(partner.getPartnerName(), dto);
        }
        log.debug("Preloaded {} partners into cache", partnerCache.size());
        log.info("{} Partners", partnerCache.size());
    }

    /**
     * Retrieves all cached partners.
     * @return list of all cached PartnerDTOs
     */
    public List<PartnerDTO> getAllPartners() {
        return new ArrayList<>(partnerCache.values());
    }

    /**
     * Retrieves a partner from cache by name.
     * @param name the partner name to look up
     * @return PartnerDTO or null if not found
     */
    public PartnerDTO getPartnerByName(String name) {
        return partnerCache.get(name);
    }

    /**
     * Adds a partner to cache.
     * Called when creating a new partner or restoring a deleted one.
     * @param dto the partner to add
     */
    public void addToCache(PartnerDTO dto) {
        partnerCache.put(dto.getPartnerName(), dto);
    }

    /**
     * Updates a partner in cache.
     * Called when updating a partner.
     * @param dto the partner to update
     */
    public void updateCache(PartnerDTO dto) {
        partnerCache.put(dto.getPartnerName(), dto);
    }

    /**
     * Removes a partner from cache.
     * Called when deleting a partner.
     * @param name the partner name to remove
     */
    public void removeFromCache(String name) {
        partnerCache.remove(name);
    }

    /**
     * Scheduled task to refresh the cache.
     * Clears and reloads all partners from database.
     * Interval: 10 minutes (600000ms) by default, configurable via cache.refresh.interval property.
     */
    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing partner cache...");
        partnerCache.clear();
        preloadCache();
    }
}