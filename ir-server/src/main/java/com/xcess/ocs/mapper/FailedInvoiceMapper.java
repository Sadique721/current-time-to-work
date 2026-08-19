package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.FailedInvoiceDTO;
import com.xcess.ocs.entity.FailedInvoice;
import org.springframework.stereotype.Component;

/**
 * Mapper component for converting between FailedInvoice entity and DTO.
 */
@Component
public class FailedInvoiceMapper {

    public FailedInvoiceDTO toDto(FailedInvoice entity) {
        if (entity == null) {
            return null;
        }
        FailedInvoiceDTO dto = new FailedInvoiceDTO();
        dto.setId(entity.getId());
        dto.setAgreementId(entity.getAgreementId());
        dto.setBillingStartDate(entity.getBillingStartDate());
        dto.setBillingEndDate(entity.getBillingEndDate());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setBillingDate(entity.getBillingDate());
        dto.setCreatedAt(entity.getCreatedDate());
        return dto;
    }

    public FailedInvoice toEntity(FailedInvoiceDTO dto) {
        if (dto == null) {
            return null;
        }
        FailedInvoice entity = new FailedInvoice();
        entity.setId(dto.getId());
        entity.setAgreementId(dto.getAgreementId());
        entity.setBillingStartDate(dto.getBillingStartDate());
        entity.setBillingEndDate(dto.getBillingEndDate());
        entity.setErrorMessage(dto.getErrorMessage());
        entity.setBillingDate(dto.getBillingDate());
        return entity;
    }
}
