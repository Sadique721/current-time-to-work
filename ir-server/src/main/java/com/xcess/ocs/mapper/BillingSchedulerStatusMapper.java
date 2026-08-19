package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.BillingSchedulerStatusDTO;
import com.xcess.ocs.entity.BillingSchedulerStatus;
import org.springframework.stereotype.Component;

@Component
public class BillingSchedulerStatusMapper {

    public BillingSchedulerStatusDTO toDto(BillingSchedulerStatus entity) {
        if (entity == null) {
            return null;
        }
        BillingSchedulerStatusDTO dto = new BillingSchedulerStatusDTO();
        dto.setSchedulerStatusId(entity.getSchedulerStatusId());
        dto.setStatus(entity.getStatus());
        dto.setLastBillingRunTime(entity.getLastBillingRunTime());
        dto.setNextBillingStartDate(entity.getNextBillingStartDate());
        return dto;
    }

    public BillingSchedulerStatus toEntity(BillingSchedulerStatusDTO dto) {
        if (dto == null) {
            return null;
        }
        BillingSchedulerStatus entity = new BillingSchedulerStatus();
        entity.setSchedulerStatusId(dto.getSchedulerStatusId());
        entity.setStatus(dto.getStatus());
        entity.setLastBillingRunTime(dto.getLastBillingRunTime());
        entity.setNextBillingStartDate(dto.getNextBillingStartDate());
        return entity;
    }
}
