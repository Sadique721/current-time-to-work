package com.xcess.ocs.roaming.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PaginationDetailsDTO;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.roaming.dto.TapFieldMappingDTO;
import com.xcess.ocs.roaming.dto.TapFieldMappingSearchDTO;
import com.xcess.ocs.roaming.entity.TapFieldMapping;
import com.xcess.ocs.roaming.repository.TapFieldMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TapFieldMappingService {

    private final TapFieldMappingRepository fieldMappingRepo;

    public List<TapFieldMappingDTO> getAllFields() {
        return fieldMappingRepo.findAll().stream().map(this::toDTO).toList();
    }

    public List<Map<String, Object>> getFieldDropdown() {
        return fieldMappingRepo.findAll().stream()
                .map(f -> Map.<String, Object>of("id", f.getId(), "fieldName", f.getFieldName()))
                .toList();
    }

    public TapFieldMappingDTO getField(Long id) {
        return toDTO(findOrThrow(id));
    }

    public PageResponseDTO<TapFieldMappingDTO> searchFields(TapFieldMappingSearchDTO criteria, Pageable pageable) {
        Page<TapFieldMapping> page = fieldMappingRepo.search(
                criteria.getCallType(),
                criteria.getFieldName(),
                criteria.getAsnPath(),
                criteria.getDataType(),
                criteria.getIsMandatory(),
                pageable);
        return toPageResponse(page.map(this::toDTO));
    }

    @Transactional
    public TapFieldMappingDTO createField(TapFieldMappingDTO req) {
        if (fieldMappingRepo.existsByFieldNameIgnoreCaseAndCallType(req.getFieldName(), req.getCallType())) {
            throw new DuplicateNameException("TapFieldMapping", req.getFieldName());
        }
        return toDTO(fieldMappingRepo.save(applyDTO(new TapFieldMapping(), req)));
    }

    @Transactional
    public TapFieldMappingDTO updateField(Long id, TapFieldMappingDTO req) {
        TapFieldMapping existing = findOrThrow(id);
        boolean fieldNameChanged = !existing.getFieldName().equalsIgnoreCase(req.getFieldName())
                || existing.getCallType() != req.getCallType();
        if (fieldNameChanged && fieldMappingRepo.existsByFieldNameIgnoreCaseAndCallTypeAndIdNot(
                req.getFieldName(), req.getCallType(), id)) {
            throw new DuplicateNameException("TapFieldMapping", req.getFieldName());
        }
        return toDTO(fieldMappingRepo.save(applyDTO(existing, req)));
    }

    @Transactional
    public void deleteField(Long id) {
        TapFieldMapping field = findOrThrow(id);
        if (fieldMappingRepo.existsActiveProfileMappingByFieldId(id)) {
            throw new ForeignReferenceException(
                    "TapFieldMapping cannot be deleted because it is referenced by an active TAP profile.");
        }
        fieldMappingRepo.delete(field);
    }

    public TapFieldMapping findOrThrow(Long id) {
        return fieldMappingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TapFieldMapping not found: " + id));
    }

    private TapFieldMapping applyDTO(TapFieldMapping entity, TapFieldMappingDTO dto) {
        entity.setCallType(dto.getCallType());
        entity.setFieldName(dto.getFieldName());
        entity.setAsnPath(dto.getAsnPath());
        entity.setDataType(dto.getDataType());
        entity.setOutSourceColumn(dto.getOutSourceColumn());
        entity.setInTargetColumn(dto.getInTargetColumn());
        entity.setDefaultValue(dto.getDefaultValue());
        entity.setIsMandatory(dto.getIsMandatory());
        return entity;
    }

    public TapFieldMappingDTO toDTO(TapFieldMapping e) {
        TapFieldMappingDTO r = new TapFieldMappingDTO();
        r.setId(e.getId());
        r.setCallType(e.getCallType());
        r.setFieldName(e.getFieldName());
        r.setAsnPath(e.getAsnPath());
        r.setDataType(e.getDataType());
        r.setOutSourceColumn(e.getOutSourceColumn());
        r.setInTargetColumn(e.getInTargetColumn());
        r.setDefaultValue(e.getDefaultValue());
        r.setIsMandatory(e.getIsMandatory());
        return r;
    }

    private <T> PageResponseDTO<T> toPageResponse(Page<T> page) {
        PaginationDetailsDTO pagination = new PaginationDetailsDTO();
        pagination.setTotalRecords(page.getTotalElements());
        pagination.setTotalPages(page.getTotalPages());
        pagination.setCurrentPageNumber(page.getNumber() + 1);
        pagination.setTotalRecordsPerPage(page.getSize());
        return new PageResponseDTO<>(pagination, page.getContent());
    }
}
