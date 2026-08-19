package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.ExchangeRateSchedulerStatusDTO;
import com.xcess.ocs.entity.ExchangeRateSchedulerStatus;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateSchedulerStatusMapper {

    public ExchangeRateSchedulerStatusDTO toDto(ExchangeRateSchedulerStatus entity) {
        if (entity == null) {
            return null;
        }
        ExchangeRateSchedulerStatusDTO dto = new ExchangeRateSchedulerStatusDTO();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setRecordsProcessed(entity.getRecordsProcessed());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setRetryCount(entity.getRetryCount());
        dto.setApiSourceUsed(entity.getApiSourceUsed());
        dto.setCreatedAt(entity.getCreatedDate());
        dto.setModifiedAt(entity.getModifiedDate());
        return dto;
    }
}
