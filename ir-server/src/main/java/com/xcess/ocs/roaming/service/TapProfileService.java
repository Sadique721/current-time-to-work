package com.xcess.ocs.roaming.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PaginationDetailsDTO;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.DuplicateRatePackageException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.roaming.dto.TapFieldMappingDTO;
import com.xcess.ocs.roaming.dto.TapFieldMappingSearchDTO;
import com.xcess.ocs.roaming.dto.TapProfileDTO;
import com.xcess.ocs.roaming.dto.TapProfileFieldOverrideDTO;
import com.xcess.ocs.roaming.dto.TapProfileSearchDTO;
import com.xcess.ocs.roaming.entity.TapProfile;
import com.xcess.ocs.roaming.entity.TapProfileFieldMapping;
import com.xcess.ocs.roaming.repository.TapProfileFieldMappingRepository;
import com.xcess.ocs.roaming.repository.TapProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TapProfileService {

    private final TapFieldMappingService fieldMappingService;
    private final TapProfileRepository profileRepo;
    private final TapProfileFieldMappingRepository profileFieldMappingRepo;

    // =========================================================================
    // TAP FIELD MAPPINGS — delegated
    // =========================================================================

    public List<TapFieldMappingDTO> getAllFields() {
        return fieldMappingService.getAllFields();
    }

    public List<Map<String, Object>> getFieldDropdown() {
        return fieldMappingService.getFieldDropdown();
    }

    public TapFieldMappingDTO getField(Long id) {
        return fieldMappingService.getField(id);
    }

    public PageResponseDTO<TapFieldMappingDTO> searchFields(TapFieldMappingSearchDTO criteria, Pageable pageable) {
        return fieldMappingService.searchFields(criteria, pageable);
    }

    @Transactional
    public TapFieldMappingDTO createField(TapFieldMappingDTO req) {
        return fieldMappingService.createField(req);
    }

    @Transactional
    public TapFieldMappingDTO updateField(Long id, TapFieldMappingDTO req) {
        return fieldMappingService.updateField(id, req);
    }

    @Transactional
    public void deleteField(Long id) {
        fieldMappingService.deleteField(id);
    }

    // =========================================================================
    // TAP PROFILES
    // =========================================================================

    public List<Map<String, Object>> getProfileDropdown() {
        return profileRepo.findAll().stream()
                .map(p -> Map.<String, Object>of("id", p.getId(), "profileName", p.getProfileName()))
                .toList();
    }

    public List<TapProfileDTO> getAllProfiles() {
        return profileRepo.findAll().stream().map(this::toProfileDTO).toList();
    }

    public TapProfileDTO getProfile(Long id) {
        return toProfileDTO(findProfileOrThrow(id));
    }

    public PageResponseDTO<TapProfileDTO> searchProfiles(TapProfileSearchDTO criteria, Pageable pageable) {
        Page<TapProfile> page = profileRepo.search(
                criteria.getProfileName(),
                criteria.getDescription(),
                criteria.getIsActive(),
                pageable);
        return toPageResponse(page.map(this::toProfileSummaryDTO));
    }

    @Transactional
    public TapProfileDTO createProfile(TapProfileDTO req) {
        log.info("Creating new TAP profile with name: {}", req.getProfileName());

        if (profileRepo.existsByProfileNameIgnoreCaseAndIsDeletedFalse(req.getProfileName())) {
            log.warn("TAP profile name already exists: {}", req.getProfileName());
            throw new DuplicateNameException("TapProfile", req.getProfileName());
        }

        validateNoDuplicateFieldMappings(req.getFieldMappings());

        TapProfile profile = TapProfile.builder()
                .profileName(req.getProfileName())
                .description(req.getDescription())
                .isActive(req.isActive())
                .serviceType(req.getServiceType())
                .build();
        profileRepo.save(profile);
        applyFieldOverrides(profile, req.getFieldMappings());
        return toProfileDTO(profileRepo.save(profile));
    }

    @Transactional
    public TapProfileDTO updateProfile(Long id, TapProfileDTO req) {
        log.info("Updating TAP profile with ID: {}", id);

        TapProfile profile = findProfileOrThrow(id);

        // Uniqueness check — only if name is changing
        if (!profile.getProfileName().equalsIgnoreCase(req.getProfileName()) &&
                profileRepo.existsByProfileNameIgnoreCaseAndIsDeletedFalse(req.getProfileName())) {
            log.warn("TAP profile name already exists: {}", req.getProfileName());
            throw new DuplicateNameException("TapProfile", req.getProfileName());
        }

        validateNoDuplicateFieldMappings(req.getFieldMappings());

        profile.setProfileName(req.getProfileName());
        profile.setDescription(req.getDescription());
        profile.setActive(req.isActive());
        profile.setServiceType(req.getServiceType());

        updateFieldMappings(profile, req.getFieldMappings());

        return toProfileDTO(profileRepo.save(profile));
    }

    @Transactional
    public void deleteProfile(Long id) {
        TapProfile profile = findProfileOrThrow(id);
        if (profileRepo.existsActiveGroupByProfileId(id)) {
            throw new ForeignReferenceException(
                    "TapProfile cannot be deleted because it is referenced by an active TAP profile group.");
        }
        // Soft-delete child field mappings first (cascade won't do it since orphanRemoval is off)
        profileFieldMappingRepo.deleteAllByProfileId(id);
        profileRepo.delete(profile);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Diff-based update of profile field mappings — mirrors AgreementService.updateAgreementTaxConfigs.
     * - Removes entries no longer in the request
     * - Updates existing entries (customDefaultValue, isMandatoryOverride)
     * - Adds new entries not previously present
     */
    private void updateFieldMappings(TapProfile profile, List<TapProfileFieldOverrideDTO> incoming) {
        List<TapProfileFieldMapping> existing = profile.getFieldMappings();

        // Build map of existing entries by tapFieldMappingId
        Map<Long, TapProfileFieldMapping> existingMap = existing.stream()
                .collect(Collectors.toMap(
                        pfm -> pfm.getTapFieldMapping().getId(),
                        pfm -> pfm));

        Set<Long> incomingIds = incoming == null ? new HashSet<>() :
                incoming.stream()
                        .map(TapProfileFieldOverrideDTO::getTapFieldMappingId)
                        .collect(Collectors.toSet());

        // Remove entries no longer in request
        existing.removeIf(pfm -> !incomingIds.contains(pfm.getTapFieldMapping().getId()));

        if (incoming == null || incoming.isEmpty()) return;

        for (TapProfileFieldOverrideDTO o : incoming) {
            TapProfileFieldMapping existingPfm = existingMap.get(o.getTapFieldMappingId());
            if (existingPfm != null) {
                // Update existing entry
                existingPfm.setCustomDefaultValue(o.getCustomDefaultValue());
                existingPfm.setIsMandatoryOverride(o.getIsMandatoryOverride());
            } else {
                // Add new entry
                TapProfileFieldMapping pfm = TapProfileFieldMapping.builder()
                        .profile(profile)
                        .tapFieldMapping(fieldMappingService.findOrThrow(o.getTapFieldMappingId()))
                        .customDefaultValue(o.getCustomDefaultValue())
                        .isMandatoryOverride(o.getIsMandatoryOverride())
                        .build();
                existing.add(pfm);
            }
        }
    }

    private void validateNoDuplicateFieldMappings(List<TapProfileFieldOverrideDTO> overrides) {
        if (overrides == null || overrides.isEmpty()) return;

        List<String> duplicateFieldNames = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (TapProfileFieldOverrideDTO o : overrides) {
            Long fieldId = o.getTapFieldMappingId();
            if (seenIds.contains(fieldId)) {
                String fieldName = fieldMappingService.findOrThrow(fieldId).getFieldName();
                duplicateFieldNames.add(fieldName);
            } else {
                seenIds.add(fieldId);
            }
        }

        if (!duplicateFieldNames.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate field mappings found in profile: " + String.join(", ", duplicateFieldNames));
        }
    }

    private void applyFieldOverrides(TapProfile profile, List<TapProfileFieldOverrideDTO> overrides) {
        if (overrides == null || overrides.isEmpty()) return;
        for (TapProfileFieldOverrideDTO o : overrides) {
            TapProfileFieldMapping pfm = TapProfileFieldMapping.builder()
                    .profile(profile)
                    .tapFieldMapping(fieldMappingService.findOrThrow(o.getTapFieldMappingId()))
                    .customDefaultValue(o.getCustomDefaultValue())
                    .isMandatoryOverride(o.getIsMandatoryOverride())
                    .build();
            profile.getFieldMappings().add(pfm);
        }
    }

    private TapProfile findProfileOrThrow(Long id) {
        return profileRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TapProfile not found: " + id));
    }

    private TapProfileDTO toProfileSummaryDTO(TapProfile p) {
        TapProfileDTO r = new TapProfileDTO();
        r.setId(p.getId());
        r.setProfileName(p.getProfileName());
        r.setDescription(p.getDescription());
        r.setActive(p.isActive());
        r.setServiceType(p.getServiceType());
        r.setMappingCount(p.getFieldMappings().size());
        return r;
    }

    private TapProfileDTO toProfileDTO(TapProfile p) {
        TapProfileDTO r = new TapProfileDTO();
        r.setId(p.getId());
        r.setProfileName(p.getProfileName());
        r.setDescription(p.getDescription());
        r.setActive(p.isActive());
        r.setServiceType(p.getServiceType());
        r.setMappingCount(p.getFieldMappings().size());
        r.setFieldMappings(p.getFieldMappings().stream().map(pfm -> {
            TapProfileFieldOverrideDTO o = new TapProfileFieldOverrideDTO();
            o.setId(pfm.getId());
            o.setTapFieldMappingId(pfm.getTapFieldMapping().getId());
            o.setFieldName(pfm.getTapFieldMapping().getFieldName());
            o.setAsnPath(pfm.getTapFieldMapping().getAsnPath());
            o.setCustomDefaultValue(pfm.getCustomDefaultValue());
            o.setIsMandatoryOverride(pfm.getIsMandatoryOverride());
            return o;
        }).toList());
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
