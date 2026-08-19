package com.xcess.ocs.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.SchedulerAuditLogDTO;
import com.xcess.ocs.dto.search.SchedulerAuditLogSearchDTO;
import com.xcess.ocs.entity.SchedulerAuditLog;
import com.xcess.ocs.mapper.SchedulerAuditLogMapper;
import com.xcess.ocs.repository.SchedulerAuditLogRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SchedulerAuditLogService {
    
    private final SchedulerAuditLogRepository auditLogRepository;
    private final SchedulerAuditLogMapper auditLogMapper;

    public PageResponseDTO<SchedulerAuditLogDTO> getAuditLogsInPage(Pageable pageable) {
        log.debug("Fetching scheduler audit logs in pages");
        Page<SchedulerAuditLog> auditLogsPage = auditLogRepository.findAllByOrderByCreatedDateDesc(pageable);
        List<SchedulerAuditLogDTO> auditLogDTOs = auditLogsPage.getContent().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(auditLogDTOs, auditLogsPage);
    }

    public PageResponseDTO<SchedulerAuditLogDTO> searchAuditLogs(SchedulerAuditLogSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching scheduler audit logs with criteria: {}", searchDTO);

        Long configId = searchDTO != null ? searchDTO.getConfigId() : null;
        String eventType = searchDTO != null ? searchDTO.getEventType() : null;
        var createdAtFrom = searchDTO != null ? searchDTO.getCreatedAtFrom() : null;
        var createdAtTo = searchDTO != null ? searchDTO.getCreatedAtTo() : null;
        Long schedulerStatusId = searchDTO != null ? searchDTO.getSchedulerStatusId() : null;

        Page<SchedulerAuditLog> auditLogsPage = auditLogRepository.searchAuditLogs(
                configId,
                eventType,
                createdAtFrom,
                createdAtTo,
                schedulerStatusId,
                pageable
        );

        List<SchedulerAuditLogDTO> auditLogDTOs = auditLogsPage.getContent().stream()
                .map(auditLogMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Found {} audit logs matching criteria", auditLogDTOs.size());
        return PaginationUtils.buildGetResponseDTO(auditLogDTOs, auditLogsPage);
    }
}
