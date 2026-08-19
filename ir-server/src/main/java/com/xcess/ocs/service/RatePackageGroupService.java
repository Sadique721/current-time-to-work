package com.xcess.ocs.service;

import com.xcess.ocs.cache.RatePackageGroupCache;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.RatePackageAssociationDTO;
import com.xcess.ocs.dto.RatePackageGroupDTO;
import com.xcess.ocs.dto.request.ReorderPackagesRequestDTO;

import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageAssociation;
import com.xcess.ocs.entity.RatePackageGroup;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.DuplicatePriorityException;
import com.xcess.ocs.exception.DuplicateRatePackageException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.RatePackageGroupMapper;
import com.xcess.ocs.repository.ProductPlanAssociationRepository;
import com.xcess.ocs.repository.RatePackageAssociationRepository;
import com.xcess.ocs.repository.RatePackageGroupRepository;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatePackageGroupService {
    private final RatePackageGroupRepository ratePackageGroupRepository;
    private final RatePackageRepository ratePackageRepository;
    private final RatePackageGroupCache ratePackageGroupCache;

    @Autowired
    private final ProductPlanAssociationRepository productPlanAssociationRepository;

    @Autowired
    private final RatePackageAssociationRepository ratePackageAssociationRepository;

    @Transactional
    public RatePackageGroupDTO createRatePackageGroup(RatePackageGroupDTO dto) {
        log.info("Creating new rate package group with name: {}", dto.getRatePackageGroupName());

        // Check if name already exists
        if (ratePackageGroupRepository.existsByNameAndIsDeletedFalse(dto.getRatePackageGroupName())) {
            log.warn("Rate package group name already exists: {}", dto.getRatePackageGroupName());
            throw new DuplicateNameException("Rate Package Group", dto.getRatePackageGroupName());
        }

        // Create and save the group
        RatePackageGroup group = RatePackageGroup.builder()
                .name(dto.getRatePackageGroupName())
                .description(dto.getDescription())
                .packageType(RatePackageGroup.PackageType.fromString(dto.getPackageType()))
                .serviceType(dto.getServiceType())
                .ratePackageSelectionType(dto.getRatePackageSelectionType() != null ?
                        RatePackageGroup.RatePackageSelectionType.valueOf(dto.getRatePackageSelectionType()) : null)
                .build();

        // Create associations
        log.debug("Adding {} rate packages to group", dto.getRatePackages().size());

        List<RatePackageAssociationDTO> ratePackages = dto.getRatePackages();

        // Step 1: Check for duplicate rate packages within request
        List<String> duplicatePackageNames = new ArrayList<>();
        Set<Long> seenPackageIds = new HashSet<>();
        for (RatePackageAssociationDTO assocDto : ratePackages) {
            Long ratePackageId = assocDto.getRatePackage();
            if (seenPackageIds.contains(ratePackageId)) {
                String packageName = ratePackageRepository.findById(ratePackageId)
                        .map(RatePackage::getPackageName)
                        .orElse("ID: " + ratePackageId);
                duplicatePackageNames.add(packageName);
            } else {
                seenPackageIds.add(ratePackageId);
            }
        }

        if (!duplicatePackageNames.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate rate packages found: " + String.join(", ", duplicatePackageNames));
        }

        // Branch validation based on ratePackageSelectionType
        RatePackageGroup.RatePackageSelectionType selectionType = group.getRatePackageSelectionType();

        if (selectionType == RatePackageGroup.RatePackageSelectionType.PRIORITY) {
            Set<Integer> priorities = new HashSet<>();
            for (RatePackageAssociationDTO assocDto : ratePackages) {
                if (assocDto.getPriority() != null) {
                    if (!priorities.add(assocDto.getPriority())) {
                        throw new DuplicatePriorityException("RatePackageGroup", "priority");
                    }
                }
                if (assocDto.getCallType() != null) {
                    throw new IllegalArgumentException("callType must be null when selection type is PRIORITY");
                }
                if (assocDto.getExpression() != null) {
                    throw new IllegalArgumentException("expression must be null when selection type is PRIORITY");
                }
            }
            if (!priorities.isEmpty()) {
                Set<Integer> expectedPriorities = new HashSet<>();
                for (int i = 1; i <= ratePackages.size(); i++) {
                    expectedPriorities.add(i);
                }
                if (!priorities.equals(expectedPriorities)) {
                    throw new IllegalArgumentException("Priorities must be sequential starting from 1");
                }
            }
        } else if (selectionType == RatePackageGroup.RatePackageSelectionType.CALL_TYPE) {
            for (RatePackageAssociationDTO assocDto : ratePackages) {
                if (assocDto.getCallType() == null) {
                    throw new IllegalArgumentException("callType is required when selection type is CALL_TYPE");
                }
                if (assocDto.getPriority() != null) {
                    throw new IllegalArgumentException("priority must be null when selection type is CALL_TYPE");
                }
                if (assocDto.getExpression() != null) {
                    throw new IllegalArgumentException("expression must be null when selection type is CALL_TYPE");
                }
            }
        } else if (selectionType == RatePackageGroup.RatePackageSelectionType.EXPRESSION) {
            for (RatePackageAssociationDTO assocDto : ratePackages) {
                if (assocDto.getExpression() == null || assocDto.getExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("expression is required when selection type is EXPRESSION");
                }
                if (assocDto.getPriority() != null) {
                    throw new IllegalArgumentException("priority must be null when selection type is EXPRESSION");
                }
                if (assocDto.getCallType() != null) {
                    throw new IllegalArgumentException("callType must be null when selection type is EXPRESSION");
                }
            }
        }

        List<RatePackageAssociation> associations = new ArrayList<>();
        for (int i = 0; i < ratePackages.size(); i++) {
            RatePackageAssociationDTO assocDto = ratePackages.get(i);

            RatePackage ratePackage = ratePackageRepository.findById(assocDto.getRatePackage())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rate Package not found with ID: " + assocDto.getRatePackage()));

            Integer priority = null;
            if (selectionType == RatePackageGroup.RatePackageSelectionType.PRIORITY) {
                priority = (assocDto.getPriority() != null) ? assocDto.getPriority() : (i + 1);
            }

            RatePackageAssociation.RatePackageAssociationBuilder builder = RatePackageAssociation.builder()
                    .ratePackageGroup(group)
                    .ratePackage(ratePackage)
                    .startTime(assocDto.getStartTime())
                    .endTime(assocDto.getEndTime())
                    .expression(assocDto.getExpression())
                    .priority(priority)
                    .callType(assocDto.getCallType());

            associations.add(builder.build());
        }

        group.setRatePackageAssociations(associations);
        group = ratePackageGroupRepository.save(group);
        log.info("Successfully created rate package group with ID: {}", group.getRatePackageGroupId());

        ratePackageGroupCache.updateCache(group);

        return RatePackageGroupMapper.toDTO(group);
    }

    public List<RatePackageGroupDTO> getAllRatePackageGroups() {
        log.debug("Fetching all rate package groups");
        List<RatePackageGroupDTO> groups = ratePackageGroupRepository.findAll().stream()
                .map(RatePackageGroupMapper::toDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} rate package groups", groups.size());
        return groups;
    }

    public PageResponseDTO<RatePackageGroupDTO> getRatePackageGroupsInPages(Pageable pageable) {
        log.debug("Fetching rate package groups in pages");
        Page<RatePackageGroup> ratePackageGroups = ratePackageGroupRepository.findAll(pageable);
        List<RatePackageGroupDTO> ratePackageGroupDTOs = ratePackageGroups.getContent().stream()
                .map(RatePackageGroupMapper::toDTO)
                .toList();

        log.debug("Retrieved {} rate package groups in a page", ratePackageGroupDTOs.size());
        return PaginationUtils.buildGetResponseDTO(ratePackageGroupDTOs, ratePackageGroups);
    }

    public PageResponseDTO<RatePackageGroupDTO> searchRatePackageGroups(String searchTerm, String packageType, Pageable pageable) {
        log.info("Searching rate package groups with searchTerm: {} and packageType: {}", searchTerm, packageType);

        RatePackageGroup.PackageType type = packageType != null ? RatePackageGroup.PackageType.fromString(packageType) : null;

        Page<RatePackageGroup> ratePackageGroups = ratePackageGroupRepository.searchRatePackageGroups(searchTerm, type, pageable);

        List<RatePackageGroupDTO> groupDTOs = ratePackageGroups.getContent().stream()
                .map(RatePackageGroupMapper::toDTO)
                .toList();

        return PaginationUtils.buildGetResponseDTO(groupDTOs, ratePackageGroups);
    }

    public RatePackageGroupDTO getRatePackageGroupById(Long id) {
        log.debug("Fetching rate package group with ID: {}", id);
        RatePackageGroup group = ratePackageGroupRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rate package group not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate Package Group not found with ID: " + id);
                });
        return RatePackageGroupMapper.toDTO(group);
    }

    @Transactional
    public RatePackageGroupDTO updateRatePackageGroup(Long id, RatePackageGroupDTO dto) {
        log.info("Updating rate package group with ID: {}", id);
        RatePackageGroup existingGroup = ratePackageGroupRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rate package group not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate Package Group not found with ID: " + id);
                });

        // Check if new name conflicts with other groups
        if (!existingGroup.getName().equalsIgnoreCase(dto.getRatePackageGroupName()) &&
                ratePackageGroupRepository.existsByNameAndIsDeletedFalse(dto.getRatePackageGroupName())) {
            log.warn("Rate package group name already exists: {}", dto.getRatePackageGroupName());
            throw new DuplicateNameException("Rate Package Group", dto.getRatePackageGroupName());
        }

        // Update basic fields
        existingGroup.setName(dto.getRatePackageGroupName());
        existingGroup.setDescription(dto.getDescription());
        existingGroup.setPackageType(RatePackageGroup.PackageType.fromString(dto.getPackageType()));
        existingGroup.setServiceType(dto.getServiceType());
        existingGroup.setRatePackageSelectionType(dto.getRatePackageSelectionType() != null ?
                RatePackageGroup.RatePackageSelectionType.valueOf(dto.getRatePackageSelectionType()) : null);

        // Diff-based update: Compare existing vs. new associations
        List<RatePackageAssociationDTO> newAssociationsList = dto.getRatePackages();

        // Step 1: Check for duplicate rate packages within request
        List<String> duplicatePackageNames = new ArrayList<>();
        Set<Long> seenPackageIds = new HashSet<>();

        for (RatePackageAssociationDTO assocDto : newAssociationsList) {
            Long ratePackageId = assocDto.getRatePackage();
            if (seenPackageIds.contains(ratePackageId)) {
                String packageName = ratePackageRepository.findById(ratePackageId)
                        .map(RatePackage::getPackageName)
                        .orElse("ID: " + ratePackageId);
                duplicatePackageNames.add(packageName);
            } else {
                seenPackageIds.add(ratePackageId);
            }
        }

        if (!duplicatePackageNames.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate rate packages found: " + String.join(", ", duplicatePackageNames));
        }

        // Branch validation based on ratePackageSelectionType
        RatePackageGroup.RatePackageSelectionType selectionType = existingGroup.getRatePackageSelectionType();

        if (selectionType == RatePackageGroup.RatePackageSelectionType.PRIORITY) {
            Set<Integer> priorities = new HashSet<>();
            for (RatePackageAssociationDTO assocDto : newAssociationsList) {
                if (assocDto.getPriority() != null) {
                    if (!priorities.add(assocDto.getPriority())) {
                        throw new DuplicatePriorityException("RatePackageGroup", "priority");
                    }
                }
                if (assocDto.getCallType() != null) {
                    throw new IllegalArgumentException("callType must be null when selection type is PRIORITY");
                }
                if (assocDto.getExpression() != null) {
                    throw new IllegalArgumentException("expression must be null when selection type is PRIORITY");
                }
            }
            if (!priorities.isEmpty()) {
                Set<Integer> expectedPriorities = new HashSet<>();
                for (int i = 1; i <= newAssociationsList.size(); i++) {
                    expectedPriorities.add(i);
                }
                if (!priorities.equals(expectedPriorities)) {
                    throw new IllegalArgumentException("Priorities must be sequential starting from 1");
                }
            }
        } else if (selectionType == RatePackageGroup.RatePackageSelectionType.CALL_TYPE) {
            for (RatePackageAssociationDTO assocDto : newAssociationsList) {
                if (assocDto.getCallType() == null) {
                    throw new IllegalArgumentException("callType is required when selection type is CALL_TYPE");
                }
                if (assocDto.getPriority() != null) {
                    throw new IllegalArgumentException("priority must be null when selection type is CALL_TYPE");
                }
                if (assocDto.getExpression() != null) {
                    throw new IllegalArgumentException("expression must be null when selection type is CALL_TYPE");
                }
            }
        } else if (selectionType == RatePackageGroup.RatePackageSelectionType.EXPRESSION) {
            for (RatePackageAssociationDTO assocDto : newAssociationsList) {
                if (assocDto.getExpression() == null || assocDto.getExpression().trim().isEmpty()) {
                    throw new IllegalArgumentException("expression is required when selection type is EXPRESSION");
                }
                if (assocDto.getPriority() != null) {
                    throw new IllegalArgumentException("priority must be null when selection type is EXPRESSION");
                }
                if (assocDto.getCallType() != null) {
                    throw new IllegalArgumentException("callType must be null when selection type is EXPRESSION");
                }
            }
        }

        List<RatePackageAssociation> existingAssociations = existingGroup.getRatePackageAssociations();
        Set<Long> newPackageIds = newAssociationsList.stream()
                .map(RatePackageAssociationDTO::getRatePackage)
                .collect(Collectors.toSet());

        // Step 3: Delete - Remove associations no longer in request (soft delete)
        List<RatePackageAssociation> toDelete = existingAssociations.stream()
                .filter(existing -> !newPackageIds.contains(existing.getRatePackage().getRatePackageId()))
                .toList();
        
        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(RatePackageAssociation::getId).toList();
            ratePackageAssociationRepository.softDeleteAllByIds(deleteIds, LocalDateTime.now());
            existingAssociations.removeAll(toDelete);
            log.debug("Soft deleted {} rate package associations", deleteIds.size());
        }

        // Step 4: Update existing associations that are still in request
        Map<Long, RatePackageAssociation> existingMap = existingAssociations.stream()
                .collect(Collectors.toMap(
                        assoc -> assoc.getRatePackage().getRatePackageId(),
                        assoc -> assoc
                ));

        for (int i = 0; i < newAssociationsList.size(); i++) {
            RatePackageAssociationDTO assocDto = newAssociationsList.get(i);
            Long ratePackageId = assocDto.getRatePackage();

            if (existingMap.containsKey(ratePackageId)) {
                // Update existing association
                RatePackageAssociation existingAssoc = existingMap.get(ratePackageId);

                Integer priority = null;
                if (selectionType == RatePackageGroup.RatePackageSelectionType.PRIORITY) {
                    priority = (assocDto.getPriority() != null) ? assocDto.getPriority() : (i + 1);
                }
                existingAssoc.setPriority(priority);
                existingAssoc.setCallType(assocDto.getCallType());

                existingAssoc.setStartTime(assocDto.getStartTime());
                existingAssoc.setEndTime(assocDto.getEndTime());
                existingAssoc.setExpression(assocDto.getExpression());
            } else {
                // Create new association
                RatePackage ratePackage = ratePackageRepository.findById(ratePackageId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Rate Package not found with ID: " + ratePackageId));

                Integer priority = null;
                if (selectionType == RatePackageGroup.RatePackageSelectionType.PRIORITY) {
                    priority = (assocDto.getPriority() != null) ? assocDto.getPriority() : (i + 1);
                }

                RatePackageAssociation.RatePackageAssociationBuilder builder = RatePackageAssociation.builder()
                        .ratePackageGroup(existingGroup)
                        .ratePackage(ratePackage)
                        .startTime(assocDto.getStartTime())
                        .endTime(assocDto.getEndTime())
                        .expression(assocDto.getExpression())
                        .priority(priority)
                        .callType(assocDto.getCallType());

                existingAssociations.add(builder.build());
            }
        }

        RatePackageGroup savedGroup = ratePackageGroupRepository.save(existingGroup);
        log.info("Successfully updated rate package group with ID: {}", id);

        ratePackageGroupCache.updateCache(savedGroup);

        return RatePackageGroupMapper.toDTO(savedGroup);
    }

    @Transactional
    public void deleteRatePackageGroup(Long id) {
        log.info("Deleting rate package group with ID: {}", id);
        if (!ratePackageGroupRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent rate package group with ID: {}", id);
            throw new ResourceNotFoundException("Rate Package Group not found with ID: " + id);
        }
        // First, check if there is any active rate package association using this rate package group
        boolean isReferenced = ratePackageAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete rate package group with ID: {} that is referenced by an active rate package association", id);
            throw new ForeignReferenceException("Rate Package Group cannot be soft deleted because it is referenced by an active Association.");
        }
        // First, check if there is any active product plan association using this rate package group
         isReferenced = productPlanAssociationRepository.existsByRatePackageGroup_RatePackageGroupIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete rate package group with ID: {} that is referenced by an active product plan association", id);
            throw new ForeignReferenceException("Rate Package Group cannot be soft deleted because it is referenced by an active Association.");
        }
        ratePackageGroupRepository.deleteById(id);
        log.info("Successfully deleted rate package group with ID: {}", id);

        ratePackageGroupCache.removeFromCache(id);
    }

    public List<Map<String, Object>> getPackageIdAndNameByType(RatePackageGroup.PackageType packageType) {
        log.info("Fetching package Group ids and names for type: {}", packageType);
        return ratePackageGroupRepository.findIdAndNameByType(packageType);
    }

    public Map<String, List<Map<String, Object>>> getRatePackageGroupsByTypeAndService(String type) {
        log.info("Fetching rate package groups with type: {}", type);
        RatePackageGroup.PackageType packageType = RatePackageGroup.PackageType.fromString(type);
        List<Map<String, Object>> groups = ratePackageGroupRepository.findIdAndNameByType(packageType);

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("voice", new ArrayList<>());
        result.put("sms", new ArrayList<>());
        result.put("usage", new ArrayList<>());

        for (Map<String, Object> group : groups) {
            Object serviceTypeObj = group.get("serviceType");
            if (serviceTypeObj != null) {
                String serviceTypeKey = serviceTypeObj.toString().toLowerCase();
                if (result.containsKey(serviceTypeKey)) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("packageId", group.get("ratePackageGroupId"));
                    entry.put("packageName", group.get("ratePackageGroupName"));
                    entry.put("serviceType", serviceTypeObj.toString());
                    result.get(serviceTypeKey).add(entry);
                }
            }
        }

        return result;
    }

    @Transactional
    public void reorderPackages(Long groupId, ReorderPackagesRequestDTO request) {
        log.info("Reordering rate packages for group ID: {}", groupId);
        
        RatePackageGroup group = ratePackageGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Rate Package Group not found with ID: " + groupId));
        
        List<ReorderPackagesRequestDTO.PackageOrder> packages = request.getPackages();
        List<RatePackageAssociation> existingAssociations = group.getRatePackageAssociations();

        if (group.getRatePackageSelectionType() != RatePackageGroup.RatePackageSelectionType.PRIORITY) {
            throw new IllegalArgumentException("Reordering is only supported for groups with PRIORITY selection type");
        }
        
        // Step 1: Check count matches (request must contain all packages)
        if (packages.size() != existingAssociations.size()) {
            throw new IllegalArgumentException("Request must contain all packages in the group");
        }
        
        // Step 2: Build maps for validation
        Map<Long, ReorderPackagesRequestDTO.PackageOrder> packageOrderMap = packages.stream()
                .collect(Collectors.toMap(ReorderPackagesRequestDTO.PackageOrder::getRatePackageId, p -> p));
        
        Set<Long> existingPackageIds = existingAssociations.stream()
                .map(assoc -> assoc.getRatePackage().getRatePackageId())
                .collect(Collectors.toSet());
        
        // Step 3: Verify all DTO package IDs exist in group
        Set<Long> requestPackageIds = packages.stream()
                .map(ReorderPackagesRequestDTO.PackageOrder::getRatePackageId)
                .collect(Collectors.toSet());
        
        if (!requestPackageIds.equals(existingPackageIds)) {
            throw new ResourceNotFoundException("One or more package IDs not found in the group");
        }
        
        // Step 4: Check for duplicate package IDs in DTO
        if (requestPackageIds.size() != packages.size()) {
            throw new DuplicateRatePackageException("Duplicate rate packages found in request");
        }
        
        // Step 5: Check for duplicate priorities in DTO
        Set<Integer> priorities = packages.stream()
                .map(ReorderPackagesRequestDTO.PackageOrder::getPriority)
                .collect(Collectors.toSet());
        
        if (priorities.size() != packages.size()) {
            throw new DuplicatePriorityException("RatePackageGroup", "priority");
        }
        
        // Step 6: Check priorities are sequential (1 to N)
        int expectedCount = packages.size();
        Set<Integer> expectedPriorities = new HashSet<>();
        for (int i = 1; i <= expectedCount; i++) {
            expectedPriorities.add(i);
        }
        
        if (!priorities.equals(expectedPriorities)) {
            throw new IllegalArgumentException("Priorities must be sequential starting from 1");
        }
        
        // Step 7: Direct update (all validations passed)
        for (RatePackageAssociation association : existingAssociations) {
            Long packageId = association.getRatePackage().getRatePackageId();
            ReorderPackagesRequestDTO.PackageOrder order = packageOrderMap.get(packageId);

            association.setPriority(order.getPriority());
        }
        
        ratePackageGroupRepository.save(group);
        ratePackageGroupCache.updateCache(group);
        
        log.info("Successfully reordered {} packages for group ID: {}", packages.size(), groupId);
    }
}