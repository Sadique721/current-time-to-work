package com.xcess.ocs.cache;

import com.xcess.ocs.dto.RatePackageGroupDTO;
import com.xcess.ocs.entity.RatePackageGroup;
import com.xcess.ocs.mapper.RatePackageGroupMapper;
import com.xcess.ocs.repository.RatePackageGroupRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatePackageGroupCache {

    private final RatePackageGroupRepository ratePackageGroupRepository;
    private final Map<Long, RatePackageGroupDTO> cache = new ConcurrentHashMap<>();

    /**
     * Preloads the cache with all rate package groups from the database on startup.
     */

    @PostConstruct
    public void preloadCache() {
//        log.info("Preloading rate package groups into cache...");
        List<RatePackageGroup> ratePackageGroups = ratePackageGroupRepository.findAll();
        for (RatePackageGroup group : ratePackageGroups) {
            RatePackageGroupDTO dto = RatePackageGroupMapper.toDTO(group);
            cache.put(group.getRatePackageGroupId(), dto);
        }
        log.debug("Preloaded {} rate package groups into registry.", cache.size());
        log.info("{} RatePackageGroups", cache.size());
    }

    /**
     * Retrieves a rate package group from the cache.
     */
    public RatePackageGroupDTO getRatePackageGroup(Long id) {
        log.debug("Fetching rate package group with ID: {} from registry", id);
        return cache.get(id);
    }

    /**
     * Returns all rate package groups from the cache.
     */
    public List<RatePackageGroupDTO> getAllRatePackageGroups() {
        log.debug("Fetching all rate package groups from registry");
        return List.copyOf(cache.values());
    }

    /**
     * Adds or updates a rate package group in the cache.
     */
    public void updateCache(RatePackageGroup ratePackageGroup) {
        if (ratePackageGroup != null && ratePackageGroup.getRatePackageGroupId() != null) {
            RatePackageGroupDTO dto = RatePackageGroupMapper.toDTO(ratePackageGroup);
            cache.put(ratePackageGroup.getRatePackageGroupId(), dto);
            log.info("Updated cache with rate package group ID: {}", ratePackageGroup.getRatePackageGroupId());
        } else {
            log.warn("Attempted to update cache with a null or invalid rate package group.");
        }
    }

    /**
     * Removes a rate package group from the cache.
     */
    public void removeFromCache(Long id) {
        if (id != null && cache.containsKey(id)) {
            cache.remove(id);
            log.info("Removed rate package group ID: {} from registry", id);
        } else {
            log.warn("Attempted to remove a non-existent or null rate package group ID: {}", id);
        }
    }

    /**
     * Clears the cache before shutting down the application.
     */
    @PreDestroy
    public void clearCache() {
        log.info("Clearing rate package group cache before shutdown...");
        cache.clear();
        log.info("Rate package group registry cleared.");
    }

    /**
     * Refreshes the cache after 10 minutes.
     */
    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing rate package group cache...");
        cache.clear();
        preloadCache();
    }
}