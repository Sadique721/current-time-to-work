package com.xcess.ocs.service;

import com.xcess.ocs.cache.PartnerCache;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PartnerDTO;
import com.xcess.ocs.dto.PartnerSftpConfigDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.dto.search.PartnerSearchDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.exception.DuplicatePartnerException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.InvalidInputException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.PartnerMapper;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.repository.ClearingHouseRepository;
import com.xcess.ocs.repository.OrganizationRepository;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import com.xcess.ocs.roaming.repository.TapProfileGroupRepository;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PartnerService {
    private final PartnerRepository partnerRepository;
    private final AccountRepository accountRepository;
    private final OrganizationRepository organizationRepository;
    private final ClearingHouseRepository clearingHouseRepository;
    private final PartnerMapper partnerMapper;
    private final PartnerCache partnerCache;
    private final TapProfileGroupRepository tapProfileGroupRepository;

    public PartnerDTO createPartner(@Valid PartnerDTO dto) {
        log.info("Creating new partner with name: {}", dto.getPartnerName());

        validateLineOfBusinessFields(dto);

        if (partnerRepository.existsByPartnerNameAndIsDeletedFalse(dto.getPartnerName())) {
            log.warn("Partner name already exists: {}", dto.getPartnerName());
            throw new DuplicatePartnerException("Partner name already exists");
        }

        if (partnerRepository.existsByPartnerCodeAndIsDeletedFalse(dto.getPartnerCode())) {
            log.warn("Partner code already exists: {}", dto.getPartnerCode());
            throw new DuplicatePartnerException("Partner code already exists");
        }

        Organization organization = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> {
                    log.warn("Organization not found with ID: {}", dto.getOrganizationId());
                    return new ResourceNotFoundException("Organization not found");
                });

        Partner partner = partnerMapper.toEntity(dto, organization);
        Partner savedPartner = partnerRepository.save(partner);
        partnerCache.addToCache(partnerMapper.toDto(savedPartner));
        log.info("Successfully created partner with ID: {}", savedPartner.getPartnerId());
        return partnerMapper.toDto(savedPartner);
    }

    public List<PartnerDTO> getAllPartners() {
        log.debug("Fetching all partners");
        List<PartnerDTO> partners = partnerRepository.findAll().stream()
                .map(partnerMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} partners", partners.size());
        return partners;
    }

    public PartnerDTO getPartnerById(Long id) {
        log.debug("Fetching partner with ID: {}", id);
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Partner not found with ID: {}", id);
                    return new ResourceNotFoundException("Partner not found");
                });
        PartnerDTO partnerDTO = partnerMapper.toDto(partner);
        if (partner.getClearingHouse() != null) {
            partnerDTO.setClearingHouseName(partner.getClearingHouse().getName());
        }
        if (partner.getOrganization() != null) {
            partnerDTO.setOrganizationName(partner.getOrganization().getName());
        }
        return partnerDTO;
    }

    public PartnerDTO updatePartner(Long id, @Valid PartnerDTO dto) {
        log.info("Updating partner with ID: {}", id);

        validateLineOfBusinessFields(dto);

        Partner existingPartner = partnerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Partner not found with ID: {}", id);
                    return new ResourceNotFoundException("Partner not found");
                });

        Organization organization = organizationRepository.findById(dto.getOrganizationId())
                .orElseThrow(() -> {
                    log.warn("Organization not found with ID: {}", dto.getOrganizationId());
                    return new ResourceNotFoundException("Organization not found");
                });

        if (!existingPartner.getPartnerName().equalsIgnoreCase(dto.getPartnerName())) {
            if (partnerRepository.existsByPartnerNameAndIsDeletedFalse(dto.getPartnerName())) {
                log.warn("Partner name already exists: {}", dto.getPartnerName());
                throw new DuplicatePartnerException("Partner name already exists");
            }
        }

        if (!existingPartner.getPartnerCode().equalsIgnoreCase(dto.getPartnerCode())) {
            if (partnerRepository.existsByPartnerCodeAndIsDeletedFalse(dto.getPartnerCode())) {
                log.warn("Partner code already exists: {}", dto.getPartnerCode());
                throw new DuplicatePartnerException("Partner code already exists");
            }
        }

        Partner updatedEntity = partnerMapper.toEntity(dto, organization);
        updatedEntity.setPartnerId(id);
        if (dto.getLineOfBusiness() == LineOfBusiness.INTERCONNECT) {
            updatedEntity.setSftpRouteType(null);
            updatedEntity.setTapVersion(null);
            updatedEntity.setClearingHouse(null);
            updatedEntity.setSftpHost(null);
            updatedEntity.setSftpPort(null);
            updatedEntity.setSftpUsername(null);
            updatedEntity.setSftpPassword(null);
            updatedEntity.setSftpRemotePath(null);
            updatedEntity.setSftpInboxPath(null);
            updatedEntity.setTapProfileGroup(null);
        } else {
            updatedEntity.setSftpRouteType(existingPartner.getSftpRouteType());
            updatedEntity.setTapVersion(existingPartner.getTapVersion());
            if (existingPartner.getSftpRouteType() == TapSftpRouteType.VIA_CLEARING_HOUSE) {
                updatedEntity.setClearingHouse(existingPartner.getClearingHouse());
            } else {
                updatedEntity.setSftpHost(existingPartner.getSftpHost());
                updatedEntity.setSftpPort(existingPartner.getSftpPort());
                updatedEntity.setSftpUsername(existingPartner.getSftpUsername());
                updatedEntity.setSftpPassword(existingPartner.getSftpPassword());
                updatedEntity.setSftpRemotePath(existingPartner.getSftpRemotePath());
                updatedEntity.setSftpInboxPath(existingPartner.getSftpInboxPath());
            }
            if(existingPartner.getTapProfileGroup()!=null)
            {
                updatedEntity.setTapProfileGroup(existingPartner.getTapProfileGroup());
            }
        }

        Partner savedPartner = partnerRepository.save(updatedEntity);
        partnerCache.updateCache(partnerMapper.toDto(savedPartner));
        log.info("Successfully updated partner with ID: {}", id);
        return partnerMapper.toDto(savedPartner);
    }

    public void deletePartner(Long id) {
        log.info("Deleting partner with ID: {}", id);
        if (!partnerRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent partner with ID: {}", id);
            throw new ResourceNotFoundException("Partner not found");
        }
        boolean isReferenced = accountRepository.existsByPartner_PartnerIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete partner with ID: {} that is referenced by an active account", id);
            throw new ForeignReferenceException("Partner cannot be soft deleted because it is referenced by an active account.");
        }
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));
        partnerRepository.deleteById(id);
        partnerCache.removeFromCache(partner.getPartnerName());
        log.info("Successfully deleted partner with ID: {}", id);
    }

    public PageResponseDTO<PartnerDTO> getPartnersInPage(Pageable pageable) {
        log.debug("Fetching partners in pages");
        Page<Partner> partnersPage = partnerRepository.findAll(pageable);
        List<PartnerDTO> partnerDTOs = partnersPage.getContent().stream()
                .map(partnerMapper::toDto)
                .collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(partnerDTOs, partnersPage);
    }

    public PageResponseDTO<PartnerDTO> searchPartners(PartnerSearchDTO partnerSearchDTO, Pageable pageable) {
        log.debug("Searching partners with criteria: {}", partnerSearchDTO);

        String partnerName = partnerSearchDTO != null ? partnerSearchDTO.getPartnerName() : null;
        String partnerCode = partnerSearchDTO != null ? partnerSearchDTO.getPartnerCode() : null;
        var partnerType = partnerSearchDTO != null ? partnerSearchDTO.getPartnerType() : null;
        var status = partnerSearchDTO != null ? partnerSearchDTO.getStatus() : null;
        String country = partnerSearchDTO != null ? partnerSearchDTO.getCountry() : null;

        Page<Partner> partnersPage = partnerRepository.searchPartners(
                partnerName, partnerCode, partnerType, status, country, pageable);

        List<PartnerDTO> partnerDTOs = partnersPage.getContent().stream()
                .map(partnerMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Found {} partners matching criteria", partnerDTOs.size());
        return PaginationUtils.buildGetResponseDTO(partnerDTOs, partnersPage);
    }

    public List<Map<String, Object>> getAllPartnerIds() {
        return partnerRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partnerId", p.getPartnerId());
                    map.put("partnerName", p.getPartnerName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAllPartnerSummaries() {
        log.debug("Fetching all partner summaries");
        return partnerRepository.findAll().stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partnerId", p.getPartnerId());
                    map.put("partnerName", p.getPartnerName());
                    map.put("partnerType", p.getPartnerType());
                    map.put("hplmn", p.getHplmn());
                    map.put("lineOfBusiness", p.getLineOfBusiness());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPartnersByPartnerType(String partnerType) {
        log.debug("Fetching partners for partner type: {}", partnerType);
        PartnerType resolvedType = PartnerType.fromString(partnerType);
        if (resolvedType == null) {
            throw new IllegalArgumentException("Invalid Partner Type");
        }
        List<Partner> partners = partnerRepository.findByPartnerTypeAndIsDeletedFalse(resolvedType);
        return partners.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("partnerId", p.getPartnerId());
                    map.put("partnerName", p.getPartnerName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    public String generateUniquePartnerCode() {
        String code;
        do {
            code = generateRandomCode();
        } while (partnerRepository.countByPartnerCode(code) > 0);
        log.debug("Generated unique partner code: {}", code);
        return code;
    }

    private String generateRandomCode() {
        return "PTR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void validateLineOfBusinessFields(PartnerDTO dto) {
        if (dto.getLineOfBusiness() == LineOfBusiness.INTERCONNECT) {
            if (dto.getInterconnectType() == null)
                throw new InvalidInputException("interconnectType is required for INTERCONNECT line of business");
            if (dto.getInterconnectType() == InterconnectType.IP && (dto.getIpAddress() == null || dto.getIpAddress().isBlank()))
                throw new InvalidInputException("ipAddress is required when interconnectType is IP");
            if (dto.getClearingHouseId() != null)
                throw new InvalidInputException("clearingHouseId must not be set for INTERCONNECT line of business");
            if (dto.getTapProfileGroupId() != null)
                throw new InvalidInputException("tapProfileGroupId must not be set for INTERCONNECT line of business");
            if (dto.getTapVersion() != null)
                throw new InvalidInputException("tapVersion must not be set for INTERCONNECT line of business");
            if (dto.getTapSftpRouteType() != null)
                throw new InvalidInputException("tapSftpRouteType must not be set for INTERCONNECT line of business");
            if (dto.getSftpHost() != null)
                throw new InvalidInputException("sftpHost must not be set for INTERCONNECT line of business");
            if (dto.getSftpPort() != null)
                throw new InvalidInputException("sftpPort must not be set for INTERCONNECT line of business");
            if (dto.getSftpUsername() != null)
                throw new InvalidInputException("sftpUsername must not be set for INTERCONNECT line of business");
            if (dto.getSftpPassword() != null)
                throw new InvalidInputException("sftpPassword must not be set for INTERCONNECT line of business");
            if (dto.getSftpRemotePath() != null)
                throw new InvalidInputException("sftpRemotePath must not be set for INTERCONNECT line of business");
            if (dto.getSftpInboxPath() != null)
                throw new InvalidInputException("sftpInboxPath must not be set for INTERCONNECT line of business");
            if (dto.getTadigCode() != null)
                throw new InvalidInputException("tadigCode must not be set for INTERCONNECT line of business");
            if (dto.getHplmn() != null)
                throw new InvalidInputException("hplmn must not be set for INTERCONNECT line of business");
        }
        if (dto.getLineOfBusiness() == LineOfBusiness.ROAMING) {
            if (dto.getTadigCode() == null || dto.getTadigCode().isBlank())
                throw new InvalidInputException("tadigCode is required for ROAMING line of business");
            if (dto.getHplmn() == null || dto.getHplmn().isBlank())
                throw new InvalidInputException("hplmn is required for ROAMING line of business");
        }
    }

    public ResponseDTO updatePartnerSftpConfig(Long id, PartnerSftpConfigDTO dto) {
        log.info("Updating SFTP config for partner ID: {}", id);
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Partner not found with ID: {}", id);
                    return new ResourceNotFoundException("Partner not found");
                });

        if (partner.getLineOfBusiness() == LineOfBusiness.INTERCONNECT) {
            log.warn("Cannot update SFTP config for INTERCONNECT partner ID: {}", id);
            throw new InvalidInputException("SFTP configuration is not applicable for INTERCONNECT line of business");
        }

        if (dto.getTapSftpRouteType() == TapSftpRouteType.VIA_CLEARING_HOUSE) {
            if (dto.getClearingHouseId() == null)
                throw new InvalidInputException("clearingHouseId is required when sftpRouteType is VIA_CLEARING_HOUSE");
            if (dto.getSftpHost() != null || dto.getSftpPort() != null || dto.getSftpUsername() != null ||
                dto.getSftpPassword() != null || dto.getSftpRemotePath() != null || dto.getSftpInboxPath() != null)
                throw new InvalidInputException("Direct SFTP fields must not be set when sftpRouteType is VIA_CLEARING_HOUSE");
        } else if (dto.getTapSftpRouteType() == TapSftpRouteType.DIRECT) {
            if (dto.getClearingHouseId() != null)
                throw new InvalidInputException("clearingHouseId must not be set when sftpRouteType is DIRECT");
            if (dto.getSftpHost() == null || dto.getSftpPort() == null || dto.getSftpUsername() == null ||
                dto.getSftpPassword() == null || dto.getSftpRemotePath() == null || dto.getSftpInboxPath() == null)
                throw new InvalidInputException("All SFTP fields (host, port, username, password, remotePath, inboxPath) are required when sftpRouteType is DIRECT");
        }

        if (dto.getSftpHost() != null) partner.setSftpHost(dto.getSftpHost());
        if (dto.getSftpPort() != null) partner.setSftpPort(dto.getSftpPort());
        if (dto.getSftpUsername() != null) partner.setSftpUsername(dto.getSftpUsername());
        if (dto.getSftpPassword() != null) partner.setSftpPassword(dto.getSftpPassword());
        if (dto.getSftpRemotePath() != null) partner.setSftpRemotePath(dto.getSftpRemotePath());
        if (dto.getSftpInboxPath() != null) partner.setSftpInboxPath(dto.getSftpInboxPath());
        if (dto.getTapSftpRouteType() != null) partner.setSftpRouteType(dto.getTapSftpRouteType());
        if (dto.getTapVersion() != null) partner.setTapVersion(dto.getTapVersion());
        if (dto.getClearingHouseId() != null) {
            ClearingHouse clearingHouse = clearingHouseRepository.findByIdAndIsDeletedFalse(dto.getClearingHouseId())
                    .orElseThrow(() -> {
                        log.warn("Clearing house not found with ID: {}", dto.getClearingHouseId());
                        return new ResourceNotFoundException("Clearing house not found");
                    });
            partner.setClearingHouse(clearingHouse);
        }
        if (dto.getTapProfileGroupId() != null) {
            TapProfileGroup group = tapProfileGroupRepository.findById(dto.getTapProfileGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("TapProfileGroup not found"));
            partner.setTapProfileGroup(group);
        }

        partnerRepository.save(partner);
        partnerCache.updateCache(partnerMapper.toDto(partner));
        log.info("Successfully updated SFTP config for partner ID: {}", id);
        return ResponseDTO.ok("Partner SFTP configuration updated successfully");
    }

    public PartnerSftpConfigDTO getPartnerSftpConfig(Long id) {
        log.debug("Fetching SFTP config for partner ID: {}", id);
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Partner not found with ID: {}", id);
                    return new ResourceNotFoundException("Partner not found");
                });

        PartnerSftpConfigDTO dto = new PartnerSftpConfigDTO();
        dto.setSftpHost(partner.getSftpHost());
        dto.setSftpPort(partner.getSftpPort());
        dto.setSftpUsername(partner.getSftpUsername());
        dto.setSftpPassword(partner.getSftpPassword());
        dto.setSftpRemotePath(partner.getSftpRemotePath());
        dto.setSftpInboxPath(partner.getSftpInboxPath());
        dto.setTapSftpRouteType(partner.getSftpRouteType());
        dto.setTapVersion(partner.getTapVersion());
        if (partner.getClearingHouse() != null) {
            dto.setClearingHouseId(partner.getClearingHouse().getId());
            dto.setClearingHouseName(partner.getClearingHouse().getName());
        }
        if (partner.getTapProfileGroup() != null) {
            dto.setTapProfileGroupId(partner.getTapProfileGroup().getId());
            dto.setTapProfileGroupName(partner.getTapProfileGroup().getName());
        }
        log.debug("Retrieved SFTP config for partner ID: {}", id);
        return dto;
    }
}
