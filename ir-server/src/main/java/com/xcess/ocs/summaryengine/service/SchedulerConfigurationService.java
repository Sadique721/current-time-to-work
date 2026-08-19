package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.dto.SchedulerConfigurationDTO;
import com.xcess.ocs.entity.SchedulerConfiguration;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.SchedulerConfigurationRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SchedulerConfigurationService {

    @Autowired
    private SchedulerConfigurationRepository schedulerConfigurationRepository;

    @Autowired
    private SchedulerService schedulerService;

    @Transactional
    public void createOrUpdate(@Valid SchedulerConfigurationDTO schedulerConfigurationDTO) {
        schedulerConfigurationDTO.validate();

        List<SchedulerConfiguration> configs = schedulerConfigurationRepository.findAll();

        SchedulerConfiguration config;
        boolean isNew = false;
        String action;

        if (configs.isEmpty()) {
            config = new SchedulerConfiguration();
            isNew = true;
            action = "CREATED";
            log.info("Creating new scheduler configuration");
        } else {
            config = configs.get(0);
            action = "UPDATED";
            log.info("Updating existing scheduler configuration with id: {}", config.getConfigId());
        }

        config.setStartTimestamp(schedulerConfigurationDTO.getStartTimestamp());
        config.setIntervalType(schedulerConfigurationDTO.getIntervalType());
        config.setIntervalValue(schedulerConfigurationDTO.getIntervalValue());
        config.setTargetedTimestamp(schedulerConfigurationDTO.getTargetedTimestamp());
        config.setTargetedIntervalType(schedulerConfigurationDTO.getTargetedIntervalType());
        config.setTargetedMaxIntervalValue(schedulerConfigurationDTO.getTargetedMaxIntervalValue());
        config.setInterconnectType(schedulerConfigurationDTO.getInterconnectType());
        config.setActive(schedulerConfigurationDTO.isActive());

        schedulerConfigurationRepository.save(config);

        if (isNew) {
            if (schedulerConfigurationDTO.isActive()) {
                schedulerService.createSchedulerStatusIfNotExists(config.getConfigId());
            }
        } else {
            schedulerService.markResetPending();
            log.info("Config updated — reset pending flagged");
        }
    }

    @Transactional
    public void updateActiveStatus(boolean isActive) {
        SchedulerConfiguration config = schedulerConfigurationRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler configuration not found"));

        boolean wasActive = config.isActive();
        config.setActive(isActive);
        schedulerConfigurationRepository.save(config);

        if (isActive && !wasActive) {
            schedulerService.createSchedulerStatusIfNotExists(config.getConfigId());
            log.info("Scheduler activated — status created if not exists");
        }
    }

    public SchedulerConfigurationDTO getConfig() {
        SchedulerConfiguration config = schedulerConfigurationRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Scheduler configuration not found"));

        SchedulerConfigurationDTO dto = new SchedulerConfigurationDTO();
        dto.setStartTimestamp(config.getStartTimestamp());
        dto.setIntervalType(config.getIntervalType());
        dto.setIntervalValue(config.getIntervalValue());
        dto.setTargetedTimestamp(config.getTargetedTimestamp());
        dto.setTargetedIntervalType(config.getTargetedIntervalType());
        dto.setTargetedMaxIntervalValue(config.getTargetedMaxIntervalValue());
        dto.setActive(config.isActive());

        return dto;
    }
}