package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.BillingSchedulerAuditLogDTO;
import com.xcess.ocs.entity.BillingSchedulerAuditLog;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between BillingSchedulerAuditLog entity and DTO.
 */
@Component
public class BillingSchedulerAuditLogMapper {

    /**
     * Converts a BillingSchedulerAuditLog entity to a DTO.
     *
     * @param entity The entity to convert
     * @return The converted DTO, or null if entity is null
     */
    public BillingSchedulerAuditLogDTO toDto(BillingSchedulerAuditLog entity) {
        if (entity == null) {
            return null;
        }
        BillingSchedulerAuditLogDTO dto = new BillingSchedulerAuditLogDTO();
        dto.setAuditId(entity.getAuditId());
        dto.setSchedulerStatusId(entity.getSchedulerStatusId());
        dto.setEventType(entity.getEventType());
        dto.setEventMessage(entity.getEventMessage());
        dto.setNoOfAgreementsProcessed(entity.getNoOfAgreementsProcessed());
        dto.setNoOfFailedInvoices(entity.getNoOfFailedInvoices());
        dto.setExecutionTimeMs(entity.getExecutionTimeMs());
        dto.setErrorDetails(entity.getErrorDetails());
        dto.setCreatedAt(entity.getCreatedDate());
        return dto;
    }

    /**
     * Converts a BillingSchedulerAuditLogDTO to an entity.
     *
     * @param dto The DTO to convert
     * @return The converted entity, or null if dto is null
     */
    public BillingSchedulerAuditLog toEntity(BillingSchedulerAuditLogDTO dto) {
        if (dto == null) {
            return null;
        }
        BillingSchedulerAuditLog entity = new BillingSchedulerAuditLog();
        entity.setAuditId(dto.getAuditId());
        entity.setSchedulerStatusId(dto.getSchedulerStatusId());
        entity.setEventType(dto.getEventType());
        entity.setEventMessage(dto.getEventMessage());
        entity.setNoOfAgreementsProcessed(dto.getNoOfAgreementsProcessed());
        entity.setNoOfFailedInvoices(dto.getNoOfFailedInvoices());
        entity.setExecutionTimeMs(dto.getExecutionTimeMs());
        entity.setErrorDetails(dto.getErrorDetails());
        return entity;
    }
}
