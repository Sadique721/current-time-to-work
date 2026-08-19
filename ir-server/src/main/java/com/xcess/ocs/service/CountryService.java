package com.xcess.ocs.service;

import com.xcess.ocs.dto.CountryDTO;
import com.xcess.ocs.dto.CountryResponseDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.CountrySearchDTO;
import com.xcess.ocs.entity.Country;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.CountryMapper;
import com.xcess.ocs.repository.CountryRepository;
import com.xcess.ocs.repository.PrefixRepository;
import com.xcess.ocs.repository.RateDetailsRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for Country operations.
 * Handles business logic for CRUD operations with soft delete support.
 *
 * <p>Business Rules:</p>
 * <ul>
 *   <li>Country name must be unique among active (non-deleted) records</li>
 *   <li>Country code CAN be duplicated for different countries (e.g., USA and Canada both use "1")</li>
 *   <li>Soft delete is blocked if country is referenced by active Prefix or RateDetails</li>
 * </ul>
 *
 * @see Country
 * @see CountryDTO
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CountryService {

    private final CountryRepository countryRepository;

    @Autowired
    private final PrefixRepository prefixRepository;

    @Autowired
    private final RateDetailsRepository rateDetailsRepository;

    private final CountryMapper countryMapper;

    /**
     * Retrieves all active (non-deleted) countries.
     *
     * @return List of CountryDTO containing all active countries
     */
    public List<CountryDTO> getAllCountries() {
        log.debug("Fetching all active countries");
        List<CountryDTO> countries = countryRepository.findAll().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} countries", countries.size());
        return countries;
    }

    /**
     * Retrieves a paginated list of active countries.
     *
     * @param pageable pagination information containing page number and size
     * @return PageResponseDTO containing paginated countries
     */
    public PageResponseDTO<CountryDTO> getCountriesInPage(Pageable pageable) {
        log.debug("Fetching countries in page: {}", pageable.getPageNumber());
        Page<Country> countryPage = countryRepository.findAll(pageable);
        List<CountryDTO> countries = countryPage.getContent().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} countries in page", countries.size());
        return PaginationUtils.buildGetResponseDTO(countries, countryPage);
    }

    /**
     * Searches countries by name or country code with pagination.
     *
     * @param searchDTO search criteria containing optional searchTerm
     * @param pageable pagination information
     * @return PageResponseDTO containing matching countries
     */
    public PageResponseDTO<CountryDTO> searchCountries(CountrySearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching countries with term: {}", searchDTO.getSearchTerm());

        Page<Country> countryPage = countryRepository.searchCountries(
                searchDTO.getSearchTerm(),
                pageable
        );

        List<CountryDTO> countries = countryPage.getContent().stream()
                .map(countryMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Found {} countries matching search criteria", countries.size());
        return PaginationUtils.buildGetResponseDTO(countries, countryPage);
    }

    /**
     * Retrieves a country by its ID.
     *
     * @param id the country ID
     * @return CountryDTO
     * @throws ResourceNotFoundException if country not found
     */
    public CountryDTO getCountryById(Long id) {
        log.debug("Fetching country with ID: {}", id);
        Country country = countryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Country not found with ID: {}", id);
                    return new ResourceNotFoundException("Country not found with ID: " + id);
                });
        return countryMapper.toDto(country);
    }

    /**
     * Creates a new country.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Validates and formats country code (removes + prefix, trims whitespace)</li>
     *   <li>Checks if an active country with the same name already exists</li>
     *   <li>If name exists in active records, throws DuplicateNameException</li>
     *   <li>If name does not exist, creates and saves the new country</li>
     * </ol>
     *
     * <p>Note: Duplicate country codes ARE allowed (e.g., USA and Canada both use "1")</p>
     *
     * @param countryDTO the country data to create
     * @return CountryResponseDTO with created country details
     * @throws DuplicateNameException if an active country with the same name already exists
     */
    public CountryResponseDTO createCountry(CountryDTO countryDTO) {
        log.info("Creating new country with name: {}", countryDTO.getName());

        // Step 1: Validate and format country code (remove + prefix)
        String countryCode = ensureCountryCodeFormat(countryDTO.getCountryCode());
        countryDTO.setCountryCode(countryCode);

        // Step 2: Check if active country with same name exists
        if (countryRepository.existsByNameAndIsDeletedFalse(countryDTO.getName())) {
            log.warn("Attempt to create country with existing name: {}", countryDTO.getName());
            throw new DuplicateNameException("Country", countryDTO.getName());
        }

        // Step 3: Validate ISO code format (uppercase) and uniqueness
        String isoCode = ensureIsoCodeFormat(countryDTO.getIsoCode());
        countryDTO.setIsoCode(isoCode);

        if (countryRepository.existsByIsoCodeAndIsDeletedFalse(isoCode)) {
            log.warn("Attempt to create country with existing ISO code: {}", isoCode);
            throw new DuplicateNameException("Country ISO code", isoCode);
        }

        Country country = countryMapper.toEntity(countryDTO);
        country = countryRepository.save(country);
        log.info("Successfully created country with ID: {}", country.getCountryId());

        return CountryResponseDTO.builder()
                .success(true)
                .message("Country created successfully")
                .data(countryMapper.toDto(country))
                .build();
    }

    /**
     * Updates an existing country.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Validates and formats country code (removes + prefix, trims whitespace)</li>
     *   <li>Verifies the country with given ID exists</li>
     *   <li>If name is being changed, checks if new name already exists in active records</li>
     *   <li>If new name exists in active records, throws DuplicateNameException</li>
     *   <li>Otherwise, updates all fields and saves</li>
     * </ol>
     *
     * <p>Note: Country code CAN be duplicated (e.g., USA and Canada both use "1")</p>
     *
     * @param id the country ID to update
     * @param countryDTO the updated country data
     * @return CountryResponseDTO with updated country details
     * @throws ResourceNotFoundException if country with given ID does not exist
     * @throws DuplicateNameException if new name already exists in active records
     */
    public CountryResponseDTO updateCountry(Long id, CountryDTO countryDTO) {
        log.info("Updating country with ID: {}", id);

        Country existingCountry = countryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Country not found with ID: {}", id);
                    return new ResourceNotFoundException("Country not found with ID: " + id);
                });

        // Validate and format country code
        String countryCode = ensureCountryCodeFormat(countryDTO.getCountryCode());
        countryDTO.setCountryCode(countryCode);

        // If name is changing, check if active country with new name exists
        if (!existingCountry.getName().equalsIgnoreCase(countryDTO.getName())) {
            log.debug("Country name changing from '{}' to '{}'", existingCountry.getName(), countryDTO.getName());

            if (countryRepository.existsByNameAndIsDeletedFalse(countryDTO.getName())) {
                log.warn("Country name already exists in active records: {}", countryDTO.getName());
                throw new DuplicateNameException("Country", countryDTO.getName());
            }

            existingCountry.setName(countryDTO.getName());
        }

        // Validate ISO code format and check uniqueness if changing
        String isoCode = ensureIsoCodeFormat(countryDTO.getIsoCode());
        if (!existingCountry.getIsoCode().equals(isoCode)) {
            log.debug("Country ISO code changing from '{}' to '{}'", existingCountry.getIsoCode(), isoCode);

            if (countryRepository.existsByIsoCodeAndIsDeletedFalse(isoCode)) {
                log.warn("Country ISO code already exists in active records: {}", isoCode);
                throw new DuplicateNameException("Country ISO code", isoCode);
            }

            existingCountry.setIsoCode(isoCode);
        }

        existingCountry.setCountryCode(countryCode);
        existingCountry.setCurrencyCode(countryDTO.getCurrencyCode());
        existingCountry.setCurrencySymbol(countryDTO.getCurrencySymbol());

        existingCountry = countryRepository.save(existingCountry);
        log.info("Successfully updated country with ID: {}", id);

        return CountryResponseDTO.builder()
                .success(true)
                .message("Country updated successfully")
                .data(countryMapper.toDto(existingCountry))
                .build();
    }

    /**
     * Deletes a country (soft delete).
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Verifies the country with given ID exists</li>
     *   <li>Checks if any active Prefix references this country</li>
     *   <li>Checks if any active RateDetails references this country (as source or destination)</li>
     *   <li>If referenced, throws ForeignReferenceException</li>
     *   <li>Otherwise, performs soft delete</li>
     * </ol>
     *
     * <p>Note: Does NOT check RateDetailsHistory (historical data can keep references)</p>
     *
     * @param id the country ID to delete
     * @throws ResourceNotFoundException if country with given ID does not exist
     * @throws ForeignReferenceException if country is referenced by active Prefix or RateDetails
     */
    public void deleteCountry(Long id) {
        log.info("Deleting country with ID: {}", id);

        if (!countryRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent country with ID: {}", id);
            throw new ResourceNotFoundException("Country not found with ID: " + id);
        }

        // Step 1: Check if there is any active prefix using this country
        boolean isReferencedByPrefix = prefixRepository.existsByCountry_CountryIdAndIsDeletedFalse(id);
        if (isReferencedByPrefix) {
            log.warn("Attempt to soft delete country with ID: {} that is referenced by an active prefix", id);
            throw new ForeignReferenceException("Country cannot be deleted because it is referenced by an active prefix.");
        }

        // Step 2: Check if there is any active rate detail using this country (source or destination)
        boolean isReferencedByRateDetails = 
            rateDetailsRepository.existsBySourceCountryId_CountryId(id) ||
            rateDetailsRepository.existsByDestinationCountryId_CountryId(id);

        if (isReferencedByRateDetails) {
            log.warn("Attempt to soft delete country with ID: {} that is referenced by active rate details", id);
            throw new ForeignReferenceException("Country cannot be deleted because it is referenced by active rate details.");
        }

        // Perform soft delete
        countryRepository.deleteById(id);
        log.info("Successfully deleted country with ID: {}", id);
    }

    /**
     * Get country names and codes for dropdown.
     *
     * @return List of maps containing country name and country code
     */
    public List<java.util.Map<String, Object>> getCountryNameAndCodeList() {
        log.debug("Fetching country names and codes for dropdown");
        return countryRepository.findAll().stream()
                .map(country -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("name", country.getName());
                    map.put("countryCode", country.getCountryCode());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Validates and formats country code.
     *
     * <p>Format rules:</p>
     * <ul>
     *   <li>Removes + prefix if present</li>
     *   <li>Trims leading/trailing whitespace</li>
     *   <li>Country code must contain only digits (0-9) and optional dashes</li>
     * </ul>
     *
     * @param countryCode the raw country code input
     * @return cleaned country code
     * @throws IllegalArgumentException if country code is null or contains invalid characters
     */
    private String ensureCountryCodeFormat(String countryCode) {
        log.debug("Formatting country code: {}", countryCode);

        if (countryCode == null) {
            log.warn("Null country code provided");
            throw new IllegalArgumentException("Country code cannot be null");
        }

        String cleanCode = countryCode.replace("+", "").trim();

        if (!cleanCode.matches("\\d+")) {
            log.warn("Invalid country code format: {}", countryCode);
            throw new IllegalArgumentException("Country code must contain only numbers");
        }

        return cleanCode;
    }

    /**
     * Validates and formats ISO 3166 alpha-2 country code.
     *
     * <p>Format rules:</p>
     * <ul>
     *   <li>Trims whitespace</li>
     *   <li>Converts to uppercase</li>
     *   <li>Must be exactly 2 uppercase letters</li>
     * </ul>
     *
     * @param isoCode the raw ISO code input
     * @return cleaned uppercase ISO code
     * @throws IllegalArgumentException if isoCode is null or invalid
     */
    private String ensureIsoCodeFormat(String isoCode) {
        log.debug("Formatting ISO code: {}", isoCode);

        if (isoCode == null) {
            log.warn("Null ISO code provided");
            throw new IllegalArgumentException("ISO code cannot be null");
        }

        String cleanCode = isoCode.trim().toUpperCase();

        if (!cleanCode.matches("[A-Z]{2}")) {
            log.warn("Invalid ISO code format: {}", isoCode);
            throw new IllegalArgumentException("ISO code must be exactly 2 letters");
        }

        return cleanCode;
    }

    public List<Map<String, Object>> getCountrycurrencyCode() {
        log.debug("Fetching country currencyCodes for dropdown");
        return countryRepository.findAll().stream()
                .map(country -> country.getCurrencyCode())
                .distinct()
                .map(code -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("currencyCode", code);
                    return map;
                })
                .collect(Collectors.toList());
    }


    /**
     * Get country ISO codes and names for dropdown.
     *
     * @return List of maps containing ISO code and country name
     */
    public List<Map<String, Object>> getCountryIsoCodeList() {
        log.debug("Fetching country ISO codes and names for dropdown");
        return countryRepository.findAll().stream()
                .map(country -> {
                    java.util.Map<String, Object> map = new java.util.HashMap<>();
                    map.put("isoCode", country.getIsoCode());
                    map.put("name", country.getName());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
