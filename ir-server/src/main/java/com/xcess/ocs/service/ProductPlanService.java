package com.xcess.ocs.service;

import com.xcess.ocs.cache.ProductPlanCache;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ProductPlanAssociationDTO;
import com.xcess.ocs.dto.ProductPlanDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.DuplicateRatePackageException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.ProductPlanMapper;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.repository.ProductPlanAssociationRepository;
import com.xcess.ocs.repository.ProductPlanRepository;
import com.xcess.ocs.repository.RatePackageGroupRepository;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductPlanService {
    private final ProductPlanRepository productPlanRepository;
    private final RatePackageGroupRepository ratePackageGroupRepository;

    @Autowired
    private final AccountRepository accountRepository;

    @Autowired
    private final ProductPlanAssociationRepository productPlanAssociationRepository;

    private final ProductPlanCache productPlanCache;

    @Transactional
    public ProductPlanDTO createProductPlan(ProductPlanDTO dto) {
        log.info("Creating new product plan with name: {}", dto.getName());

        if (productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            log.warn("Product plan name already exists: {}", dto.getName());
            throw new DuplicateNameException("Product Plan", dto.getName());
        }

        ProductPlan plan = ProductPlan.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .packageType(ProductPlan.PackageType.fromString(dto.getPackageType()))
                .build();

        List<ProductPlanAssociationDTO> ratePackageGroups = dto.getRatePackageGroups();

        List<String> duplicateGroupNames = new ArrayList<>();
        Set<Long> seenGroupIds = new HashSet<>();
        for (ProductPlanAssociationDTO assocDto : ratePackageGroups) {
            Long groupId = assocDto.getRatePackageGroupId();
            if (seenGroupIds.contains(groupId)) {
                String groupName = ratePackageGroupRepository.findById(groupId)
                        .map(RatePackageGroup::getName)
                        .orElse("ID: " + groupId);
                duplicateGroupNames.add(groupName);
            } else {
                seenGroupIds.add(groupId);
            }
        }

        if (!duplicateGroupNames.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate rate package groups found: " + String.join(", ", duplicateGroupNames));
        }

        Set<ServiceType> validServiceTypes = Set.of(ServiceType.VOICE, ServiceType.SMS, ServiceType.USAGE);
        List<ProductPlanAssociation> associations = new ArrayList<>();
        for (ProductPlanAssociationDTO assocDto : ratePackageGroups) {
            RatePackageGroup group = ratePackageGroupRepository.findById(assocDto.getRatePackageGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rate Package Group not found with ID: " + assocDto.getRatePackageGroupId()));

            if (assocDto.getServiceType() == null || assocDto.getServiceType().isBlank()) {
                throw new IllegalArgumentException("Service type is required for each association");
            }

            ServiceType serviceType;
            try {
                serviceType = ServiceType.valueOf(assocDto.getServiceType());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid service type: " + assocDto.getServiceType()
                        + ". Valid values: " + validServiceTypes);
            }

            if (!serviceType.equals(group.getServiceType())) {
                throw new IllegalArgumentException("Association service type '" + serviceType
                        + "' does not match group '" + group.getName() + "' service type '" + group.getServiceType() + "'");
            }

            ProductPlanAssociation association = ProductPlanAssociation.builder()
                    .productPlan(plan)
                    .ratePackageGroup(group)
                    .serviceType(serviceType)
                    .build();

            associations.add(association);
        }

        plan.setRatePackageGroups(associations);
        plan = productPlanRepository.save(plan);
        log.info("Successfully created product plan with ID: {}", plan.getProductPlanId());

        productPlanCache.addToCache(ProductPlanMapper.toDTO(plan));

        return ProductPlanMapper.toDTO(plan);
    }

    public List<ProductPlanDTO> getAllProductPlans() {
        log.debug("Fetching all product plans");
        List<ProductPlanDTO> plans = productPlanRepository.findAll().stream()
                .map(ProductPlanMapper::toDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} product plans", plans.size());
        return plans;
    }

    public PageResponseDTO<ProductPlanDTO> getProductPlansInPages(Pageable pageable) {
        log.debug("Fetching product plans in pages");
        Page<ProductPlan> productPlans = productPlanRepository.findAll(pageable);
        List<ProductPlanDTO> productPlanDTOs = productPlans.getContent().stream()
                .map(ProductPlanMapper::toDTO)
                .toList();

        log.debug("Retrieved {} product plans in a page", productPlanDTOs.size());
        return PaginationUtils.buildGetResponseDTO(productPlanDTOs, productPlans);
    }

    public PageResponseDTO<ProductPlanDTO> searchProductPlans(String searchTerm, String packageType, Pageable pageable) {
        log.info("Searching product plans with searchTerm: {} and packageType: {}", searchTerm, packageType);

        ProductPlan.PackageType type = packageType != null ? ProductPlan.PackageType.fromString(packageType) : null;

        Page<ProductPlan> productPlans = productPlanRepository.searchProductPlans(searchTerm, type, pageable);

        List<ProductPlanDTO> planDTOs = productPlans.getContent().stream()
                .map(ProductPlanMapper::toDTO)
                .toList();

        return PaginationUtils.buildGetResponseDTO(planDTOs, productPlans);
    }

    public ProductPlanDTO getProductPlanById(Long id) {
        log.debug("Fetching product plan with ID: {}", id);
        ProductPlan plan = productPlanRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product plan not found with ID: {}", id);
                    return new ResourceNotFoundException("Product Plan not found with ID: " + id);
                });
        return ProductPlanMapper.toDTO(plan);
    }

    @Transactional
    public ProductPlanDTO updateProductPlan(Long id, ProductPlanDTO dto) {
        log.info("Updating product plan with ID: {}", id);
        ProductPlan existingPlan = productPlanRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Product plan not found with ID: {}", id);
                    return new ResourceNotFoundException("Product Plan not found with ID: " + id);
                });

        if (!existingPlan.getName().equalsIgnoreCase(dto.getName()) &&
                productPlanRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            log.warn("Product plan name already exists: {}", dto.getName());
            throw new DuplicateNameException("Product Plan", dto.getName());
        }

        existingPlan.setName(dto.getName());
        existingPlan.setDescription(dto.getDescription());
        existingPlan.setPackageType(ProductPlan.PackageType.fromString(dto.getPackageType()));

        List<ProductPlanAssociationDTO> newAssociationsList = dto.getRatePackageGroups();

        List<String> duplicateGroupNames = new ArrayList<>();
        Set<Long> seenGroupIds = new HashSet<>();
        for (ProductPlanAssociationDTO assocDto : newAssociationsList) {
            Long groupId = assocDto.getRatePackageGroupId();
            if (seenGroupIds.contains(groupId)) {
                String groupName = ratePackageGroupRepository.findById(groupId)
                        .map(RatePackageGroup::getName)
                        .orElse("ID: " + groupId);
                duplicateGroupNames.add(groupName);
            } else {
                seenGroupIds.add(groupId);
            }
        }

        if (!duplicateGroupNames.isEmpty()) {
            throw new DuplicateRatePackageException(
                    "Duplicate rate package groups found: " + String.join(", ", duplicateGroupNames));
        }

        List<ProductPlanAssociation> existingAssociations = existingPlan.getRatePackageGroups();
        Set<Long> newGroupIds = newAssociationsList.stream()
                .map(ProductPlanAssociationDTO::getRatePackageGroupId)
                .collect(Collectors.toSet());

        List<ProductPlanAssociation> toDelete = existingAssociations.stream()
                .filter(existing -> !newGroupIds.contains(existing.getRatePackageGroup().getRatePackageGroupId()))
                .toList();

        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(ProductPlanAssociation::getId).toList();
            productPlanAssociationRepository.softDeleteAllByIds(deleteIds, LocalDateTime.now());
            existingAssociations.removeAll(toDelete);
            log.debug("Soft deleted {} product plan associations", deleteIds.size());
        }

        Map<Long, ProductPlanAssociation> existingMap = existingAssociations.stream()
                .collect(Collectors.toMap(
                        assoc -> assoc.getRatePackageGroup().getRatePackageGroupId(),
                        assoc -> assoc
                ));

        Set<ServiceType> validServiceTypes = Set.of(ServiceType.VOICE, ServiceType.SMS, ServiceType.USAGE);
        for (ProductPlanAssociationDTO assocDto : newAssociationsList) {
            Long groupId = assocDto.getRatePackageGroupId();

            if (existingMap.containsKey(groupId)) {
                ProductPlanAssociation existingAssoc = existingMap.get(groupId);

                if (assocDto.getServiceType() != null && !assocDto.getServiceType().isBlank()) {
                    ServiceType serviceType;
                    try {
                        serviceType = ServiceType.valueOf(assocDto.getServiceType());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid service type: " + assocDto.getServiceType()
                                + ". Valid values: " + validServiceTypes);
                    }

                    RatePackageGroup group = existingAssoc.getRatePackageGroup();
                    if (!serviceType.equals(group.getServiceType())) {
                        throw new IllegalArgumentException("Association service type '" + serviceType
                                + "' does not match group '" + group.getName() + "' service type '" + group.getServiceType() + "'");
                    }
                    existingAssoc.setServiceType(serviceType);
                }
            } else {
                RatePackageGroup group = ratePackageGroupRepository.findById(groupId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Rate Package Group not found with ID: " + groupId));

                if (assocDto.getServiceType() == null || assocDto.getServiceType().isBlank()) {
                    throw new IllegalArgumentException("Service type is required for each association");
                }

                ServiceType serviceType;
                try {
                    serviceType = ServiceType.valueOf(assocDto.getServiceType());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid service type: " + assocDto.getServiceType()
                            + ". Valid values: " + validServiceTypes);
                }

                if (!serviceType.equals(group.getServiceType())) {
                    throw new IllegalArgumentException("Association service type '" + serviceType
                            + "' does not match group '" + group.getName() + "' service type '" + group.getServiceType() + "'");
                }

                ProductPlanAssociation association = ProductPlanAssociation.builder()
                        .productPlan(existingPlan)
                        .ratePackageGroup(group)
                        .serviceType(serviceType)
                        .build();

                existingAssociations.add(association);
            }
        }

        existingPlan = productPlanRepository.save(existingPlan);
        log.info("Successfully updated product plan with ID: {}", id);

        productPlanCache.updateCache(ProductPlanMapper.toDTO(existingPlan));

        return ProductPlanMapper.toDTO(existingPlan);
    }

    @Transactional
    public void deleteProductPlan(Long id) {
        log.info("Deleting product plan with ID: {}", id);
        if (!productPlanRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent product plan with ID: {}", id);
            throw new ResourceNotFoundException("Product Plan not found with ID: " + id);
        }
        boolean isReferenced = accountRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete product plan with ID: {} that is referenced by an active account", id);
            throw new ForeignReferenceException("Product Plan cannot be soft deleted because it is referenced by an active account.");
        }
        isReferenced = productPlanAssociationRepository.existsByProductPlan_ProductPlanIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete product plan with ID: {} that is referenced by an active product plan association", id);
            throw new ForeignReferenceException("Product Plan cannot be soft deleted because it is referenced by an active Association.");
        }
        productPlanRepository.deleteById(id);
        log.info("Successfully deleted product plan with ID: {}", id);

        productPlanCache.removeFromCache(id);
    }

    public List<Map<String, Object>> getProductPlansByPartnerType(String partnerType) {
        log.debug("Fetching product plans for partner type: {}", partnerType);

        List<ProductPlan> productPlans;
        if (com.xcess.ocs.entity.PartnerType.CUSTOMER.name().equalsIgnoreCase(partnerType)) {
            productPlans = productPlanRepository.findByPackageTypeAndIsDeletedFalse(ProductPlan.PackageType.SELLING);
            log.debug("Fetched {} SELLING product plans for CUSTOMER", productPlans.size());
        } else if (com.xcess.ocs.entity.PartnerType.VENDOR.name().equalsIgnoreCase(partnerType)) {
            productPlans = productPlanRepository.findByPackageTypeAndIsDeletedFalse(ProductPlan.PackageType.BUYING);
            log.debug("Fetched {} BUYING product plans for VENDOR", productPlans.size());
        } else {
            productPlans = productPlanRepository.findAll();
            log.debug("Fetched {} all product plans for BOTH", productPlans.size());
        }

        return productPlans.stream()
                .map(plan -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productPlanId", plan.getProductPlanId());
                    map.put("productPlanName", plan.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
