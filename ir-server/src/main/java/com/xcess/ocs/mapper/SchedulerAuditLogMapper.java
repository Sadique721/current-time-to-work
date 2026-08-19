package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.SchedulerAuditLogDTO;
import com.xcess.ocs.entity.SchedulerAuditLog;
import org.springframework.stereotype.Component;

@Component
public class SchedulerAuditLogMapper {

    public SchedulerAuditLogDTO toDto(SchedulerAuditLog entity) {
        if (entity == null) return null;
        SchedulerAuditLogDTO dto = new SchedulerAuditLogDTO();
        dto.setAuditId(entity.getAuditId());
        dto.setConfigId(entity.getConfigId());
        dto.setSchedulerStatusId(entity.getSchedulerStatusId());
        dto.setEventType(entity.getEventType());
        dto.setEventMessage(entity.getEventMessage());
        dto.setCdrStartTime(entity.getCdrStartTime());
        dto.setCdrEndTime(entity.getCdrEndTime());
        dto.setRecordsProcessed(entity.getRecordsProcessed());
        dto.setExecutionTimeMs(entity.getExecutionTimeMs());
        dto.setErrorDetails(entity.getErrorDetails());
        dto.setCreatedAt(entity.getCreatedDate());
        return dto;
    }

    public SchedulerAuditLog toEntity(SchedulerAuditLogDTO dto) {
        if (dto == null) return null;
        SchedulerAuditLog entity = new SchedulerAuditLog();
        entity.setAuditId(dto.getAuditId());
        entity.setConfigId(dto.getConfigId());
        entity.setSchedulerStatusId(dto.getSchedulerStatusId());
        entity.setEventType(dto.getEventType());
        entity.setEventMessage(dto.getEventMessage());
        entity.setCdrStartTime(dto.getCdrStartTime());
        entity.setCdrEndTime(dto.getCdrEndTime());
        entity.setRecordsProcessed(dto.getRecordsProcessed());
        entity.setExecutionTimeMs(dto.getExecutionTimeMs());
        entity.setErrorDetails(dto.getErrorDetails());
        return entity;
    }
}
