package com.xcess.ocs.service;

import com.xcess.ocs.cache.SourceCdrConfigurationCache;
import com.xcess.ocs.dto.SourceCdrConfigurationDTO;
import com.xcess.ocs.entity.CdrFieldName;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.SourceCdrConfiguration;
import com.xcess.ocs.entity.SourceConfiguration;
import com.xcess.ocs.mapper.SourceCdrConfigurationMapper;
import com.xcess.ocs.repository.SourceCdrConfigurationRepository;
import com.xcess.ocs.repository.SourceConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class SourceCdrConfigurationService {

    private final SourceCdrConfigurationRepository sourceCdrConfigurationRepository;
    private final SourceConfigurationRepository sourceConfigurationRepository;
    private final SourceCdrConfigurationMapper sourceCdrConfigurationMapper;
    private final SourceCdrConfigurationCache sourceCdrConfigurationCache;

    public List<SourceCdrConfigurationDTO> getConfigurationsBySourceId(Long sourceId) {
        // Verify source exists
        if (!sourceConfigurationRepository.existsById(sourceId)) {
            throw new NoSuchElementException("Source not found with ID: " + sourceId);
        }

        // Retrieve all configurations for the source
        List<SourceCdrConfiguration> configurations = sourceCdrConfigurationRepository
                .findBySourceConfiguration_SourceIdAndIsDeletedFalse(sourceId);

        // Map to DTOs and sort by sequence
        return configurations.stream()
                .map(sourceCdrConfigurationMapper::toDTO)
                .sorted(Comparator.comparing(SourceCdrConfigurationDTO::getSequence))
                .collect(Collectors.toList());
    }

    public SourceCdrConfigurationDTO updateSequence(Long sourceId, Long id, Integer sequence) {
        SourceCdrConfiguration config = sourceCdrConfigurationRepository
                .findBySourceConfiguration_SourceIdAndSourceCdrConfigurationIdAndIsDeletedFalse(sourceId, id)
                .orElseThrow(() -> new NoSuchElementException("Configuration not found for id: " + id));

        // Update the sequence without checking for duplicates in the database
        config.setSequence(sequence);

        SourceCdrConfigurationDTO savedDto = sourceCdrConfigurationMapper.toDTO(sourceCdrConfigurationRepository.save(config));

        sourceCdrConfigurationCache.updateCache(savedDto);

        return savedDto;
    }


    // Add default fields if missing (called internally)
    public void insertDefaultFieldsIfMissing(Long sourceId) {
        SourceConfiguration sourceConfig = sourceConfigurationRepository.findById(sourceId)
                .orElseThrow(() -> new NoSuchElementException("Source not found"));

        Map<CdrFieldName, Integer> defaultSequences;
        if (sourceConfig.getServiceType() == ServiceType.VOICE) {
            defaultSequences = Map.of(
                    CdrFieldName.CALLING_NUMBER, 1,
                    CdrFieldName.CALLED_NUMBER, 2,
                    CdrFieldName.START_TIME, 3,
                    CdrFieldName.END_TIME, 4,
                    CdrFieldName.HOME_PLMN, 5,
                    CdrFieldName.VISITED_PLMN, 6,
                    CdrFieldName.INCOMING_ACCOUNT_ID, 7,
                    CdrFieldName.OUTGOING_ACCOUNT_ID, 8,
                    CdrFieldName.SERVICE_TYPE, 9,
                    CdrFieldName.CALL_TYPE, 10
            );
        } else if (sourceConfig.getServiceType() == ServiceType.SMS) {
            defaultSequences = Map.of(
                    CdrFieldName.CALLING_NUMBER, 1,
                    CdrFieldName.CALLED_NUMBER, 2,
                    CdrFieldName.EVENT_NOS, 3,
                    CdrFieldName.HOME_PLMN, 4,
                    CdrFieldName.VISITED_PLMN, 5,
                    CdrFieldName.INCOMING_ACCOUNT_ID, 6,
                    CdrFieldName.OUTGOING_ACCOUNT_ID, 7,
                    CdrFieldName.SERVICE_TYPE, 8,
                    CdrFieldName.CALL_TYPE, 9
            );
        } else if (sourceConfig.getServiceType() == ServiceType.USAGE) {
            // USAGE
            defaultSequences = Map.ofEntries(
                    Map.entry(CdrFieldName.SUBSCRIBER_IDENTITY, 1),
                    Map.entry(CdrFieldName.ACCESS_POINT_NAME, 2),
                    Map.entry(CdrFieldName.START_TIME, 3),
                    Map.entry(CdrFieldName.END_TIME, 4),
                    Map.entry(CdrFieldName.USAGE, 5),
                    Map.entry(CdrFieldName.UPLOAD_USAGE, 6),
                    Map.entry(CdrFieldName.DOWNLOAD_USAGE, 7),
                    Map.entry(CdrFieldName.MEASURMENT_UNIT, 8),
                    Map.entry(CdrFieldName.HOME_PLMN, 9),
                    Map.entry(CdrFieldName.VISITED_PLMN, 10),
                    Map.entry(CdrFieldName.INCOMING_ACCOUNT_ID, 11),
                    Map.entry(CdrFieldName.OUTGOING_ACCOUNT_ID, 12),
                    Map.entry(CdrFieldName.SERVICE_TYPE, 13)
            );
        }else {
            defaultSequences = Map.of(
                    CdrFieldName.CALLING_NUMBER, 1,
                    CdrFieldName.CALLED_NUMBER, 2,
                    CdrFieldName.USAGE, 3,
                    CdrFieldName.UPLOAD_USAGE, 4,
                    CdrFieldName.DOWNLOAD_USAGE, 5,
                    CdrFieldName.MEASURMENT_UNIT, 6,
                    CdrFieldName.HOME_PLMN, 7,
                    CdrFieldName.VISITED_PLMN, 8,
                    CdrFieldName.INCOMING_ACCOUNT_ID, 9,
                    CdrFieldName.OUTGOING_ACCOUNT_ID, 10
            );
        }

        for (var entry : defaultSequences.entrySet()) {
            sourceCdrConfigurationRepository.findBySourceConfiguration_SourceIdAndFieldNameAndIsDeletedFalse(sourceId, entry.getKey().name())
                    .or(() -> {
                        SourceCdrConfiguration config = new SourceCdrConfiguration();
                        config.setSourceConfiguration(sourceConfig);
                        config.setFieldName(entry.getKey().name());
                        config.setSequence(entry.getValue());
                        SourceCdrConfiguration savedConfig = sourceCdrConfigurationRepository.save(config);
                        sourceCdrConfigurationCache.addToCache(sourceCdrConfigurationMapper.toDTO(savedConfig));
                        return Optional.of(savedConfig);
                    });
        }
    }
}
