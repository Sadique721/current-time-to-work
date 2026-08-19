package com.xcess.ocs.cache;

import com.xcess.ocs.dto.SourceCdrConfigurationDTO;
import com.xcess.ocs.entity.SourceCdrConfiguration;
import com.xcess.ocs.mapper.SourceCdrConfigurationMapper;
import com.xcess.ocs.repository.SourceCdrConfigurationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceCdrConfigurationCache {

    private final SourceCdrConfigurationRepository sourceCdrConfigurationRepository;
    private final SourceCdrConfigurationMapper sourceCdrConfigurationMapper;

    //Store CDR configurations in cache with `sourceId` as the key and a List of configurations as value
    private final Map<Long, List<SourceCdrConfigurationDTO>> cdrConfigCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void preloadCache() {
        log.info("Preloading source CDR configuration cache...");
        List<SourceCdrConfiguration> configurations = sourceCdrConfigurationRepository.findAll();

        // Group configurations by sourceId
        Map<Long, List<SourceCdrConfiguration>> groupedConfigs = configurations.stream()
                .collect(Collectors.groupingBy(config -> config.getSourceConfiguration().getSourceId()));

        // Convert and store in cache
        groupedConfigs.forEach((sourceId, configs) -> {
            List<SourceCdrConfigurationDTO> dtos = configs.stream()
                    .map(sourceCdrConfigurationMapper::toDTO)
                    .collect(Collectors.toList());
            cdrConfigCache.put(sourceId, dtos);
        });

        log.debug("Preloaded CDR configurations for {} sources into cache", cdrConfigCache.size());
        log.info("{} Source CDR Configurations", cdrConfigCache.size());
    }

    public List<SourceCdrConfigurationDTO> getConfigurationsBySourceId(Long sourceId) {
        return cdrConfigCache.getOrDefault(sourceId, new ArrayList<>());
    }

    public void addToCache(SourceCdrConfigurationDTO dto) {
        Long sourceId = dto.getSourceId();
        List<SourceCdrConfigurationDTO> configs = cdrConfigCache.getOrDefault(sourceId, new ArrayList<>());
        configs.add(dto);
        cdrConfigCache.put(sourceId, configs);
    }

    public void updateCache(SourceCdrConfigurationDTO dto) {
        Long sourceId = dto.getSourceId();
        List<SourceCdrConfigurationDTO> configs = cdrConfigCache.getOrDefault(sourceId, new ArrayList<>());
        configs.removeIf(config -> config.getId().equals(dto.getId()));
        configs.add(dto);
        cdrConfigCache.put(sourceId, configs);
    }

    public void removeFromCache(Long sourceId, Long cdrId) {
        List<SourceCdrConfigurationDTO> configs = cdrConfigCache.get(sourceId);
        if (configs != null) {
            configs.removeIf(config -> config.getId().equals(cdrId));
            if (configs.isEmpty()) {
                cdrConfigCache.remove(sourceId);
            }
        }
    }

    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing source CDR configuration cache...");
        cdrConfigCache.clear();
        preloadCache();
    }
}