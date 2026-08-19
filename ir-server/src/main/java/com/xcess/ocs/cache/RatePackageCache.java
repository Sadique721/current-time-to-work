package com.xcess.ocs.cache;

import com.xcess.ocs.dto.RatePackageDTO;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.mapper.RatePackageMapper;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.ratingengine.service.RateTrieService;
import com.xcess.ocs.ratingengine.service.TrieInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RatePackageCache {
    private final RatePackageRepository ratePackageRepository;
    private final TrieInitializationService trieInitializationService;
    private final RateTrieService rateTrieService;

    private final Map<Long, RatePackageDTO> packageCache = new ConcurrentHashMap<>();


    @PostConstruct
    public void preloadCache() {
        List<RatePackage> packages = ratePackageRepository.findAll();
        for (RatePackage ratePackage : packages) {
            log.debug("Loading RatePackage ID: {} with {} RateDetails",
                    ratePackage.getRatePackageId(),
                    ratePackage.getRateDetails().size());
            RatePackageDTO dto = RatePackageMapper.toDTO(ratePackage);
            packageCache.put(ratePackage.getRatePackageId(), dto);
        }
        log.debug("Preloaded {} rate packages into registry", packageCache.size());
        log.info(String.valueOf(packageCache.size())+" "+"RatePackages");
        trieInitializationService.initialize(packages);
    }

    public List<RatePackageDTO> getAllRatePackages() {
        log.debug("Fetching all rate packages from Registry");
        return new ArrayList<>(packageCache.values());
    }

    public RatePackageDTO getRatePackageById(Long id) {
        log.debug("Fetching rate package with ID: {} from Registry", id);
        return packageCache.get(id);
    }

    public void addToCache(RatePackageDTO dto) {
        log.debug("Adding rate package ID: {} to Registry", dto.getRatePackageId());
        packageCache.put(dto.getRatePackageId(), dto);
    }

    public void updateCache(RatePackageDTO dto) {
        log.debug("Updating Registry for rate package ID: {}", dto.getRatePackageId());
        packageCache.put(dto.getRatePackageId(), dto);
    }

    public void removeFromCache(Long id) {
        log.debug("Removing rate package ID: {} from Registry", id);
        packageCache.remove(id);
    }

    public void updateCacheWithTrie(RatePackageDTO dto) {
        log.debug("Updating Registry and Trie for rate package ID: {}", dto.getRatePackageId());
        packageCache.put(dto.getRatePackageId(), dto);
        
        RatePackage ratePackage = ratePackageRepository.findByIdWithRateDetails(dto.getRatePackageId())
                .orElse(null);
        if (ratePackage != null) {
            rateTrieService.addOrUpdatePackage(ratePackage);
        }
    }

    public void removeFromCacheWithTrie(Long id) {
        log.debug("Removing rate package ID: {} from Registry and Trie", id);
        packageCache.remove(id);
        rateTrieService.removePackage(id);
    }

    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing rate package cache...");
        packageCache.clear();
        preloadCache();
    }
}