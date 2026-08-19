package com.xcess.ocs.service;

import com.xcess.ocs.dto.CdrQueryConfigDTO;
import com.xcess.ocs.entity.CdrQueryConfig;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.repository.CdrQueryConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CdrQueryConfigService {

    private final CdrQueryConfigRepository repository;

    @Transactional
    public CdrQueryConfigDTO create(CdrQueryConfigDTO dto, String requestType) {
        String fetchQuery = null;
        if(requestType.equals("ErrorRequest")){
            fetchQuery  = createErrorQuery(dto.getServiceType());
        }
        if(requestType.equals("ReRateRequest")){
            fetchQuery  = createRerateQuery(dto.getServiceType());
        }

        CdrQueryConfig entity = CdrQueryConfig.builder()
                .queryName(dto.getQueryName())
                .serviceType(dto.getServiceType())
                .fetchQuery(fetchQuery)
                .isActive(true)
                .isDelete(false)
                .build();
        return toDto(repository.save(entity));
    }

    private String createErrorQuery(ServiceType type){
        if(type.equals(ServiceType.VOICE)){
            return "SELECT rated_cdr_id FROM voice_rated_cdr WHERE (incoming_rating_status = 'FAILED' OR outgoing_rating_status = 'FAILED')";
        }
        if(type.equals(ServiceType.SMS)){
            return "SELECT sms_rated_cdr_id FROM sms_rated_cdr WHERE (incoming_rating_status = 'FAILED' OR outgoing_rating_status = 'FAILED')";
        }
        if(type.equals(ServiceType.USAGE)){
            return "SELECT rated_cdr_id FROM usage_rated_cdr WHERE (incoming_rating_status = 'FAILED' OR outgoing_rating_status = 'FAILED')";
        }
        return null;
    }

    private String createRerateQuery(ServiceType type){
        if(type.equals(ServiceType.VOICE)){
            return "SELECT rated_cdr_id FROM voice_rated_cdr WHERE (incoming_rating_status IN ('RATED', 'UNRATED') OR outgoing_rating_status IN ('RATED', 'UNRATED'))";
        }
        if(type.equals(ServiceType.SMS)){
            return "SELECT sms_rated_cdr_id FROM sms_rated_cdr WHERE (incoming_rating_status IN ('RATED', 'UNRATED') OR outgoing_rating_status IN ('RATED', 'UNRATED'))";
        }
        if(type.equals(ServiceType.USAGE)){
            return "SELECT rated_cdr_id FROM usage_rated_cdr WHERE (incoming_rating_status IN ('RATED', 'UNRATED') OR outgoing_rating_status IN ('RATED', 'UNRATED'))";
        }
        return null;
    }

    @Transactional
    public CdrQueryConfigDTO update(Long id, CdrQueryConfigDTO dto) {
        CdrQueryConfig entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CdrQueryConfig not found: " + id));
        entity.setQueryName(dto.getQueryName());
        entity.setServiceType(dto.getServiceType());
        if (dto.getFetchQuery() != null) {
            entity.setFetchQuery(dto.getFetchQuery());
        }
        return toDto(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        CdrQueryConfig entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CdrQueryConfig not found: " + id));
        entity.setIsDelete(true);
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }

    public List<CdrQueryConfigDTO> findAll() {
        return repository.findAll().stream()
                .filter(e -> !e.getIsDelete())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CdrQueryConfigDTO findById(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("CdrQueryConfig not found: " + id));
    }

    public CdrQueryConfig getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("CdrQueryConfig not found: " + id));
    }

    private CdrQueryConfigDTO toDto(CdrQueryConfig e) {
        return CdrQueryConfigDTO.builder()
                .id(e.getId())
                .queryName(e.getQueryName())
                .serviceType(e.getServiceType())
                .fetchQuery(e.getFetchQuery())
                .isActive(e.getIsActive())
                .isDelete(e.getIsDelete())
                .deletedAt(e.getDeletedAt())
                .createdAt(e.getCreatedAt())
                .modifiedAt(e.getModifiedAt())
                .createdBy(e.getCreatedBy())
                .modifiedBy(e.getModifiedBy())
                .build();
    }
}
