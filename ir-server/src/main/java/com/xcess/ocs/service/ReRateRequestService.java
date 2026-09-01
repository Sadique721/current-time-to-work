package com.xcess.ocs.service;

import com.xcess.ocs.constants.enums.RequestStatus;
import com.xcess.ocs.dto.CdrQueryConfigDTO;
import com.xcess.ocs.dto.ReRateRequestDTO;
import com.xcess.ocs.entity.CdrQueryConfig;
import com.xcess.ocs.entity.ReRateRequest;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.repository.ReRateRequestRepository;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReRateRequestService {

    private final ReRateRequestRepository reRateRequestRepository;
    private final CdrQueryConfigService cdrQueryConfigService;
    private final VoiceRatedCdrRepository voiceRatedCdrRepository;
    private final SmsRatedCdrRepository smsRatedCdrRepository;
    private final UsageRatedCdrRepository usageRatedCdrRepository;
    private final String reRateRequest = com.xcess.ocs.constants.enums.CdrQueryRequestType.RERATE_REQUEST.label();

    @Transactional
    public void editReRateStatus(String requestId, String status) {
        reRateRequestRepository.findByRequestId(requestId).ifPresentOrElse(
                request -> {
                    String currentStatus = request.getStatus();
                    if (RequestStatus.FAILED.name().equals(currentStatus) || RequestStatus.COMPLETED.name().equals(currentStatus)
                            || RequestStatus.IN_PROGRESS.name().equals(currentStatus)) {
                        request.setStatus(status);
                        reRateRequestRepository.save(request);
                        log.info("Updated ReRateRequest {} status from {} to {}", requestId, currentStatus, status);
                    } else {
                        log.warn("ReRateRequest {} is in {} state, cannot update to {}", requestId, currentStatus,
                                status);
                    }
                },
                () -> log.warn("ReRateRequest {} not found", requestId));
    }

    @Transactional
    public ReRateRequestDTO createReRateRequest(ReRateRequestDTO dto) {
        ReRateRequest entity = new ReRateRequest();
        entity.setRequestId(
                dto.getRequestId() != null ? dto.getRequestId() : "RR-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setAuditName(dto.getAuditName());
        entity.setRequestParameters(dto.getRequestParameters());
        entity.setVoiceQueryConfig(resolveQueryConfig(dto.getVoiceQueryConfig(), reRateRequest));
        entity.setSmsQueryConfig(resolveQueryConfig(dto.getSmsQueryConfig(), reRateRequest));
        entity.setUsageQueryConfig(resolveQueryConfig(dto.getUsageQueryConfig(), reRateRequest));
        entity.setStatus(RequestStatus.NEW.name());
        entity.setEnable(dto.getEnable() != null ? dto.getEnable() : true);
        entity.setRemark(dto.getRemark());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setRequestedAt(LocalDateTime.now());
        entity.setIsActive(true);
        entity.setIsDelete(false);
        return toDto(reRateRequestRepository.save(entity));
    }

    @Transactional
    public ReRateRequestDTO updateReRateRequest(ReRateRequestDTO dto, Long id) {
        ReRateRequest entity = reRateRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ReRateRequest not found: " + id));
        entity.setAuditName(dto.getAuditName());
        entity.setRequestParameters(dto.getRequestParameters());
        entity.setVoiceQueryConfig(resolveQueryConfig(dto.getVoiceQueryConfig(), reRateRequest));
        entity.setSmsQueryConfig(resolveQueryConfig(dto.getSmsQueryConfig(), reRateRequest));
        entity.setUsageQueryConfig(resolveQueryConfig(dto.getUsageQueryConfig(), reRateRequest));
        entity.setEnable(dto.getEnable());
        entity.setRemark(dto.getRemark());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        return toDto(reRateRequestRepository.save(entity));
    }

    @Transactional
    public void deleteReRateRequest(Long id) {
        ReRateRequest entity = reRateRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ReRateRequest not found: " + id));
        entity.setIsDelete(true);
        entity.setDeletedAt(LocalDateTime.now());
        reRateRequestRepository.save(entity);
    }

    public List<ReRateRequestDTO> fetchReRateRequestsWithoutPagination() {
        return reRateRequestRepository.findAll().stream()
                .filter(r -> !r.getIsDelete())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<ReRateRequestDTO> fetchReRateRequestsWithPagination(int page, int size) {
        return reRateRequestRepository.findByIsDeleteFalse(PageRequest.of(page, size)).map(this::toDto);
    }

    public ReRateRequestDTO fetchReRateRequestById(Long id) {
        return reRateRequestRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("ReRateRequest not found: " + id));
    }

    // ── CdrQueryConfig CRUD (managed under rerate-requests) ──────────────────

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

    // ── fetchErrorConfigCheckResult ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> fetchErrorConfigCheckResult(int page, int size) {
        log.info("Fetching error config check result page...");
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);

        Page<com.xcess.ocs.entity.VoiceRatedCdr> failedVoiceCdrs = voiceRatedCdrRepository
                .findByIncomingOrOutgoingRatingStatus(RatingStatus.FAILED, pageable);
        Page<com.xcess.ocs.entity.SmsRatedCdr> failedSmsCdrs = smsRatedCdrRepository
                .findByIncomingOrOutgoingRatingStatus(RatingStatus.FAILED, pageable);
        Page<com.xcess.ocs.entity.UsageRatedCdr> failedUsageCdrs = usageRatedCdrRepository
                .findByIncomingOrOutgoingRatingStatus(RatingStatus.FAILED, pageable);

        List<com.xcess.ocs.entity.RateableCdr> combinedCdrs = new ArrayList<>();
        combinedCdrs.addAll(failedVoiceCdrs.getContent());
        combinedCdrs.addAll(failedSmsCdrs.getContent());
        combinedCdrs.addAll(failedUsageCdrs.getContent());

        long totalElements = failedVoiceCdrs.getTotalElements() + failedSmsCdrs.getTotalElements()
                + failedUsageCdrs.getTotalElements();

        List<com.xcess.ocs.dto.ErrorConfigCheckStatusDTO> dtoList = combinedCdrs.stream().limit(size).map(cdr -> {
            com.xcess.ocs.dto.ErrorConfigCheckStatusDTO dto = new com.xcess.ocs.dto.ErrorConfigCheckStatusDTO();
            Long id = cdr instanceof com.xcess.ocs.entity.VoiceRatedCdr
                    ? ((com.xcess.ocs.entity.VoiceRatedCdr) cdr).getRatedCdrId()
                    : cdr instanceof com.xcess.ocs.entity.SmsRatedCdr
                            ? ((com.xcess.ocs.entity.SmsRatedCdr) cdr).getSmsRatedCdrId()
                            : ((com.xcess.ocs.entity.UsageRatedCdr) cdr).getRatedCdrId();
            dto.setId(id);
            dto.setErrorRatedRecordId(cdr.getServiceType() + "-" + id);
            dto.setServiceType(cdr.getServiceType() != null ? cdr.getServiceType().name() : "UNKNOWN");
            dto.setLineOfBusiness(cdr.getLineOfBusiness() != null ? cdr.getLineOfBusiness().name() : "UNKNOWN");
            dto.setCallingNumber(cdr.getCallingNumber());
            dto.setCalledNumber(cdr.getCalledNumber());
            dto.setIncomingAccountId(cdr.getIncomingAccountId());
            dto.setOutgoingAccountId(cdr.getOutgoingAccountId());
            try {
                dto.setIsConfigReady(false);
                String incomingReason = cdr.getIncomingRatingFailureReason();
                String outgoingReason = cdr.getOutgoingRatingFailureReason();
                if (RatingStatus.FAILED.equals(cdr.getIncomingRatingStatus())
                        || RatingStatus.FAILED.equals(cdr.getOutgoingRatingStatus())) {
                    String mainReason = RatingStatus.FAILED.equals(cdr.getIncomingRatingStatus()) ? incomingReason
                            : outgoingReason;
                    dto.setErrorMessage(mainReason != null ? mainReason : "Rating failed");
                } else {
                    dto.setIsConfigReady(true);
                    dto.setErrorMessage("Ready for re-rating");
                }
                populateValidationChecks(dto, cdr, incomingReason, true);
                populateValidationChecks(dto, cdr, outgoingReason, false);
            } catch (Exception e) {
                dto.setIsConfigReady(false);
                dto.setErrorMessage(e.getMessage());
                dto.setIncomingSuccessData(new ArrayList<>());
                dto.setIncomingFailureData(Arrays.asList("Internal Error: " + e.getMessage()));
                dto.setOutgoingSuccessData(new ArrayList<>());
                dto.setOutgoingFailureData(Arrays.asList("Internal Error: " + e.getMessage()));
            }
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", dtoList);
        response.put("totalElements", totalElements);
        return response;
    }

    private void populateValidationChecks(com.xcess.ocs.dto.ErrorConfigCheckStatusDTO dto,
            com.xcess.ocs.entity.RateableCdr cdr, String failureReason, boolean isIncoming) {
        List<String> success = new ArrayList<>();
        List<String> failure = new ArrayList<>();
        boolean hasFailed = false;

        if ("INVALID_TIMESTAMP".equals(failureReason) || "MISSING_START_TIME".equals(failureReason)) {
            failure.add("Start Time Present");
            hasFailed = true;
        } else {
            success.add("Start Time Present");
        }
        if (hasFailed) {
            setDtoData(dto, success, failure, isIncoming);
            return;
        }

        if (com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT.equals(cdr.getLineOfBusiness())) {
            if ("MISSING_ACCOUNT_ID".equals(failureReason)) {
                failure.add("Account Context (Incoming/Outgoing) Present");
                hasFailed = true;
            } else {
                success.add("Account Context (Incoming/Outgoing) Present");
            }
        } else if (com.xcess.ocs.entity.LineOfBusiness.ROAMING.equals(cdr.getLineOfBusiness())) {
            if ("MISSING_HOME_PLMN".equals(failureReason)) {
                failure.add("Home PLMN Present");
                hasFailed = true;
            } else {
                success.add("Home PLMN Present");
                if ("MISSING_VISITED_PLMN".equals(failureReason)) {
                    failure.add("Visited PLMN Present");
                    hasFailed = true;
                } else {
                    success.add("Visited PLMN Present");
                }
            }
        }
        if (hasFailed) {
            setDtoData(dto, success, failure, isIncoming);
            return;
        }

        if (com.xcess.ocs.entity.ServiceType.VOICE.equals(cdr.getServiceType())) {
            if ("MISSING_CALLING_NUMBER".equals(failureReason)) {
                failure.add("Calling Number Present");
                hasFailed = true;
            } else {
                success.add("Calling Number Present");
                if ("MISSING_CALLED_NUMBER".equals(failureReason)) {
                    failure.add("Called Number Present");
                    hasFailed = true;
                } else {
                    success.add("Called Number Present");
                    if ("MISSING_END_TIME".equals(failureReason)) {
                        failure.add("End Time Present");
                        hasFailed = true;
                    } else {
                        success.add("End Time Present");
                        if ("INVALID_DURATION".equals(failureReason)) {
                            failure.add("Duration Valid (Chronological)");
                            hasFailed = true;
                        } else {
                            success.add("Duration Valid (Chronological)");
                        }
                    }
                }
            }
        } else if (com.xcess.ocs.entity.ServiceType.SMS.equals(cdr.getServiceType())) {
            if ("MISSING_CALLING_NUMBER".equals(failureReason)) {
                failure.add("Calling Number Present");
                hasFailed = true;
            } else {
                success.add("Calling Number Present");
                if ("MISSING_CALLED_NUMBER".equals(failureReason)) {
                    failure.add("Called Number Present");
                    hasFailed = true;
                } else {
                    success.add("Called Number Present");
                    if ("INVALID_MESSAGE_COUNT".equals(failureReason)) {
                        failure.add("Message Count Valid");
                        hasFailed = true;
                    } else {
                        success.add("Message Count Valid");
                    }
                }
            }
        } else if (com.xcess.ocs.entity.ServiceType.USAGE.equals(cdr.getServiceType())) {
            if ("MISSING_SUBSCRIBER_IDENTITY".equals(failureReason)) {
                failure.add("Subscriber Identity Present");
                hasFailed = true;
            } else {
                success.add("Subscriber Identity Present");
                if ("MISSING_ACCESS_POINT_NAME".equals(failureReason)) {
                    failure.add("Access Point Name Present");
                    hasFailed = true;
                } else {
                    success.add("Access Point Name Present");
                    if ("INVALID_USAGE_AMOUNT".equals(failureReason)) {
                        failure.add("Usage Amount Valid");
                        hasFailed = true;
                    } else {
                        success.add("Usage Amount Valid");
                        if ("MISSING_MEASUREMENT_UNIT".equals(failureReason)) {
                            failure.add("Measurement Unit Present");
                            hasFailed = true;
                        } else {
                            success.add("Measurement Unit Present");
                        }
                    }
                }
            }
        }
        if (hasFailed) {
            setDtoData(dto, success, failure, isIncoming);
            return;
        }

        if ("NULL_ACCOUNT_ID_AND_PLMN".equals(failureReason)) {
            failure.add("Rating Account/PLMN Resolved");
        } else {
            success.add("Rating Account/PLMN Resolved");
            if ("NO_RATE_PACKAGE".equals(failureReason)) {
                failure.add("Rate Package Configured");
            } else {
                success.add("Rate Package Configured");
                if ("NO_MATCHING_RATE".equals(failureReason)) {
                    failure.add("Specific Rate Details Matched");
                } else {
                    success.add("Specific Rate Details Matched");
                    if (failureReason != null && !failureReason.isEmpty()) {
                        failure.add(failureReason);
                    }
                }
            }
        }
        setDtoData(dto, success, failure, isIncoming);
    }

    private void setDtoData(com.xcess.ocs.dto.ErrorConfigCheckStatusDTO dto, List<String> success, List<String> failure,
            boolean isIncoming) {
        if (isIncoming) {
            dto.setIncomingSuccessData(success);
            dto.setIncomingFailureData(failure);
        } else {
            dto.setOutgoingSuccessData(success);
            dto.setOutgoingFailureData(failure);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CdrQueryConfig resolveQueryConfig(CdrQueryConfigDTO dto, String requestType) {
        if (dto == null)
            return null;
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
        if (entity == null)
            return null;
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

    private ReRateRequestDTO toDto(ReRateRequest entity) {
        return ReRateRequestDTO.builder()
                .id(entity.getId())
                .requestId(entity.getRequestId())
                .auditName(entity.getAuditName())
                .requestParameters(entity.getRequestParameters())
                .voiceQueryConfig(toQueryConfigDto(entity.getVoiceQueryConfig()))
                .smsQueryConfig(toQueryConfigDto(entity.getSmsQueryConfig()))
                .usageQueryConfig(toQueryConfigDto(entity.getUsageQueryConfig()))
                .status(entity.getStatus())
                .enable(entity.getEnable())
                .remark(entity.getRemark())
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
