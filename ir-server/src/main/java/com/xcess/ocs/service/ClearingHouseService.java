package com.xcess.ocs.service;

import com.xcess.ocs.dto.ClearingHouseDTO;
import com.xcess.ocs.dto.ClearingHouseSftpConfigDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.entity.ClearingHouse;
import com.xcess.ocs.entity.ClearingHouseProtocol;
import com.xcess.ocs.entity.ClearingHouseProtocol.SupportedProtocol;
import com.xcess.ocs.entity.ClearingHouseStatus;
import com.xcess.ocs.entity.ClearingHouseType;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.ClearingHouseRepository;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClearingHouseService {

    private final ClearingHouseRepository clearingHouseRepository;
    private final PartnerRepository partnerRepository;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    public ClearingHouseDTO create(ClearingHouseDTO dto) {
        log.info("Creating clearing house: {}", dto.getName());

        if (clearingHouseRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new DuplicateNameException("ClearingHouse", dto.getName());
        }

        ClearingHouse entity = toEntity(dto);
        ClearingHouse saved = clearingHouseRepository.save(entity);
        log.info("Created clearing house with ID: {}", saved.getId());
        return toDto(saved);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ClearingHouseDTO> getAll() {
        return clearingHouseRepository.findByIsDeletedFalse()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClearingHouseDTO getById(Long id) {
        return toDto(findOrThrow(id));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PageResponseDTO<ClearingHouseDTO> search(String name, ClearingHouseType type,
                                                     ClearingHouseStatus status, Pageable pageable) {
        Page<ClearingHouse> page = clearingHouseRepository.search(name, type, status, pageable);
        List<ClearingHouseDTO> dtos = page.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(dtos, page);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Map<String, Object>> getDropdown() {
        return clearingHouseRepository.findIdAndNameActive();
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    public ClearingHouseDTO update(Long id, ClearingHouseDTO dto) {
        log.info("Updating clearing house ID: {}", id);
        ClearingHouse existing = findOrThrow(id);

        if (!existing.getName().equalsIgnoreCase(dto.getName()) &&
                clearingHouseRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new DuplicateNameException("ClearingHouse", dto.getName());
        }

        existing.setName(dto.getName());
        existing.setType(dto.getType());
        existing.setStatus(dto.getStatus());
        existing.setDefaultCurrency(dto.getDefaultCurrency());
        existing.setTimezone(dto.getTimezone());

        // Update protocols — only add/remove what changed to avoid unique constraint violations
        if (dto.getProtocols() != null) {
            Set<SupportedProtocol> desiredProtocols = new HashSet<>(dto.getProtocols());
            Set<SupportedProtocol> currentProtocols = existing.getProtocols().stream()
                    .map(ClearingHouseProtocol::getProtocol)
                    .collect(Collectors.toSet());

            existing.getProtocols().removeIf(p -> !desiredProtocols.contains(p.getProtocol()));

            desiredProtocols.stream()
                    .filter(p -> !currentProtocols.contains(p))
                    .forEach(p -> existing.getProtocols().add(
                            ClearingHouseProtocol.builder()
                                    .clearingHouse(existing)
                                    .protocol(p)
                                    .build()));
        } else {
            existing.getProtocols().clear();
        }

        ClearingHouse saved = clearingHouseRepository.save(existing);
        log.info("Updated clearing house ID: {}", id);
        return toDto(saved);
    }

    // ─── SFTP Config ─────────────────────────────────────────────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ClearingHouseSftpConfigDTO getSftpConfig(Long id) {
        log.debug("Fetching SFTP config for clearing house ID: {}", id);
        ClearingHouse entity = findOrThrow(id);
        ClearingHouseSftpConfigDTO dto = new ClearingHouseSftpConfigDTO();
        dto.setSftpHost(entity.getSftpHost());
        dto.setSftpPort(entity.getSftpPort());
        dto.setSftpUsername(entity.getSftpUsername());
        dto.setSftpPassword(entity.getSftpPassword());
        dto.setSftpRemotePath(entity.getSftpRemotePath());
        dto.setSftpInboxPath(entity.getSftpInboxPath());
        log.debug("Retrieved SFTP config for clearing house ID: {}", id);
        return dto;
    }

    public ResponseDTO updateSftpConfig(Long id, ClearingHouseSftpConfigDTO dto) {
        log.info("Updating SFTP config for clearing house ID: {}", id);

        ClearingHouse existing = findOrThrow(id);

        if (dto.getSftpHost() != null) existing.setSftpHost(dto.getSftpHost());
        if (dto.getSftpPort() != null) existing.setSftpPort(dto.getSftpPort());
        if (dto.getSftpUsername() != null) existing.setSftpUsername(dto.getSftpUsername());
        if (dto.getSftpPassword() != null) existing.setSftpPassword(dto.getSftpPassword());
        if (dto.getSftpRemotePath() != null) existing.setSftpRemotePath(dto.getSftpRemotePath());
        if (dto.getSftpInboxPath() != null) existing.setSftpInboxPath(dto.getSftpInboxPath());

        clearingHouseRepository.save(existing);

        log.info("Successfully updated SFTP config for clearing house ID: {}", id);
        return ResponseDTO.ok("Clearing House SFTP configuration updated successfully");
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    public void delete(Long id) {
        log.info("Deleting clearing house ID: {}", id);
        findOrThrow(id);

        boolean referenced = partnerRepository.existsByClearingHouse_IdAndIsDeletedFalse(id);
        if (referenced) {
            throw new ForeignReferenceException(
                    "ClearingHouse cannot be deleted because it is referenced by an active Partner.");
        }

        clearingHouseRepository.deleteById(id);
        log.info("Deleted clearing house ID: {}", id);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private ClearingHouse findOrThrow(Long id) {
        return clearingHouseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClearingHouse not found with ID: " + id));
    }

    private ClearingHouse toEntity(ClearingHouseDTO dto) {
        ClearingHouse entity = ClearingHouse.builder()
                .name(dto.getName())
                .type(dto.getType())
                .status(dto.getStatus())
                .defaultCurrency(dto.getDefaultCurrency())
                .timezone(dto.getTimezone())
                .sftpHost(dto.getSftpHost())
                .sftpPort(dto.getSftpPort())
                .sftpUsername(dto.getSftpUsername())
                .sftpPassword(dto.getSftpPassword())
                .sftpRemotePath(dto.getSftpRemotePath())
                .sftpInboxPath(dto.getSftpInboxPath())
                .build();

        if (dto.getProtocols() != null) {
            List<ClearingHouseProtocol> protocols = dto.getProtocols().stream()
                    .map(p -> ClearingHouseProtocol.builder()
                            .clearingHouse(entity)
                            .protocol(p)
                            .build())
                    .collect(Collectors.toList());
            entity.setProtocols(protocols);
        }
        return entity;
    }

    private ClearingHouseDTO toDto(ClearingHouse entity) {
        List<SupportedProtocol> protocols = entity.getProtocols() == null ? List.of() :
                entity.getProtocols().stream()
                        .map(ClearingHouseProtocol::getProtocol)
                        .collect(Collectors.toList());

        return ClearingHouseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .type(entity.getType())
                .status(entity.getStatus())
                .defaultCurrency(entity.getDefaultCurrency())
                .timezone(entity.getTimezone())
                .protocols(protocols)
                .sftpHost(entity.getSftpHost())
                .sftpPort(entity.getSftpPort())
                .sftpUsername(entity.getSftpUsername())
                .sftpPassword(entity.getSftpPassword())
                .sftpRemotePath(entity.getSftpRemotePath())
                .sftpInboxPath(entity.getSftpInboxPath())
                .build();
    }
}
