package com.xcess.ocs.cache;

import com.xcess.ocs.dto.SourceConfigurationDTO;
import com.xcess.ocs.entity.SourceConfiguration;
import com.xcess.ocs.mapper.SourceConfigurationMapper;
import com.xcess.ocs.repository.SourceConfigurationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SourceConfigurationCache {

    private final SourceConfigurationRepository sourceConfigurationRepository;
    private final SourceConfigurationMapper sourceConfigurationMapper;

    //Store source configurations in cache with `topicName` as the key
    private final Map<String, SourceConfigurationDTO> sourceConfigCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void preloadCache() {
        log.info("Preloading source configuration cache...");
        List<SourceConfiguration> configurations = sourceConfigurationRepository.findAll();
        for (SourceConfiguration config : configurations) {
            SourceConfigurationDTO dto = sourceConfigurationMapper.toDTO(config);
            sourceConfigCache.put(config.getTopicName(), dto);
        }
        log.debug("Preloaded {} source configurations into cache", sourceConfigCache.size());
        log.info("{} Source Configurations", sourceConfigCache.size());
    }

    public List<SourceConfigurationDTO> getAllConfigurations() {
        return new ArrayList<>(sourceConfigCache.values());
    }

    public SourceConfigurationDTO getConfigurationByTopicName(String topicName) {
        return sourceConfigCache.get(topicName);
    }

    public void addToCache(SourceConfigurationDTO dto) {
        sourceConfigCache.put(dto.getTopicName(), dto);
    }

    public void updateCache(SourceConfigurationDTO dto) {
        sourceConfigCache.put(dto.getTopicName(), dto);
    }

    public void removeFromCache(String topicName) {
        sourceConfigCache.remove(topicName);
    }

    @Scheduled(fixedDelayString = "${cache.refresh.interval:600000}")
    public void refreshCache() {
        log.info("Refreshing source configuration cache...");
        sourceConfigCache.clear();
        preloadCache();
    }
}