package com.xcess.ocs.service;

import com.xcess.ocs.cache.SourceConfigurationCache;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.SourceConfigurationDTO;
import com.xcess.ocs.dto.search.SourceConfigurationSearchDTO;
import com.xcess.ocs.entity.SourceConfiguration;
import com.xcess.ocs.entity.SourceCdrConfiguration;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.mapper.SourceConfigurationMapper;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.repository.SourceConfigurationRepository;
import com.xcess.ocs.repository.SourceCdrConfigurationRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Optional Service for refreshing Kafka topic subscriptions.
 */

@Service
@Slf4j
public class SourceConfigurationService {

    @Autowired
    private SourceConfigurationRepository sourceConfigurationRepository;

    @Autowired
    private SourceConfigurationMapper sourceConfigurationMapper;

    @Autowired
    private SourceCdrConfigurationRepository sourceCdrConfigurationRepository;

    @Autowired
    private VoiceRatedCdrRepository voiceRatedCdrRepository;

    @Autowired
    private SourceCdrConfigurationService sourceCdrConfigurationService;

    @Autowired
    private SourceConfigurationCache sourceConfigurationCache;

    public List<SourceConfigurationDTO> getAllConfigurations() {
        log.debug("Retrieving all source configurations");
        List<SourceConfiguration> configurations = sourceConfigurationRepository.findByIsDeletedFalse();
        return configurations.stream()
                .map(sourceConfigurationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PageResponseDTO<SourceConfigurationDTO> getConfigurationsInPagesByPost(Pageable pageable) {
        log.debug("Fetching source configurations in pages");
        Page<SourceConfiguration> configurations = sourceConfigurationRepository.findAll(pageable);
        List<SourceConfigurationDTO> configurationDTOs = configurations.getContent().stream()
                .map(sourceConfigurationMapper::toDTO)
                .toList();

        log.debug("Retrieved {} source configurations in a page", configurationDTOs.size());
        return PaginationUtils.buildGetResponseDTO(configurationDTOs, configurations);
    }

    public PageResponseDTO<SourceConfigurationDTO> searchSourceConfigurations(SourceConfigurationSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching source configurations with criteria: {}", searchDTO);

        Page<SourceConfiguration> configurationsPage = sourceConfigurationRepository.searchSourceConfigurations(
                searchDTO.getSearchTerm(),
                searchDTO.getStatus(),
                pageable
        );

        List<SourceConfigurationDTO> configurations = configurationsPage.getContent().stream()
                .map(sourceConfigurationMapper::toDTO)
                .collect(Collectors.toList());

        log.debug("Found {} source configurations matching criteria", configurations.size());
        return PaginationUtils.buildGetResponseDTO(configurations, configurationsPage);
    }


    public SourceConfigurationDTO getConfigurationById(Long id) {
        log.debug("Retrieving source configuration with ID: {}", id);
        SourceConfiguration configuration = sourceConfigurationRepository.findBySourceIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NoSuchElementException("Source configuration not found with ID: " + id));
        return sourceConfigurationMapper.toDTO(configuration);
    }

    public SourceConfigurationDTO createSourceConfiguration(SourceConfigurationDTO dto) {
        log.debug("Creating new Source Config with topic name: {}", dto.getTopicName());
        if (sourceConfigurationRepository.existsByTopicNameAndIsDeletedFalse(dto.getTopicName())) {
            log.warn("Topic name already exists: {}", dto.getTopicName());
            throw new DuplicateNameException("Topic", dto.getTopicName());
        }

        SourceConfiguration config = new SourceConfiguration();
        // Use the sourceId from DTO if provided
        if (dto.getSourceId() != null) {
            config.setSourceId(dto.getSourceId());
        }
        config.setSourceName(dto.getSourceName());
        config.setTopicName(dto.getTopicName());
        config.setStatus(dto.getStatus().toLowerCase());
        if (dto.getServiceType() != null) {
            config.setServiceType(dto.getServiceType());
        }
        if (dto.getServiceType() != null) {
            config.setServiceType(dto.getServiceType());
        }

        SourceConfiguration finalConfig = sourceConfigurationRepository.save(config);
        sourceCdrConfigurationService.insertDefaultFieldsIfMissing(config.getSourceId());

        sourceConfigurationCache.addToCache(sourceConfigurationMapper.toDTO(finalConfig));

        return sourceConfigurationMapper.toDTO(finalConfig);
    }

    public SourceConfigurationDTO updateSourceConfiguration(Long id, SourceConfigurationDTO dto) {

        SourceConfiguration existing = sourceConfigurationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Source config not found"));

        if (!existing.getTopicName().equalsIgnoreCase(dto.getTopicName()) &&
                sourceConfigurationRepository.existsByTopicNameAndIsDeletedFalse(dto.getTopicName())) {
            log.warn("Topic name already exists: {}", dto.getTopicName());
            throw new DuplicateNameException("Topic", dto.getTopicName());
        }

        if (dto.getSourceName() != null) {
            existing.setSourceName(dto.getSourceName());
        }
        if (dto.getTopicName() != null) {
            existing.setTopicName(dto.getTopicName());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus().toLowerCase());
        }
        if (dto.getServiceType() != null) {
            existing.setServiceType(dto.getServiceType());
        }
        if (dto.getServiceType() != null) {
            existing.setServiceType(dto.getServiceType());
        }

        SourceConfiguration updatedConfig = sourceConfigurationRepository.save(existing);

        sourceConfigurationCache.updateCache(sourceConfigurationMapper.toDTO(updatedConfig));

        return sourceConfigurationMapper.toDTO(updatedConfig);
    }

    @Transactional
    public void deleteSourceConfiguration(Long id) {
        SourceConfiguration existing = sourceConfigurationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Source config not found"));

        String topicName = existing.getTopicName();

        // Fetch all related SourceCDRConfiguration records and soft delete them
        List<SourceCdrConfiguration> relatedConfigs = sourceCdrConfigurationRepository.findBySourceConfiguration_SourceIdAndIsDeletedFalse(id);
        for (SourceCdrConfiguration config : relatedConfigs) {
            config.setDeleted(true);
            sourceCdrConfigurationRepository.save(config);
        }

        sourceConfigurationRepository.deleteById(id);

        sourceConfigurationCache.removeFromCache(topicName);
    }
}