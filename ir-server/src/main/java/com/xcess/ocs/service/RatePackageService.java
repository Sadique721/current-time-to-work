package com.xcess.ocs.service;

import com.xcess.ocs.cache.RatePackageCache;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.RatePackageSearchDTO;
import com.xcess.ocs.entity.Pulse;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceConflictException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.mapper.RatePackageMapper;
import com.xcess.ocs.repository.PulseRepository;
import com.xcess.ocs.repository.RatePackageAssociationRepository;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.dto.RatePackageDTO;
import com.xcess.ocs.util.PaginationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RatePackageService {
    private final RatePackageRepository ratePackageRepository;
    private final PulseRepository pulseRepository;
    private final RatePackageCache ratePackageCache;

    @Autowired
    private RatePackageAssociationRepository ratePackageAssociationRepository;

    @Autowired
    private RateDetailsService rateDetailsService;

    @Autowired
    public RatePackageService(RatePackageRepository ratePackageRepository, PulseRepository pulseRepository, RatePackageCache ratePackageCache) {
        this.ratePackageRepository = ratePackageRepository;
        this.pulseRepository = pulseRepository;
        // this.ratePackageMapper = ratePackageMapper;
        this.ratePackageCache = ratePackageCache;
    }

    @Transactional
    public RatePackageDTO createRatePackage(RatePackageDTO ratePackageDTO) {
        log.info("Creating new rate package with name: {}", ratePackageDTO.getPackageName());

        if (ratePackageRepository.existsByPackageNameAndIsDeletedFalse(ratePackageDTO.getPackageName())) {
            log.warn("Rate package name already exists: {}", ratePackageDTO.getPackageName());
            throw new DuplicateNameException("Rate Package", ratePackageDTO.getPackageName());
        }

        Pulse pulse = pulseRepository.findById(ratePackageDTO.getPulseId())
                .orElseThrow(() -> {
                    log.warn("Pulse not found with ID: {}", ratePackageDTO.getPulseId());
                    return new ResourceNotFoundException("Pulse not found with ID: " + ratePackageDTO.getPulseId());
                });

        ServiceType serviceTypeEnum = ratePackageDTO.getServiceType();
        RatePackageType ratePackageTypeEnum = ratePackageDTO.getRatePackageType();

        log.info("Creating rate package: {}", ratePackageDTO.getPackageName());

        RatePackage ratePackage = RatePackageMapper.toEntity(ratePackageDTO, pulse);
        ratePackage = ratePackageRepository.save(ratePackage);

        // flush() forces the pending INSERT to hit the DB within this transaction.
        // RateDetailsService.createRateDetail() calls findRatePackageById() in the same
        // transaction — without flush(), the row is not yet visible and the lookup fails.
        ratePackageRepository.flush();

        final Long savedPackageId = ratePackage.getRatePackageId();

        // Delegate rate_details creation to RateDetailsService — handles validation
        // (country lookup, prefix dedup, package-type enforcement) and versioning.
        if (ratePackageDTO.getRate_details() != null && !ratePackageDTO.getRate_details().isEmpty()) {
            rateDetailsService.createRateDetail(savedPackageId, ratePackageDTO.getRate_details());
        }

        // Re-fetch with JOIN FETCH so rate_details are populated in the response.
        RatePackage savedWithDetails = ratePackageRepository.findByIdWithRateDetails(savedPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rate Package not found after save with ID: " + savedPackageId));

        log.info("Successfully created rate package with ID: {}", savedWithDetails.getRatePackageId());

        ratePackageCache.updateCacheWithTrie(RatePackageMapper.toDTO(savedWithDetails));

        return RatePackageMapper.toDTO(savedWithDetails);
    }

    public List<RatePackageDTO> getAllRatePackages() {
        log.debug("Fetching all rate packages");
        // findAll() is overridden in repository with LEFT JOIN FETCH — safe to map directly.
        List<RatePackageDTO> packages = ratePackageRepository.findAll().stream()
                .map(RatePackageMapper::toDTO)
                .collect(Collectors.toList());
        log.debug("Retrieved {} rate packages", packages.size());
        return packages;
    }

    public PageResponseDTO<RatePackageDTO> getRatePackagesInPages(Pageable pageable) {
        log.debug("Fetching rate packages in pages");
        // Page<> with JOIN FETCH causes MultipleBagFetchException in Hibernate.
        // Two-step approach: fetch page of IDs first, then bulk-load with JOIN FETCH.
        Page<RatePackage> ratePackagesPage = ratePackageRepository.findAll(pageable);

        List<Long> ids = ratePackagesPage.getContent().stream()
                .map(RatePackage::getRatePackageId)
                .collect(Collectors.toList());

        Map<Long, RatePackage> hydratedMap = ratePackageRepository.findAllByIdWithRateDetails(ids)
                .stream()
                .collect(Collectors.toMap(RatePackage::getRatePackageId, p -> p));

        List<RatePackageDTO> ratePackageDTOs = ratePackagesPage.getContent().stream()
                .map(p -> RatePackageMapper.toDTO(hydratedMap.getOrDefault(p.getRatePackageId(), p)))
                .collect(Collectors.toList());

        log.debug("Retrieved {} rate packages in a page", ratePackageDTOs.size());
        return PaginationUtils.buildGetResponseDTO(ratePackageDTOs, ratePackagesPage);
    }

    public PageResponseDTO<RatePackageDTO> searchRatePackages(RatePackageSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching rate packages with searchTerm: {} and serviceType: {}",
                searchDTO.getSearchTerm(), searchDTO.getServiceType());

        // Same two-step pattern as getRatePackagesInPages(): fetch IDs first, then bulk-load with JOIN FETCH.
        Page<RatePackage> ratePackagesPage = ratePackageRepository.searchRatePackages(
                searchDTO.getSearchTerm(), searchDTO.getServiceType(), pageable
        );

        List<Long> ids = ratePackagesPage.getContent().stream()
                .map(RatePackage::getRatePackageId)
                .collect(Collectors.toList());

        Map<Long, RatePackage> hydratedMap = ratePackageRepository.findAllByIdWithRateDetails(ids)
                .stream()
                .collect(Collectors.toMap(RatePackage::getRatePackageId, p -> p));

        List<RatePackageDTO> ratePackages = ratePackagesPage.getContent().stream()
                .map(p -> RatePackageMapper.toDTO(hydratedMap.getOrDefault(p.getRatePackageId(), p)))
                .collect(Collectors.toList());

        log.debug("Found {} rate packages matching criteria", ratePackages.size());
        return PaginationUtils.buildGetResponseDTO(ratePackages, ratePackagesPage);
    }

    public RatePackageDTO getPackageById(Long id) {
        log.debug("Fetching rate package with ID: {}", id);
        // Uses LEFT JOIN FETCH via findByIdWithRateDetails() to populate rate_details eagerly.
        RatePackage ratePackage = ratePackageRepository.findByIdWithRateDetails(id)
                .orElseThrow(() -> {
                    log.warn("Rate package not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate Package not found with ID: " + id);
                });
        return RatePackageMapper.toDTO(ratePackage);
    }
    @Transactional
    public RatePackageDTO updateRatePackage(Long id, RatePackageDTO ratePackageDTO) {
        log.info("Updating rate package with ID: {}", id);

        RatePackage existingPackage = ratePackageRepository.findByIdWithRateDetails(id)
                .orElseThrow(() -> {
                    log.warn("Rate package not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate Package not found with ID: " + id);
                });

        // Skip name uniqueness check if the name has not changed (avoids self-conflict).
        if (!existingPackage.getPackageName().equalsIgnoreCase(ratePackageDTO.getPackageName()) &&
                ratePackageRepository.existsByPackageNameAndIsDeletedFalse(ratePackageDTO.getPackageName())) {
            log.warn("Rate package name already exists: {}", ratePackageDTO.getPackageName());
            throw new DuplicateNameException("Rate Package", ratePackageDTO.getPackageName());
        }

        Pulse pulse = pulseRepository.findById(ratePackageDTO.getPulseId())
                .orElseThrow(() -> {
                    log.warn("Pulse not found with ID: {}", ratePackageDTO.getPulseId());
                    return new ResourceNotFoundException("Pulse not found with ID: " + ratePackageDTO.getPulseId());
                });

        existingPackage.setPackageName(ratePackageDTO.getPackageName());
        existingPackage.setPackageDesc(ratePackageDTO.getPackageDesc());
        existingPackage.setType(ratePackageDTO.getType());
        existingPackage.setServiceType(ratePackageDTO.getServiceType());
        existingPackage.setRatePackageType(ratePackageDTO.getRatePackageType());
        existingPackage.setPulse(pulse);
        existingPackage.setRounding(ratePackageDTO.getRounding());
        existingPackage.setPriceRounding(ratePackageDTO.getPriceRounding());
        existingPackage.setCurrency(ratePackageDTO.getCurrency());
        existingPackage.setStartTime(ratePackageDTO.getStartTime());
        existingPackage.setEndTime(ratePackageDTO.getEndTime());
        existingPackage.setRate(ratePackageDTO.getRate());

        ratePackageRepository.save(existingPackage);

        // flush() ensures the header UPDATE is written to DB before RateDetailsService
        // executes its bulk DELETE + re-INSERT within the same transaction.
        ratePackageRepository.flush();

        // If ratePackageType changed, rate_details must be provided — existing details
        // would be structurally incompatible with the new type.
        boolean typeChanged = existingPackage.getRatePackageType() != ratePackageDTO.getRatePackageType();
        boolean hasNewRateDetails = ratePackageDTO.getRate_details() != null && !ratePackageDTO.getRate_details().isEmpty();

        if (typeChanged && !hasNewRateDetails) {
            throw new IllegalArgumentException(
                    "rate_details must be provided when changing ratePackageType from "
                    + existingPackage.getRatePackageType() + " to " + ratePackageDTO.getRatePackageType());
        }

        // Delegates to createRateDetail which archives existing details to history,
        // increments the version, and saves the new batch with full validation.
        if (hasNewRateDetails) {
            rateDetailsService.createRateDetail(id, ratePackageDTO.getRate_details());
        }

        // Re-fetch with JOIN FETCH so rate_details are populated in the response.
        RatePackage updatedWithDetails = ratePackageRepository.findByIdWithRateDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rate Package not found after update with ID: " + id));

        log.info("Successfully updated rate package with ID: {}", id);

        ratePackageCache.updateCacheWithTrie(RatePackageMapper.toDTO(updatedWithDetails));

        return RatePackageMapper.toDTO(updatedWithDetails);
    }


    public void deleteRatePackage(Long id) {
        log.info("Deleting rate package with ID: {}", id);
        if (!ratePackageRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent rate package with ID: {}", id);
            throw new ResourceNotFoundException("Rate Package not found with ID: " + id);
        }
        // First, check if there is any active rate package association using this rate package
        boolean isReferenced = ratePackageAssociationRepository.existsByRatePackage_RatePackageIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete rate package with ID: {} that is referenced by an active rate package association", id);
            throw new ForeignReferenceException("Rate Package cannot be soft deleted because it is referenced by an active Association.");
        }
        ratePackageRepository.deleteById(id);
        log.info("Successfully deleted rate package with ID: {}", id);

        ratePackageCache.removeFromCacheWithTrie(id);
    }

    public List<RatePackageDTO> getRatePackagesByType(RatePackageType type) {
        log.info("Fetching rate packages of type: {}", type);
        // Uses LEFT JOIN FETCH via findByRatePackageTypeWithDetails() to populate rate_details eagerly.
        Optional<List<RatePackage>> ratePackages = ratePackageRepository.findByRatePackageTypeWithDetails(type);
        if (ratePackages.isEmpty() || ratePackages.get().isEmpty()) {
            log.warn("No rate packages found for type: {}", type);
            throw new ResourceNotFoundException("No Rate Packages found for type: " + type);
        }
        List<RatePackageDTO> packageDTOs = ratePackages.get().stream()
                .map(RatePackageMapper::toDTO)
                .collect(Collectors.toList());
        log.info("Successfully retrieved {} rate packages of type: {}", packageDTOs.size(), type);
        return packageDTOs;
    }


    public List<Map<String, Object>> getPackageIdAndNameByType(Type packageType, ServiceType serviceType) {
        log.info("Fetching package ids and names for type: {}, serviceType: {}", packageType, serviceType);
        return ratePackageRepository.findIdAndNameByType(packageType, serviceType);
    }
}