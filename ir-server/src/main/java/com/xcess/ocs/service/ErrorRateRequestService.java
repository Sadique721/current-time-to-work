package com.xcess.ocs.service;

import com.xcess.ocs.dto.CdrQueryConfigDTO;
import com.xcess.ocs.dto.ErrorRateRequestDTO;
import com.xcess.ocs.entity.CdrQueryConfig;
import com.xcess.ocs.entity.ErrorRateRequest;
import com.xcess.ocs.repository.ErrorRateRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorRateRequestService {

    private final ErrorRateRequestRepository errorRateRequestRepository;
    private final CdrQueryConfigService cdrQueryConfigService;
    String errorRateRequest = "ErrorRequest";

    @Transactional
    public void editErrorRateStatus(String requestId, String status) {
        errorRateRequestRepository.findByRequestIdAndStatus(requestId, "PROCESSING").ifPresentOrElse(
                request -> {
                    request.setStatus(status);
                    errorRateRequestRepository.save(request);
                    log.info("Updated ErrorRateRequest {} status to {}", requestId, status);
                },
                () -> log.warn("ErrorRateRequest {} not found or not in PROCESSING state to update status to {}", requestId, status)
        );
    }

    @Transactional
    public ErrorRateRequestDTO createErrorRateRequest(ErrorRateRequestDTO dto) {

        ErrorRateRequest entity = new ErrorRateRequest();
        entity.setRequestId(dto.getRequestId() != null ? dto.getRequestId() : "ERR-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setRequestName(dto.getRequestName());
        entity.setRequestParameters(dto.getRequestParameters());
        entity.setVoiceQueryConfig(resolveQueryConfig(dto.getVoiceQueryConfig(), errorRateRequest));
        entity.setSmsQueryConfig(resolveQueryConfig(dto.getSmsQueryConfig(), errorRateRequest));
        entity.setUsageQueryConfig(resolveQueryConfig(dto.getUsageQueryConfig(), errorRateRequest));
        entity.setStatus("NEW");
        entity.setEnable(dto.getEnable() != null ? dto.getEnable() : true);
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setRequestedAt(LocalDateTime.now());
        entity.setIsActive(true);
        entity.setIsDelete(false);
        return toDto(errorRateRequestRepository.save(entity));
    }

    @Transactional
    public ErrorRateRequestDTO updateErrorRateRequest(ErrorRateRequestDTO dto, Long id) {
        ErrorRateRequest entity = errorRateRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ErrorRateRequest not found: " + id));
        entity.setRequestName(dto.getRequestName());
        entity.setRequestParameters(dto.getRequestParameters());
        entity.setVoiceQueryConfig(resolveQueryConfig(dto.getVoiceQueryConfig(), errorRateRequest));
        entity.setSmsQueryConfig(resolveQueryConfig(dto.getSmsQueryConfig(), errorRateRequest));
        entity.setUsageQueryConfig(resolveQueryConfig(dto.getUsageQueryConfig(), errorRateRequest));
        entity.setEnable(dto.getEnable());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        return toDto(errorRateRequestRepository.save(entity));
    }

    @Transactional
    public void deleteErrorRateRequest(Long id) {
        ErrorRateRequest entity = errorRateRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ErrorRateRequest not found: " + id));
        entity.setIsDelete(true);
        entity.setDeletedAt(LocalDateTime.now());
        errorRateRequestRepository.save(entity);
    }

    public List<ErrorRateRequestDTO> fetchErrorRateRequestsWithoutPagination() {
        return errorRateRequestRepository.findAll().stream()
                .filter(r -> !r.getIsDelete())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<ErrorRateRequestDTO> fetchErrorRateRequestsWithPagination(int page, int size) {
        return errorRateRequestRepository.findByIsDeleteFalse(PageRequest.of(page, size)).map(this::toDto);
    }



    public CdrQueryConfigDTO updateQueryConfig(Long id, CdrQueryConfigDTO dto) {
        return cdrQueryConfigService.update(id, dto);
    }

    public void deleteQueryConfig(Long id) {
        cdrQueryConfigService.delete(id);
    }

    public List<CdrQueryConfigDTO> fetchAllQueryConfigs() {
        return cdrQueryConfigService.findAll();
    }

    public CdrQueryConfigDTO fetchQueryConfigById(Long id) {
        return cdrQueryConfigService.findById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CdrQueryConfig resolveQueryConfig(CdrQueryConfigDTO dto, String requestType) {
        if (dto == null) return null;
        if (dto.getId() != null) {
            cdrQueryConfigService.update(dto.getId(), dto);
            return cdrQueryConfigService.getEntityById(dto.getId());
        }
        dto.setQueryName(dto.getServiceType().name().concat("-") + UUID.randomUUID());
        if (dto.getQueryName() != null && !dto.getQueryName().isEmpty()) {
            CdrQueryConfigDTO created = cdrQueryConfigService.create(dto, requestType);
            return cdrQueryConfigService.getEntityById(created.getId());
        }
        return null;
    }

    private CdrQueryConfigDTO toQueryConfigDto(CdrQueryConfig entity) {
        if (entity == null) return null;
        return CdrQueryConfigDTO.builder()
                .id(entity.getId())
                .queryName(entity.getQueryName())
                .serviceType(entity.getServiceType())
                .fetchQuery(entity.getFetchQuery())
                .isActive(entity.getIsActive())
                .isDelete(entity.getIsDelete())
                .deletedAt(entity.getDeletedAt())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .createdBy(entity.getCreatedBy())
                .modifiedBy(entity.getModifiedBy())
                .build();
    }

    private ErrorRateRequestDTO toDto(ErrorRateRequest entity) {
        return ErrorRateRequestDTO.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .requestName(entity.getRequestName())
                .requestParameters(entity.getRequestParameters())
                .voiceQueryConfig(toQueryConfigDto(entity.getVoiceQueryConfig()))
                .smsQueryConfig(toQueryConfigDto(entity.getSmsQueryConfig()))
                .usageQueryConfig(toQueryConfigDto(entity.getUsageQueryConfig()))
                .status(entity.getStatus())
                .enable(entity.getEnable())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .requestedAt(entity.getRequestedAt())
                .isActive(entity.getIsActive())
                .isDelete(entity.getIsDelete())
                .deletedAt(entity.getDeletedAt())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .createdBy(entity.getCreatedBy())
                .modifiedBy(entity.getModifiedBy())
                .version(entity.getVersion())
                .build();
    }
}
