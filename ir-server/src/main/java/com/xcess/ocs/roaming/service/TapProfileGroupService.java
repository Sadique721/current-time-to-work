package com.xcess.ocs.roaming.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PaginationDetailsDTO;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.DuplicateRatePackageException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.roaming.dto.TapProfileDTO;
import com.xcess.ocs.roaming.dto.TapProfileGroupDTO;
import com.xcess.ocs.roaming.dto.TapProfileGroupSearchDTO;
import com.xcess.ocs.roaming.entity.TapProfile;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import com.xcess.ocs.roaming.repository.TapProfileGroupRepository;
import com.xcess.ocs.roaming.repository.TapProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TapProfileGroupService {

    private final TapProfileGroupRepository groupRepo;
    private final TapProfileRepository profileRepo;

    public List<Map<String, Object>> getGroupDropdown() {
        return groupRepo.findAll().stream()
                .map(g -> Map.<String, Object>of("id", g.getId(), "name", g.getName()))
                .toList();
    }

    public Map<String, List<Map<String, Object>>> getTapProfilesByServiceType() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("voice", new ArrayList<>());
        result.put("sms", new ArrayList<>());
        result.put("usage", new ArrayList<>());

        profileRepo.findAll().forEach(p -> {
            if (p.getServiceType() == null) return;
            String key = p.getServiceType().toLowerCase();
            if (result.containsKey(key)) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", p.getId());
                entry.put("profileName", p.getProfileName());
                entry.put("serviceType", p.getServiceType());
                result.get(key).add(entry);
            }
        });
        return result;
    }

    public List<TapProfileGroupDTO> getAllGroups() {
        return groupRepo.findAll().stream().map(this::toDTO).toList();
    }

    public TapProfileGroupDTO getGroup(Long id) {
        return toDTO(findOrThrow(id));
    }

    public PageResponseDTO<TapProfileGroupDTO> searchGroups(TapProfileGroupSearchDTO criteria, Pageable pageable) {
        Page<TapProfileGroup> page = groupRepo.search(criteria.getName(), criteria.getDescription(), criteria.getIsActive(), pageable);
        return toPageResponse(page.map(this::toDTO));
    }

    @Transactional
    public TapProfileGroupDTO createGroup(TapProfileGroupDTO req) {
        log.info("Creating TAP profile group: {}", req.getName());
        if (groupRepo.existsByNameIgnoreCaseAndIsDeletedFalse(req.getName())) {
            throw new DuplicateNameException("TapProfileGroup", req.getName());
        }
        validateNoDuplicateProfileIds(req.getTapProfileIds());
        TapProfileGroup group = TapProfileGroup.builder()
                .name(req.getName())
                .description(req.getDescription())
                .isActive(req.isActive())
                .tapProfiles(resolveProfiles(req.getTapProfileIds()))
                .build();
        return toDTO(groupRepo.save(group));
    }

    @Transactional
    public TapProfileGroupDTO updateGroup(Long id, TapProfileGroupDTO req) {
        log.info("Updating TAP profile group ID: {}", id);
        TapProfileGroup group = findOrThrow(id);
        if (!group.getName().equalsIgnoreCase(req.getName()) &&
                groupRepo.existsByNameIgnoreCaseAndIsDeletedFalse(req.getName())) {
            throw new DuplicateNameException("TapProfileGroup", req.getName());
        }
        validateNoDuplicateProfileIds(req.getTapProfileIds());
        group.setName(req.getName());
        group.setDescription(req.getDescription());
        group.setActive(req.isActive());
        updateProfiles(group, req.getTapProfileIds());
        return toDTO(groupRepo.save(group));
    }

    @Transactional
    public void deleteGroup(Long id) {
        TapProfileGroup group = findOrThrow(id);
        if (groupRepo.existsActivePartnerByGroupId(id)) {
            throw new ForeignReferenceException(
                    "TapProfileGroup cannot be deleted because it is assigned to an active partner.");
        }
        groupRepo.delete(group);
    }

    private TapProfileGroup findOrThrow(Long id) {
        return groupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TapProfileGroup not found: " + id));
    }

    private List<TapProfile> resolveProfiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return ids.stream()
                .map(pid -> profileRepo.findById(pid)
                        .orElseThrow(() -> new ResourceNotFoundException("TapProfile not found: " + pid)))
                .collect(Collectors.toList());
    }

    private void validateNoDuplicateProfileIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        Set<Long> seen = new java.util.HashSet<>();
        List<Long> duplicates = ids.stream()
                .filter(pid -> !seen.add(pid))
                .distinct()
                .toList();
        if (!duplicates.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate TAP profile IDs in request: " + duplicates);
        }
    }

    /**
     * Diff-based update — removes profiles no longer in the request,
     * keeps existing ones, adds new ones. Mirrors ProductPlanService update logic.
     */
    private void updateProfiles(TapProfileGroup group, List<Long> incomingIds) {
        List<TapProfile> existing = group.getTapProfiles();
        Set<Long> incomingSet = incomingIds == null ? Set.of()
                : new java.util.HashSet<>(incomingIds);
        Set<Long> existingIds = existing.stream()
                .map(TapProfile::getId).collect(Collectors.toSet());

        // Remove profiles no longer in request
        existing.removeIf(p -> !incomingSet.contains(p.getId()));

        // Add profiles not already present
        if (incomingIds != null) {
            incomingIds.stream()
                    .filter(pid -> !existingIds.contains(pid))
                    .forEach(pid -> existing.add(
                            profileRepo.findById(pid)
                                    .orElseThrow(() -> new ResourceNotFoundException("TapProfile not found: " + pid))));
        }
    }

    private TapProfileGroupDTO toDTO(TapProfileGroup g) {
        TapProfileGroupDTO dto = new TapProfileGroupDTO();
        dto.setId(g.getId());
        dto.setName(g.getName());
        dto.setDescription(g.getDescription());
        dto.setActive(g.isActive());
        dto.setTapProfileIds(g.getTapProfiles().stream().map(TapProfile::getId).toList());
        dto.setTapProfiles(g.getTapProfiles().stream().map(p -> {
            TapProfileDTO pd = new TapProfileDTO();
            pd.setId(p.getId());
            pd.setProfileName(p.getProfileName());
            pd.setDescription(p.getDescription());
            pd.setActive(p.isActive());
            pd.setServiceType(p.getServiceType());
            pd.setMappingCount(p.getFieldMappings().size());
            return pd;
        }).toList());
        return dto;
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
