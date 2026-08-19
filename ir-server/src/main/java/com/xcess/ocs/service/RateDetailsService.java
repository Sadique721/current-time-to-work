package com.xcess.ocs.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.RateDetailDTO;
import com.xcess.ocs.dto.RateDetailUploadResponse;
import com.xcess.ocs.entity.Country;
import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.entity.RateDetailsHistory;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.RateDetailMapper;
import com.xcess.ocs.repository.CountryRepository;
import com.xcess.ocs.repository.RateDetailsHistoryRepository;
import com.xcess.ocs.repository.RateDetailsRepository;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Service for managing rate details
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RateDetailsService {
    // Repositories
    private final RateDetailsRepository rateDetailsRepository;
    private final RatePackageRepository ratePackageRepository;
    private final CountryRepository countryRepository;

    @Autowired
    private RateDetailsHistoryRepository rateDetailsHistoryRepository;

    // Mappers
    private final RateDetailMapper rateDetailMapper;

    // Constants
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    /**
     * Creates or replaces rate details for the given rate package.
     * If existing details are present, they are archived to RateDetailsHistory
     * and deleted before the new batch is saved with an incremented version.
     * Validates batch-level duplicates, package type constraints, and country
     * prefix mapping.
     */
    @Transactional
    public List<RateDetailDTO> createRateDetail(Long ratePackageId, List<RateDetailDTO> rateDetailDTOs) {
        List<RateDetailDTO> createdDetails = new ArrayList<>();

        rateDetailDTOs.forEach(dto -> dto.setRatePackageId(ratePackageId));

        List<RateDetails> existingDetails = rateDetailsRepository.findByRatePackageRatePackageId(ratePackageId);
        boolean isFirstUpload = existingDetails.isEmpty();

        Integer currentVersion;
        int newVersion;

        if (isFirstUpload) {
            currentVersion = 1;
            newVersion = 1;
        } else {
            currentVersion = rateDetailsRepository.findMaxCurrentVersionByRatePackageId(ratePackageId);
            newVersion = (currentVersion == null) ? 1 : currentVersion + 1;

            for (RateDetails detail : existingDetails) {
                RateDetailsHistory history = RateDetailsHistory.builder()
                        .rateDetailsId(detail.getRateDetailsId())
                        .destinationPrefix(detail.getDestinationPrefix())
                        .destinationPrefixName(detail.getDestinationPrefixName())
                        .sourcePrefix(detail.getSourcePrefix())
                        .sourcePrefixName(detail.getSourcePrefixName())
                        .zoneName(detail.getZoneName())
                        .rate(detail.getRate())
                        .startTime(detail.getStartTime())
                        .endTime(LocalDateTime.now())
                        .versionNumber(detail.getCurrentVersion())
                        .ratePackageId(detail.getRatePackage().getRatePackageId())
                        .sourceCountryId(detail.getSourceCountryId())
                        .sourceCountryCode(detail.getSourceCountryCode())
                        .sourceCountryName(detail.getSourceCountryName())
                        .destinationCountryId(detail.getDestinationCountryId())
                        .destinationCountryCode(detail.getDestinationCountryCode())
                        .destinationCountryName(detail.getDestinationCountryName())
                        .destinationCountryId(detail.getDestinationCountryId())
                        .sourceCountryId(detail.getSourceCountryId())
                        .build();
                rateDetailsHistoryRepository.save(history);
            }

            rateDetailsRepository.deleteByRatePackageId(ratePackageId);
        }

        RatePackage ratePackage = findRatePackageById(ratePackageId);

        // Check for duplicates within the incoming batch itself
        validateRateDetails(rateDetailDTOs, ratePackageId, ratePackage);

        List<Country> countries = countryRepository.findAll();
        Map<String, Country> countryMap = countries.stream()
                .collect(Collectors.toMap(
                        Country::getCountryCode,
                        country -> country,
                        (existing, duplicate) -> existing));
        List<String> sortedCountryCodes = countries.stream()
                .map(Country::getCountryCode)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());

        for (RateDetailDTO rateDetailDTO : rateDetailDTOs) {
            log.info("Creating new rate detail for destination prefix: {}", rateDetailDTO.getDestinationPrefix());

            validateRateDetail(rateDetailDTO, ratePackage);
            validateRateDetailAgainstPackageType(rateDetailDTO, ratePackage);

            if (ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
                if (rateDetailDTO.getSourcePrefix() != null) {
                    String matchedCode = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getSourcePrefix()::startsWith)
                            .findFirst()
                            .orElse(null);
                    if (matchedCode == null) {
                        throw new IllegalArgumentException(
                                "Source prefix does not match any country code: " + rateDetailDTO.getSourcePrefix());
                    }
                    Country country = countryMap.get(matchedCode);
                    rateDetailDTO.setSourceCountryCode(country.getCountryCode());
                    rateDetailDTO.setSourceCountryName(country.getName());
                    rateDetailDTO.setSourceCountryId(country.getCountryId());
                    if (!rateDetailDTO.getSourcePrefix().startsWith(country.getCountryCode())) {
                        throw new IllegalArgumentException("Source prefix '" + rateDetailDTO.getSourcePrefix()
                                + "' does not match country code '" + country.getCountryCode() + "'");
                    }
                }

                if (rateDetailDTO.getDestinationPrefix() != null) {
                    String matchedCode = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getDestinationPrefix()::startsWith)
                            .findFirst()
                            .orElse(null);
                    if (matchedCode == null) {
                        throw new IllegalArgumentException("Destination prefix does not match any country code: "
                                + rateDetailDTO.getDestinationPrefix());
                    }
                    Country country = countryMap.get(matchedCode);
                    rateDetailDTO.setDestinationCountryCode(country.getCountryCode());
                    rateDetailDTO.setDestinationCountryName(country.getName());
                    rateDetailDTO.setDestinationCountryId(country.getCountryId());
                    if (!rateDetailDTO.getDestinationPrefix().startsWith(country.getCountryCode())) {
                        throw new IllegalArgumentException("Destination prefix '" + rateDetailDTO.getDestinationPrefix()
                                + "' does not match country code '" + country.getCountryCode() + "'");
                    }
                }
            }

            Country sourceCountry = null;
            if (rateDetailDTO.getSourceCountryId() != null) {
                sourceCountry = countryRepository.findById(rateDetailDTO.getSourceCountryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Source country not found with ID: " + rateDetailDTO.getSourceCountryId()));
            }

            Country destinationCountry = null;
            if (rateDetailDTO.getDestinationCountryId() != null) {
                destinationCountry = countryRepository.findById(rateDetailDTO.getDestinationCountryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Destination country not found with ID: " + rateDetailDTO.getDestinationCountryId()));
            }

            LocalDateTime startTime = rateDetailDTO.getStartTime() != null ? rateDetailDTO.getStartTime()
                    : LocalDateTime.now();
            LocalDateTime endTime = rateDetailDTO.getEndTime();

            RateDetails rateDetail = RateDetails.builder()
                    .ratePackage(ratePackage)
                    .destinationPrefix(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getDestinationPrefix()
                            : null)
                    .destinationPrefixName(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getDestinationPrefixName()
                            : null)
                    .sourcePrefix(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getSourcePrefix()
                            : null)
                    .sourcePrefixName(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getSourcePrefixName()
                            : null)
                    .rate(rateDetailDTO.getRate())
                    .startTime(startTime)
                    .endTime(endTime)
                    .currentVersion(newVersion)
                    .sourceCountryCode(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getSourceCountryCode()
                            : null)
                    .sourceCountryName(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getSourceCountryName()
                            : null)
                    .destinationCountryCode(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getDestinationCountryCode()
                            : null)
                    .destinationCountryName(ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED
                            ? rateDetailDTO.getDestinationCountryName()
                            : null)
                    .sourceCountryId(sourceCountry)
                    .destinationCountryId(destinationCountry)
                    .zoneName(rateDetailDTO.getZoneName())
                    .build();

            rateDetail = rateDetailsRepository.save(rateDetail);

            log.info("Successfully created rate detail with ID: {}", rateDetail.getRateDetailsId());
            createdDetails.add(RateDetailMapper.toDTO(rateDetail));

        }

        return createdDetails;
    }

    /**
     * Returns all rate details across all rate packages.
     */
    public List<RateDetailDTO> getAllRateDetails() {
        log.debug("Fetching all rate details");

        List<Country> countries = countryRepository.findAll();
        Map<String, Country> countryMap = countries.stream()
                .collect(Collectors.toMap(
                        Country::getCountryCode,
                        country -> country,
                        (existing, duplicate) -> existing));

        List<String> sortedCountryCodes = countries.stream()
                .map(Country::getCountryCode)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());

        List<RateDetailDTO> rateDetails = rateDetailsRepository.findAll().stream()
                .map(rateDetail -> {
                    RateDetailDTO dto = rateDetailMapper.toDTO(rateDetail);

                    if (dto.getSourcePrefix() != null) {
                        sortedCountryCodes.stream()
                                .filter(dto.getSourcePrefix()::startsWith)
                                .findFirst()
                                .map(countryMap::get)
                                .ifPresent(sourceCountry -> {
                                    dto.setSourceCountryCode(sourceCountry.getCountryCode());
                                    dto.setSourceCountryName(sourceCountry.getName());
                                    dto.setSourceCountryId(sourceCountry.getCountryId());
                                });
                    }

                    if (dto.getDestinationPrefix() != null) {
                        sortedCountryCodes.stream()
                                .filter(dto.getDestinationPrefix()::startsWith)
                                .findFirst()
                                .map(countryMap::get)
                                .ifPresent(destinationCountry -> {
                                    dto.setDestinationCountryCode(destinationCountry.getCountryCode());
                                    dto.setDestinationCountryName(destinationCountry.getName());
                                    dto.setDestinationCountryId(destinationCountry.getCountryId());
                                });
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        log.debug("Retrieved {} rate details", rateDetails.size());
        return rateDetails;
    }

    /**
     * Returns a paginated list of rate details.
     */
    public PageResponseDTO<RateDetailDTO> getRateDetailsInPages(Pageable pageable) {
        log.debug("Fetching rate details in pages");
        Page<RateDetails> rateDetailsPage = rateDetailsRepository.findAll(pageable);
        List<RateDetailDTO> rateDetailDTOs = rateDetailsPage.getContent().stream()
                .map(RateDetailMapper::toDTO)
                .toList();

        log.debug("Retrieved {} rate details in a page", rateDetailDTOs.size());
        return PaginationUtils.buildGetResponseDTO(rateDetailDTOs, rateDetailsPage);
    }

    /**
     * Returns a single rate detail by ID. Throws ResourceNotFoundException if not
     * found.
     */
    public RateDetailDTO getRateDetailById(Long id) {
        log.debug("Fetching rate detail with ID: {}", id);
        return rateDetailsRepository.findById(id)
                .map(RateDetailMapper::toDTO)
                .orElseThrow(() -> {
                    log.warn("Rate detail not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate detail not found with ID: " + id);
                });
    }

    /**
     * Updates a single rate detail by ID.
     * Validates against the package type, resolves country from prefix,
     * and checks for duplicate prefix/zone name (skipped if value unchanged).
     */
    @Transactional
    public RateDetailDTO updateRateDetail(Long id, RateDetailDTO rateDetailDTO) {
        log.info("Updating rate detail with ID: {}", id);

        RateDetails existingDetail = findRateDetailById(id);

        RatePackage ratePackage = existingDetail.getRatePackage();

        validateRateDetail(rateDetailDTO, ratePackage);

        RatePackageType packageType = existingDetail.getRatePackage().getRatePackageType();

        if (packageType != RatePackageType.ZONE_DESTINATION_BASED) {
            List<Country> countries = countryRepository.findAll();
            Map<String, Country> countryMap = countries.stream()
                    .collect(Collectors.toMap(
                            Country::getCountryCode,
                            country -> country,
                            (existing, duplicate) -> existing));
            List<String> sortedCountryCodes = countries.stream()
                    .map(Country::getCountryCode)
                    .distinct()
                    .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                    .collect(Collectors.toList());

            if (rateDetailDTO.getSourcePrefix() != null) {
                if (!Objects.equals(existingDetail.getSourcePrefix(), rateDetailDTO.getSourcePrefix())) {
                    Optional<String> matchedSource = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getSourcePrefix()::startsWith)
                            .findFirst();

                    if (matchedSource.isPresent()) {
                        Country matchedCountry = countryMap.get(matchedSource.get());
                        rateDetailDTO.setSourceCountryCode(matchedCountry.getCountryCode());
                        rateDetailDTO.setSourceCountryName(matchedCountry.getName());
                        rateDetailDTO.setSourceCountryId(matchedCountry.getCountryId());
                    } else {
                        log.warn("Source prefix does not match any known country code: {}",
                                rateDetailDTO.getSourcePrefix());
                        throw new IllegalArgumentException("Source prefix does not match any known country code: "
                                + rateDetailDTO.getSourcePrefix());
                    }
                }
            }

            if (rateDetailDTO.getDestinationPrefix() != null) {
                if (!Objects.equals(existingDetail.getDestinationPrefix(), rateDetailDTO.getDestinationPrefix())) {
                    Optional<String> matchedDest = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getDestinationPrefix()::startsWith)
                            .findFirst();

                    if (matchedDest.isPresent()) {
                        Country matchedCountry = countryMap.get(matchedDest.get());
                        rateDetailDTO.setDestinationCountryCode(matchedCountry.getCountryCode());
                        rateDetailDTO.setDestinationCountryName(matchedCountry.getName());
                        rateDetailDTO.setDestinationCountryId(matchedCountry.getCountryId());
                    } else {
                        log.warn("Destination prefix does not match any known country code: {}",
                                rateDetailDTO.getDestinationPrefix());
                        throw new IllegalArgumentException("Destination prefix does not match any known country code: "
                                + rateDetailDTO.getDestinationPrefix());
                    }
                }
            }
        }

        if (rateDetailDTO.getRatePackageId() != null) {
            log.debug("Fetching rate package with ID: {}", rateDetailDTO.getRatePackageId());
            ratePackage = ratePackageRepository.findById(rateDetailDTO.getRatePackageId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rate package not found with ID: " + rateDetailDTO.getRatePackageId()));
            existingDetail.setRatePackage(ratePackage);
        }

        updateRateDetailFields(existingDetail, rateDetailDTO);

        rateDetailsRepository.save(existingDetail);

        log.info("Successfully updated rate detail with ID: {}", existingDetail.getRateDetailsId());
        return RateDetailMapper.toDTO(existingDetail);
    }

    /**
     * Retrieve history records for a specific rate detail ID.
     *
     * @param rateDetailsId the ID of the rate detail
     * @return a list of RateDetailsHistory entries
     */
    public List<RateDetailsHistory> getRateHistoryByRateDetailsId(Long rateDetailsId) {
        return rateDetailsHistoryRepository.findByRateDetailsId(rateDetailsId);
    }

    /**
     * Deletes a rate detail by ID. Throws ResourceNotFoundException if not found.
     */
    public void deleteRateDetail(Long id) {
        log.info("Deleting rate detail with ID: {}", id);

        if (!rateDetailsRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent rate detail with ID: {}", id);
            throw new ResourceNotFoundException("Rate detail not found with ID: " + id);
        }

        rateDetailsRepository.deleteById(id);
        log.info("Successfully deleted rate detail with ID: {}", id);
    }

    /**
     * Processes a CSV or XLSX file upload for the given rate package.
     * Archives existing details to history, then saves the new batch with an
     * incremented version.
     * Validates file headers, batch-level duplicates, package type constraints, and
     * country prefix mapping.
     */
    @Transactional
    public RateDetailUploadResponse processFile(MultipartFile file, Long ratePackageId) throws IOException {
        validateFile(file);
        RatePackage ratePackage = findRatePackageById(ratePackageId);

        // Determine new version: increment from current max, or start at 1.
        Integer currentVersion = rateDetailsRepository.findMaxCurrentVersionByRatePackageId(ratePackageId);
        currentVersion = (currentVersion == null) ? 1 : currentVersion + 1;

        // Archive existing rate details to history before replacing.
        List<RateDetails> existingDetails = rateDetailsRepository.findByRatePackageRatePackageId(ratePackageId);
        for (RateDetails detail : existingDetails) {
            RateDetailsHistory history = RateDetailsHistory.builder()
                    .rateDetailsId(detail.getRateDetailsId())
                    .destinationPrefix(detail.getDestinationPrefix())
                    .destinationPrefixName(detail.getDestinationPrefixName())
                    .sourcePrefix(detail.getSourcePrefix())
                    .sourcePrefixName(detail.getSourcePrefixName())
                    .zoneName(detail.getZoneName())
                    .rate(detail.getRate())
                    .startTime(detail.getStartTime())
                    .endTime(detail.getEndTime())
                    .versionNumber(detail.getCurrentVersion())
                    .ratePackageId(detail.getRatePackage().getRatePackageId())
                    .sourceCountryCode(detail.getSourceCountryCode())
                    .sourceCountryName(detail.getSourceCountryName())
                    .destinationCountryCode(detail.getDestinationCountryCode())
                    .destinationCountryName(detail.getDestinationCountryName())
                    .destinationCountryId(detail.getDestinationCountryId())
                    .sourceCountryId(detail.getSourceCountryId())
                    .build();
            rateDetailsHistoryRepository.save(history);
        }

        // Delete existing rate details after archiving.
        rateDetailsRepository.deleteByRatePackageId(ratePackageId);

        // Parse file and validate batch-level duplicates.
        List<RateDetailDTO> rateDetails = parseFile(file, ratePackageId, ratePackage);
        validateRateDetails(rateDetails, ratePackageId, ratePackage);

        // Resolve country data from prefixes and save each record.
        List<Country> countries = countryRepository.findAll();
        Map<String, Country> countryMap = countries.stream()
                .collect(Collectors.toMap(
                        Country::getCountryCode,
                        country -> country,
                        (existing, duplicate) -> existing));
        List<String> sortedCountryCodes = countries.stream()
                .map(Country::getCountryCode)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());

        for (RateDetailDTO rateDetailDTO : rateDetails) {
            validateRateDetailAgainstPackageType(rateDetailDTO, ratePackage);

            if (ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
                if (rateDetailDTO.getSourcePrefix() != null) {
                    String matchedCode = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getSourcePrefix()::startsWith)
                            .findFirst()
                            .orElse(null);
                    if (matchedCode == null) {
                        throw new IllegalArgumentException(
                                "Source prefix does not match any country code: " + rateDetailDTO.getSourcePrefix());
                    }
                    Country country = countryMap.get(matchedCode);
                    rateDetailDTO.setSourceCountryCode(country.getCountryCode());
                    rateDetailDTO.setSourceCountryName(country.getName());
                    rateDetailDTO.setSourceCountryId(country.getCountryId());

                }

                if (rateDetailDTO.getDestinationPrefix() != null) {
                    String matchedCode = sortedCountryCodes.stream()
                            .filter(rateDetailDTO.getDestinationPrefix()::startsWith)
                            .findFirst()
                            .orElse(null);
                    if (matchedCode == null) {
                        throw new IllegalArgumentException("Destination prefix does not match any country code: "
                                + rateDetailDTO.getDestinationPrefix());
                    }
                    Country country = countryMap.get(matchedCode);
                    rateDetailDTO.setDestinationCountryCode(country.getCountryCode());
                    rateDetailDTO.setDestinationCountryName(country.getName());
                    rateDetailDTO.setDestinationCountryId(country.getCountryId());

                }

                RateDetails rateDetail = RateDetailMapper.toEntity(rateDetailDTO, ratePackage);
                rateDetail.setCurrentVersion(currentVersion);
                rateDetail.setSourceCountryCode(rateDetailDTO.getSourceCountryCode());
                rateDetail.setSourceCountryName(rateDetailDTO.getSourceCountryName());
                rateDetail.setDestinationCountryCode(rateDetailDTO.getDestinationCountryCode());
                rateDetail.setDestinationCountryName(rateDetailDTO.getDestinationCountryName());
                rateDetail.setZoneName(rateDetailDTO.getZoneName());

                rateDetailsRepository.save(rateDetail);
            } else {
                RateDetails rateDetail = RateDetailMapper.toEntity(rateDetailDTO, ratePackage);
                rateDetail.setCurrentVersion(currentVersion);
                rateDetail.setZoneName(rateDetailDTO.getZoneName());

                rateDetailsRepository.save(rateDetail);
            }
        }

        return new RateDetailUploadResponse(rateDetails.size(), "File processed successfully");
    }

    private void moveExistingDetailsToHistory(Long ratePackageId) {
        List<RateDetails> existingDetails = rateDetailsRepository.findByRatePackageRatePackageId(ratePackageId);
        for (RateDetails detail : existingDetails) {
            RateDetailsHistory history = RateDetailsHistory.builder()
                    .rateDetailsId(detail.getRateDetailsId())
                    .destinationPrefix(detail.getDestinationPrefix())
                    .destinationPrefixName(detail.getDestinationPrefixName())
                    .sourcePrefix(detail.getSourcePrefix())
                    .sourcePrefixName(detail.getSourcePrefixName())
                    .zoneName(detail.getZoneName())
                    .rate(detail.getRate())
                    .startTime(detail.getStartTime())
                    .endTime(detail.getEndTime())
                    .versionNumber(detail.getCurrentVersion())
                    .ratePackageId(detail.getRatePackage().getRatePackageId())
                    .sourceCountryCode(detail.getSourceCountryCode())
                    .sourceCountryName(detail.getSourceCountryName())
                    .destinationCountryCode(detail.getDestinationCountryCode())
                    .destinationCountryName(detail.getDestinationCountryName())
                    .destinationCountryId(detail.getDestinationCountryId())
                    .sourceCountryId(detail.getSourceCountryId())
                    .build();
            rateDetailsHistoryRepository.save(history);
        }
        rateDetailsRepository.deleteAllInBatch();
    }

    private Map<String, Country> getCountryMap() {
        return countryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Country::getCountryCode,
                        country -> country,
                        (existing, duplicate) -> existing));
    }

    private List<String> getSortedCountryCodes() {
        return countryRepository.findAll().stream()
                .map(Country::getCountryCode)
                .distinct()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());
    }

    private void saveNewRateDetails(List<RateDetailDTO> rateDetails, RatePackage ratePackage, Integer currentVersion,
            Map<String, Country> countryMap, List<String> sortedCountryCodes) {
        for (RateDetailDTO rateDetailDTO : rateDetails) {
            enrichRateDetailWithCountryData(rateDetailDTO, countryMap, sortedCountryCodes);

            RateDetails rateDetail = RateDetailMapper.toEntity(rateDetailDTO, ratePackage);
            rateDetail.setCurrentVersion(currentVersion);
            rateDetailsRepository.save(rateDetail);
        }
    }

    private void enrichRateDetailWithCountryData(RateDetailDTO rateDetailDTO, Map<String, Country> countryMap,
            List<String> sortedCountryCodes) {
        if (rateDetailDTO.getSourcePrefix() != null) {
            String matchedCode = sortedCountryCodes.stream()
                    .filter(rateDetailDTO.getSourcePrefix()::startsWith)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source prefix does not match any country code: " + rateDetailDTO.getSourcePrefix()));
            Country country = countryMap.get(matchedCode);
            rateDetailDTO.setSourceCountryCode(country.getCountryCode());
            rateDetailDTO.setSourceCountryName(country.getName());
            rateDetailDTO.setSourceCountryId(country.getCountryId());
        }

        if (rateDetailDTO.getDestinationPrefix() != null) {
            String matchedCode = sortedCountryCodes.stream()
                    .filter(rateDetailDTO.getDestinationPrefix()::startsWith)
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException("Destination prefix does not match any country code: "
                                    + rateDetailDTO.getDestinationPrefix()));
            Country country = countryMap.get(matchedCode);
            rateDetailDTO.setDestinationCountryCode(country.getCountryCode());
            rateDetailDTO.setDestinationCountryName(country.getName());
            rateDetailDTO.setDestinationCountryId(country.getCountryId());
        }
    }

    private RatePackage findRatePackageById(Long ratePackageId) {
        return ratePackageRepository.findById(ratePackageId)
                .orElseThrow(() -> {
                    log.warn("Rate package not found with ID: {}", ratePackageId);
                    return new ResourceNotFoundException("Rate package not found with ID: " + ratePackageId);
                });
    }

    private RateDetails findRateDetailById(Long id) {
        return rateDetailsRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rate detail not found with ID: {}", id);
                    return new ResourceNotFoundException("Rate detail not found with ID: " + id);
                });
    }

    /**
     * Validates rate, time fields, and prefix format based on the package type.
     */
    private void validateRateDetail(RateDetailDTO dto, RatePackage ratePackage) {
        if (ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
            String ratingType = dto.getSourcePrefix() != null ? "source-destination" : "destination";
            validatePrefixFormat(dto.getDestinationPrefix(), dto.getSourcePrefix(), ratingType);

            if (dto.getDestinationPrefixName() != null && !dto.getDestinationPrefixName().trim().isEmpty()
                    && (dto.getDestinationPrefix() != null && !dto.getDestinationPrefix().trim().isEmpty())) {
            }
        }

        if (dto.getRate() == null || dto.getRate() <= 0) {
            throw new IllegalArgumentException("Rate must be greater than 0.");
        }

        validateTimeFields(dto);
    }

    /**
     * Enforces field constraints per package type:
     * - DESTINATION_BASED: only destinationPrefix allowed, sourcePrefix must be
     * absent.
     * - SOURCE_DESTINATION_BASED: both sourcePrefix and sourcePrefixName required.
     * - ZONE_DESTINATION_BASED: only zoneName allowed, prefixes must be absent.
     */
    private void validateRateDetailAgainstPackageType(RateDetailDTO dto, RatePackage ratePackage) {
        RatePackageType packageType = ratePackage.getRatePackageType();
        boolean hasSourcePrefix = dto.getSourcePrefix() != null && !dto.getSourcePrefix().trim().isEmpty();
        boolean hasDestinationPrefix = dto.getDestinationPrefix() != null
                && !dto.getDestinationPrefix().trim().isEmpty();
        boolean hasZoneName = dto.getZoneName() != null && !dto.getZoneName().trim().isEmpty();

        switch (packageType) {
            case DESTINATION_BASED:
                if (hasSourcePrefix) {
                    throw new IllegalArgumentException(
                            "Source prefix is not allowed for DESTINATION_BASED rate packages. " +
                                    "Only destination prefix should be provided.");
                }
                break;

            case SOURCE_DESTINATION_BASED:
                if (!hasSourcePrefix) {
                    throw new IllegalArgumentException(
                            "Source prefix is required for SOURCE_DESTINATION_BASED rate packages. " +
                                    "Both source and destination prefixes must be provided.");
                }
                if (dto.getSourcePrefixName() == null || dto.getSourcePrefixName().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Source prefix name is required when source prefix is provided.");
                }
                break;

            case ZONE_DESTINATION_BASED:
                if (hasSourcePrefix || hasDestinationPrefix) {
                    throw new IllegalArgumentException(
                            "Source and destination prefixes are not allowed for ZONE_DESTINATION_BASED rate packages. "
                                    +
                                    "Only zone name should be provided.");
                }
                if (!hasZoneName) {
                    throw new IllegalArgumentException(
                            "Zone name is required for ZONE_DESTINATION_BASED rate packages.");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported rate package type: " + packageType);
        }

        log.debug(
                "Rate detail validation passed for package type: {}, hasSourcePrefix: {}, hasDestinationPrefix: {}, hasZoneName: {}",
                packageType, hasSourcePrefix, hasDestinationPrefix, hasZoneName);
    }

    /**
     * Checks the DB for an existing rate detail with the same key for this rate
     * package.
     * Key is zoneName (ZONE_DESTINATION_BASED), sourcePrefix+destinationPrefix
     * (SOURCE_DESTINATION_BASED),
     * or destinationPrefix (DESTINATION_BASED).
     */
    private void checkDuplicatePrefix(RateDetailDTO dto, RatePackage ratePackage) {
        switch (ratePackage.getRatePackageType()) {
            case ZONE_DESTINATION_BASED:
                if (dto.getZoneName() != null && !dto.getZoneName().trim().isEmpty()) {
                    boolean exists = rateDetailsRepository.existsByZoneNameAndRatePackageRatePackageId(
                            dto.getZoneName().trim(),
                            ratePackage.getRatePackageId());
                    if (exists) {
                        throw new IllegalArgumentException(
                                "Rate detail already exists for zone name: " + dto.getZoneName());
                    }
                }
                break;

            default:
                if (dto.getSourcePrefix() != null && !dto.getSourcePrefix().trim().isEmpty()) {
                    boolean exists = rateDetailsRepository
                            .existsBySourcePrefixAndDestinationPrefixAndRatePackageRatePackageId(
                                    dto.getSourcePrefix(),
                                    dto.getDestinationPrefix(),
                                    ratePackage.getRatePackageId());
                    if (exists) {
                        throw new IllegalArgumentException(
                                "Rate detail already exists for source prefix " + dto.getSourcePrefix() +
                                        " and destination prefix " + dto.getDestinationPrefix());
                    }
                } else {
                    boolean exists = rateDetailsRepository.existsByDestinationPrefixAndRatePackageRatePackageId(
                            dto.getDestinationPrefix(),
                            ratePackage.getRatePackageId());
                    if (exists) {
                        throw new IllegalArgumentException(
                                "Rate detail already exists for destination prefix " + dto.getDestinationPrefix());
                    }
                }
                break;
        }
    }
    /*
     * Check if a prefix is unique within a rate package
     */
    // private void checkPrefixUniqueness(String prefix, Long ratePackageId) {
    // if (rateDetailsRepository.existsByPrefixAndRatePackageRatePackageId(prefix,
    // ratePackageId)) {
    // log.warn("Prefix {} already exists for rate package ID: {}", prefix,
    // ratePackageId);
    // throw new IllegalArgumentException("Prefix " + prefix + " already exists for
    // this rate package");
    // }
    // }

    /**
     * Applies DTO field values onto the existing entity.
     * For ZONE_DESTINATION_BASED only sets zoneName; otherwise sets all prefix and
     * country fields.
     * Always updates rate, startTime, and endTime.
     */
    private void updateRateDetailFields(RateDetails existingDetail, RateDetailDTO rateDetailDTO) {
        if (existingDetail.getRatePackage().getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
            if (rateDetailDTO.getSourcePrefix() != null && rateDetailDTO.getDestinationPrefix() != null
                    && rateDetailDTO.getRatePackageId() != null) {
                boolean duplicateExists = rateDetailsRepository
                        .existsBySourcePrefixAndDestinationPrefixAndRatePackageRatePackageId(
                                rateDetailDTO.getSourcePrefix(),
                                rateDetailDTO.getDestinationPrefix(),
                                rateDetailDTO.getRatePackageId());

                if (duplicateExists
                        && !Objects.equals(existingDetail.getRateDetailsId(), rateDetailDTO.getRateDetailsId())) {
                    throw new IllegalArgumentException(
                            "A rate detail with the same source prefix, destination prefix, and rate package already exists.");
                }
            }
        }

        if (existingDetail.getRatePackage().getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
            if (rateDetailDTO.getDestinationPrefix() != null) {
                log.debug("Updating destination prefix: {}", rateDetailDTO.getDestinationPrefix());
                existingDetail.setDestinationPrefix(rateDetailDTO.getDestinationPrefix());
            }
            if (rateDetailDTO.getDestinationPrefixName() != null) {
                log.debug("Updating destination prefix name: {}", rateDetailDTO.getDestinationPrefixName());
                existingDetail.setDestinationPrefixName(rateDetailDTO.getDestinationPrefixName());
            }
            if (rateDetailDTO.getSourceCountryCode() != null) {
                log.debug("Updating source country code: {}", rateDetailDTO.getSourceCountryCode());
                existingDetail.setSourceCountryCode(rateDetailDTO.getSourceCountryCode());
            }
            if (rateDetailDTO.getSourceCountryName() != null) {
                log.debug("Updating source country name: {}", rateDetailDTO.getSourceCountryName());
                existingDetail.setSourceCountryName(rateDetailDTO.getSourceCountryName());
            }
            if (rateDetailDTO.getDestinationCountryCode() != null) {
                log.debug("Updating destination country code: {}", rateDetailDTO.getDestinationCountryCode());
                existingDetail.setDestinationCountryCode(rateDetailDTO.getDestinationCountryCode());
            }
            if (rateDetailDTO.getDestinationCountryName() != null) {
                log.debug("Updating destination country name: {}", rateDetailDTO.getDestinationCountryName());
                existingDetail.setDestinationCountryName(rateDetailDTO.getDestinationCountryName());
            }
            if (rateDetailDTO.getSourcePrefix() != null) {
                log.debug("Updating source prefix: {}", rateDetailDTO.getSourcePrefix());
                existingDetail.setSourcePrefix(rateDetailDTO.getSourcePrefix());
            }
            if (rateDetailDTO.getSourcePrefixName() != null) {
                log.debug("Updating source prefix name: {}", rateDetailDTO.getSourcePrefixName());
                existingDetail.setSourcePrefixName(rateDetailDTO.getSourcePrefixName());
            }

            if (rateDetailDTO.getSourceCountryId() != null) {
                log.debug("Fetching source country with ID: {}", rateDetailDTO.getSourceCountryId());
                Country sourceCountry = countryRepository.findById(rateDetailDTO.getSourceCountryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Source country not found with ID: " + rateDetailDTO.getSourceCountryId()));
                existingDetail.setSourceCountryId(sourceCountry);
            }
            if (rateDetailDTO.getDestinationCountryId() != null) {
                log.debug("Fetching destination country with ID: {}", rateDetailDTO.getDestinationCountryId());
                Country destinationCountry = countryRepository.findById(rateDetailDTO.getDestinationCountryId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Destination country not found with ID: " + rateDetailDTO.getDestinationCountryId()));
                existingDetail.setDestinationCountryId(destinationCountry);
            }
        }
        if (rateDetailDTO.getRate() != null) {
            log.debug("Updating rate: {}", rateDetailDTO.getRate());
            existingDetail.setRate(rateDetailDTO.getRate());
        }
        if (rateDetailDTO.getStartTime() != null) {
            log.debug("Updating start time: {}", rateDetailDTO.getStartTime());
            existingDetail.setStartTime(rateDetailDTO.getStartTime());
        }
        if (rateDetailDTO.getEndTime() != null) {
            log.debug("Updating end time: {}", rateDetailDTO.getEndTime());
            existingDetail.setEndTime(rateDetailDTO.getEndTime());
        }
        if (rateDetailDTO.getZoneName() != null) {
            log.debug("Updating zone name: {}", rateDetailDTO.getZoneName());
            existingDetail.setZoneName(rateDetailDTO.getZoneName());
        }

        if (rateDetailDTO.getRatePackageId() != null) {
            log.debug("Fetching rate package with ID: {}", rateDetailDTO.getRatePackageId());
            RatePackage ratePackage = ratePackageRepository.findById(rateDetailDTO.getRatePackageId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Rate package not found with ID: " + rateDetailDTO.getRatePackageId()));
            existingDetail.setRatePackage(ratePackage);
        }
    }

    /**
     * Validates that the file is non-empty and has a supported extension (.csv or
     * .xlsx).
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        if (!fileName.endsWith(".csv") && !fileName.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Unsupported file format. Please upload CSV or XLSX file.");
        }
    }

    /**
     * Routes parsing to processCSVFile or processExcelFile based on file extension.
     */
    private List<RateDetailDTO> parseFile(MultipartFile file, Long ratePackageId, RatePackage ratePackage)
            throws IOException {
        String fileName = file.getOriginalFilename();

        if (fileName.endsWith(".csv")) {
            return processCSVFile(file, ratePackageId, ratePackage.getRatePackageType());
        } else {
            return processExcelFile(file, ratePackageId, ratePackage.getRatePackageType());
        }
    }

    /**
     * Validates that no two records in the batch share the same key:
     * - ZONE_DESTINATION_BASED: zoneName
     * - SOURCE_DESTINATION_BASED: sourcePrefix-destinationPrefix
     * - DESTINATION_BASED: destinationPrefix
     */
    private void validateRateDetails(List<RateDetailDTO> rateDetails, Long ratePackageId, RatePackage ratePackage) {
        if (ratePackage.getRatePackageType() == RatePackageType.ZONE_DESTINATION_BASED) {
            Set<String> zoneNamesInBatch = new HashSet<>();
            for (RateDetailDTO dto : rateDetails) {
                if (dto.getZoneName() == null || dto.getZoneName().trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Zone name is required for ZONE_DESTINATION_BASED rate packages.");
                }
                if (!zoneNamesInBatch.add(dto.getZoneName().trim())) {
                    throw new IllegalArgumentException("Duplicate zone name found in request: " + dto.getZoneName());
                }
            }
        } else {
            Set<String> prefixesInBatch = new HashSet<>();
            for (RateDetailDTO dto : rateDetails) {
                String key = ratePackage.getRatePackageType() == RatePackageType.SOURCE_DESTINATION_BASED
                        ? dto.getSourcePrefix() + "-" + dto.getDestinationPrefix()
                        : dto.getDestinationPrefix();
                if (!prefixesInBatch.add(key)) {
                    throw new IllegalArgumentException("Duplicate prefix found in request: " + key);
                }
            }
        }
    }

    private List<RateDetailDTO> processCSVFile(MultipartFile file, Long ratePackageId, RatePackageType packageType)
            throws IOException {
        List<RateDetailDTO> rateDetails = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReader csvReader = new CSVReader(reader)) {

            String[] header = csvReader.readNext();
            validateCSVHeader(header, packageType);

            String[] line;
            while ((line = csvReader.readNext()) != null) {
                try {
                    RateDetailDTO dto = parseCSVLine(line, ratePackageId, packageType);
                    validateRateDetailDTO(dto);
                    rateDetails.add(dto);
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                            "Error in line: " + String.join(",", line) + ". " + e.getMessage());
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("Error reading CSV file", e);
        }

        return rateDetails;
    }

    /**
     * Parses an XLSX file into a list of RateDetailDTOs based on the package type.
     * Validates the header row and each data row.
     */
    private List<RateDetailDTO> processExcelFile(MultipartFile file, Long ratePackageId, RatePackageType packageType)
            throws IOException {
        List<RateDetailDTO> rateDetails = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            validateExcelHeader(headerRow, packageType);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    try {
                        RateDetailDTO dto = parseExcelRow(row, ratePackageId, packageType);
                        validateRateDetailDTO(dto);
                        rateDetails.add(dto);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Error in row " + (i + 1) + ": " + e.getMessage());
                    }
                }
            }
        }

        return rateDetails;
    }

    /**
     * @deprecated Not used in current flow. Saving is handled inline in
     *             createRateDetail and processFile.
     */
    @Transactional
    private int saveRateDetails(List<RateDetailDTO> rateDetails, RatePackage ratePackage) {
        int recordsProcessed = 0;
        for (RateDetailDTO dto : rateDetails) {
            try {
                RateDetails entity = RateDetailMapper.toEntity(dto, ratePackage);
                rateDetailsRepository.save(entity);
                recordsProcessed++;
            } catch (DataIntegrityViolationException e) {
                throw new IllegalArgumentException("Error saving rate detail: " + e.getMessage());
            }
        }
        return recordsProcessed;
    }

    /**
     * Validates that rate is positive and startTime is before endTime.
     */
    private void validateRateDetailDTO(RateDetailDTO dto) {
        if (dto.getRate() == null || dto.getRate() <= 0) {
            throw new IllegalArgumentException("Rate must be greater than 0");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    /**
     * Validates that prefix is numeric (1-10 digits).
     * Also validates prefix2 format when provided.
     */
    private void validatePrefixFormat(String prefix, String prefix2, String type) {
        if (prefix == null || !prefix.matches("^\\d{1,10}$")) {
            throw new IllegalArgumentException(
                    "Invalid " + type
                            + " prefix format. It must be a number and may contain a single hyphen (e.g., '91', '1-345').");
        }
        if (prefix2 != null && !prefix2.equals("") && !prefix2.matches("^\\d{1,10}$")) {
            throw new IllegalArgumentException(
                    "Invalid " + type
                            + " prefix2 format. It must be a number and may contain a single hyphen (e.g., '91', '1-345').");
        }
        if (type.equals("source") && prefix2 != null && prefix2.trim().isEmpty()) {
            throw new IllegalArgumentException("Source prefix name cannot be empty when source prefix is provided.");
        }
    }

    /**
     * Validates that startTime and endTime are non-null and startTime is before
     * endTime.
     */
    private void validateTimeFields(RateDetailDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
    }

    /**
     * Validates the Excel header row has the correct number of columns for the
     * given package type:
     * - ZONE_DESTINATION_BASED: 4 columns
     * - DESTINATION_BASED: 5 columns
     * - SOURCE_DESTINATION_BASED: 7 columns
     */
    private void validateExcelHeader(Row headerRow, RatePackageType packageType) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Invalid Excel header format.");
        }

        if (packageType == RatePackageType.ZONE_DESTINATION_BASED) {
            if (headerRow.getPhysicalNumberOfCells() != 4) {
                throw new IllegalArgumentException(
                        "Invalid Excel header format for ZONE_DESTINATION_BASED. Expected 4 columns: zoneName, rate, startTime, endTime.");
            }
        } else {
            if (headerRow.getPhysicalNumberOfCells() != 5 && headerRow.getPhysicalNumberOfCells() != 7) {
                throw new IllegalArgumentException(
                        "Invalid Excel header format. Expected 5 columns for DESTINATION_BASED or 7 columns for SOURCE_DESTINATION_BASED.");
            }
        }
    }

    /**
     * Parses a date-time string trying multiple supported formats:
     * yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm, dd-MM-yyyy HH:mm:ss, yyyy-MM-dd HH:mm.
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date cannot be empty");
        }

        // Try each formatter
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {
                // Try next formatter
            }
        }

        // If we get here, none of the formatters worked
        throw new IllegalArgumentException(
                "Invalid date format: " + dateTimeStr
                        + ". Expected formats: yyyy-MM-dd HH:mm:ss, dd-MM-yyyy HH:mm, etc.");
    }

    /**
     * Parses a single CSV line into a RateDetailDTO based on the package type.
     */
    private RateDetailDTO parseCSVLine(String[] line, Long ratePackageId, RatePackageType packageType) {
        try {
            if (packageType == RatePackageType.ZONE_DESTINATION_BASED) {
                if (line.length < 4) {
                    throw new IllegalArgumentException(
                            "Invalid number of columns. Expected 4 for ZONE_DESTINATION_BASED.");
                }
                return RateDetailDTO.builder()
                        .zoneName(line[0])
                        .rate(Double.parseDouble(line[1]))
                        .startTime(parseDateTime(line[2]))
                        .endTime(parseDateTime(line[3]))
                        .ratePackageId(ratePackageId)
                        .build();
            } else {
                if (line.length < 5) {
                    throw new IllegalArgumentException("Invalid number of columns in line: " + String.join(",", line));
                }

                RateDetailDTO.RateDetailDTOBuilder builder = RateDetailDTO.builder()
                        .destinationPrefix(line[0])
                        .destinationPrefixName(line[1])
                        .rate(Double.parseDouble(line[2]))
                        .startTime(parseDateTime(line[3]))
                        .endTime(parseDateTime(line[4]))
                        .ratePackageId(ratePackageId);

                if (line.length > 5) {
                    builder.sourcePrefix(line[5])
                            .sourcePrefixName(line[6])
                            .currentVersion(1);
                }

                return builder.build();
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in line: " + String.join(",", line));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error in line: " + String.join(",", line) + ". " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Invalid number of columns in line: " + String.join(",", line));
        }
    }

    /**
     * Parses a single Excel row into a RateDetailDTO based on the package type.
     */
    private RateDetailDTO parseExcelRow(Row row, Long ratePackageId, RatePackageType packageType) {
        try {
            if (packageType == RatePackageType.ZONE_DESTINATION_BASED) {
                String zoneName = getStringCellValue(row.getCell(0));
                double rate = row.getCell(1).getNumericCellValue();
                String startTimeStr = getStringCellValue(row.getCell(2));
                String endTimeStr = getStringCellValue(row.getCell(3));

                return RateDetailDTO.builder()
                        .zoneName(zoneName)
                        .rate(rate)
                        .startTime(parseDateTime(startTimeStr))
                        .endTime(parseDateTime(endTimeStr))
                        .ratePackageId(ratePackageId)
                        .build();
            } else {
                String destinationPrefix = getStringCellValue(row.getCell(0));
                String destinationPrefixName = getStringCellValue(row.getCell(1));
                double rate = row.getCell(2).getNumericCellValue();
                String startTimeStr = getStringCellValue(row.getCell(3));
                String endTimeStr = getStringCellValue(row.getCell(4));

                RateDetailDTO.RateDetailDTOBuilder builder = RateDetailDTO.builder()
                        .destinationPrefix(destinationPrefix)
                        .destinationPrefixName(destinationPrefixName)
                        .rate(rate)
                        .startTime(parseDateTime(startTimeStr))
                        .endTime(parseDateTime(endTimeStr))
                        .ratePackageId(ratePackageId);

                if (row.getPhysicalNumberOfCells() > 6) {
                    builder.sourcePrefix(getStringCellValue(row.getCell(5)))
                            .sourcePrefixName(getStringCellValue(row.getCell(6)));
                }

                return builder.build();
            }
        } catch (Exception e) {
            int rowNum = row.getRowNum() + 1;
            throw new IllegalArgumentException("Error in row " + rowNum + ": " + e.getMessage());
        }
    }

    /**
     * Reads a cell value as a String. Handles STRING and NUMERIC (including
     * date-formatted) cell types.
     */
    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("Cell cannot be null");
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return DATE_FORMATTERS[0].format(cell.getLocalDateTimeCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                throw new IllegalArgumentException("Unsupported cell type");
        }
    }

    /**
     * Validates the CSV header row matches the expected columns for the given
     * package type.
     */
    private void validateCSVHeader(String[] header, RatePackageType packageType) {
        if (header == null) {
            throw new IllegalArgumentException("Invalid CSV header format");
        }

        if (packageType == RatePackageType.ZONE_DESTINATION_BASED) {
            if (header.length < 4) {
                throw new IllegalArgumentException("Invalid CSV header format for ZONE_DESTINATION_BASED rates.");
            }
            if (!header[0].equalsIgnoreCase("zoneName") ||
                    !header[1].equalsIgnoreCase("rate") ||
                    !header[2].equalsIgnoreCase("startTime") ||
                    !header[3].equalsIgnoreCase("endTime")) {
                throw new IllegalArgumentException(
                        "Invalid CSV header format for ZONE_DESTINATION_BASED rates. Expected: zoneName,rate,startTime,endTime");
            }
        } else {
            if (header.length < 5) {
                throw new IllegalArgumentException("Invalid CSV header format");
            }
            if (!header[0].equalsIgnoreCase("destinationPrefix") ||
                    !header[1].equalsIgnoreCase("destinationPrefixName") ||
                    !header[2].equalsIgnoreCase("rate") ||
                    !header[3].equalsIgnoreCase("startTime") ||
                    !header[4].equalsIgnoreCase("endTime")) {
                throw new IllegalArgumentException(
                        "Invalid CSV header format. Expected: destinationPrefix,destinationPrefixName,rate,startTime,endTime");
            }

            if (header.length > 5) {
                if (!header[5].equalsIgnoreCase("sourcePrefix") ||
                        !header[6].equalsIgnoreCase("sourcePrefixName")) {
                    throw new IllegalArgumentException(
                            "Invalid CSV header format for source-destination based rates. " +
                                    "Expected: destinationPrefix,destinationPrefixName,rate,startTime,endTime,sourcePrefix,sourcePrefixName");
                }
            }
        }
    }

    /**
     * Parses a preview file (CSV or XLSX) using the given ratePackageType without
     * persisting.
     * Used by the preview endpoint to validate and return parsed records before
     * actual upload.
     */
    public List<RateDetailDTO> parseRateDetailsFile(MultipartFile file, String ratePackageType) throws IOException {
        validateFile(file);
        RatePackageType packageType;
        try {
            packageType = RatePackageType.valueOf(ratePackageType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid ratePackageType: " + ratePackageType +
                    ". Allowed: DESTINATION_BASED, SOURCE_DESTINATION_BASED, ZONE_DESTINATION_BASED");
        }
        String fileName = file.getOriginalFilename();
        if (fileName.endsWith(".csv")) {
            return processCSVFile(file, null, packageType);
        } else {
            return processExcelFile(file, null, packageType);
        }
    }
}