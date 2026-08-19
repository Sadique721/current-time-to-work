package com.xcess.ocs.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PrefixDTO;
import com.xcess.ocs.dto.search.PrefixSearchDTO;
import com.xcess.ocs.entity.Country;
import com.xcess.ocs.entity.Prefix;
import com.xcess.ocs.entity.PrefixType;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.PrefixMapper;
import com.xcess.ocs.repository.CountryRepository;
import com.xcess.ocs.repository.PrefixRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for Prefix operations.
 * Handles business logic for CRUD operations with soft delete support.
 *
 * <p>Business Rules:</p>
 * <ul>
 *   <li>Prefix value must be unique per country (same country can't have duplicate prefix like "415" twice)</li>
 *   <li>Prefix name must be unique per country (same country can't have duplicate names like "New York" twice)</li>
 *   <li>Prefix value CAN be duplicated across different countries (e.g., USA "415" and India "415" are both allowed)</li>
 *   <li>Soft delete is blocked if prefix is referenced by active RateDetails</li>
 * </ul>
 *
 * @see Prefix
 * @see PrefixDTO
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PrefixService {

    private final PrefixRepository prefixRepository;
    private final CountryRepository countryRepository;
    private final PrefixMapper prefixMapper;

    public List<PrefixDTO> getPrefixesByIds(List<Long> ids) {
        log.debug("Fetching prefixes by IDs: {}", ids);
        return prefixRepository.findAllByIds(ids).stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<PrefixDTO> getPrefixesByType(PrefixType prefixType) {
        log.debug("Fetching prefixes by type: {}", prefixType);
        return prefixRepository.findForZoneDropdown(prefixType, null).stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all active (non-deleted) prefixes.
     *
     * @return List of PrefixDTO containing all active prefixes
     */
    public List<PrefixDTO> getAllPrefixes() {
        log.debug("Fetching all active prefixes");
        List<PrefixDTO> prefixes = prefixRepository.findAll().stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} prefixes", prefixes.size());
        return prefixes;
    }

    /**
     * Retrieves a paginated list of active prefixes.
     *
     * @param pageable pagination information containing page number and size
     * @return PageResponseDTO containing paginated prefixes
     */
    public PageResponseDTO<PrefixDTO> getPrefixesInPage(Pageable pageable) {
        log.debug("Fetching prefixes in page: {}", pageable.getPageNumber());
        Page<Prefix> prefixesPage = prefixRepository.findAll(pageable);
        List<PrefixDTO> prefixes = prefixesPage.getContent().stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} prefixes in page", prefixes.size());
        return PaginationUtils.buildGetResponseDTO(prefixes, prefixesPage);
    }

    /**
     * Searches prefixes by search term matching prefix, prefixName, or country name with pagination.
     *
     * @param searchDTO search criteria containing optional searchTerm
     * @param pageable pagination information
     * @return PageResponseDTO containing matching prefixes
     */
    public PageResponseDTO<PrefixDTO> searchPrefixes(PrefixSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching prefixes with term: {}", searchDTO.getSearchTerm());

        Page<Prefix> prefixPage = prefixRepository.searchPrefixes(
                searchDTO.getSearchTerm(),
                pageable);

        List<PrefixDTO> prefixes = prefixPage.getContent().stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Found {} prefixes matching criteria", prefixes.size());
        return PaginationUtils.buildGetResponseDTO(prefixes, prefixPage);
    }

    /**
     * Retrieves a prefix by its ID.
     *
     * @param id the prefix ID
     * @return PrefixDTO
     * @throws ResourceNotFoundException if prefix not found
     */
    public PrefixDTO getPrefixById(Long id) {
        log.debug("Fetching prefix with ID: {}", id);
        Prefix prefix = prefixRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Prefix not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefix not found with ID: " + id);
                });
        return prefixMapper.toDto(prefix);
    }

    /**
     * Creates a new prefix.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Validate prefix format (numbers only, optional single hyphen)</li>
     *   <li>Find country by name → error if not found</li>
     *   <li>Check if prefix already exists for this country → error if exists</li>
     *   <li>Check if prefix name already exists for this country → error if exists</li>
     *   <li>Create and save the new prefix</li>
     * </ol>
     *
     * <p>Note: Prefix value CAN be duplicated across different countries (e.g., USA "415" and India "415")</p>
     *
     * @param prefixDTO the prefix data to create
     * @return created PrefixDTO
     * @throws ResourceNotFoundException if country not found
     * @throws DuplicateNameException if prefix or prefix name already exists for the country
     */
    public PrefixDTO createPrefix(PrefixDTO prefixDTO) {
        log.info("Creating new prefix: {} for country: {}", prefixDTO.getPrefix(), prefixDTO.getCountryName());

        validatePrefixFormat(prefixDTO.getPrefix());

        Country country = countryRepository.findByName(prefixDTO.getCountryName())
                .orElseThrow(() -> {
                    log.warn("Country not found with name: {}", prefixDTO.getCountryName());
                    return new ResourceNotFoundException("Country not found with name: " + prefixDTO.getCountryName());
                });

        //  Check if active prefix with same value exists for  same country
        if (prefixRepository.existsByPrefixAndCountryCountryIdAndIsDeletedFalse(prefixDTO.getPrefix(), country.getCountryId())) {
            log.warn("Prefix {} already exists for country: {}", prefixDTO.getPrefix(), prefixDTO.getCountryName());
            throw new DuplicateNameException("Prefix", prefixDTO.getPrefix() + " for country " + prefixDTO.getCountryName());
        }

        //  Check if active prefix name with same value exists for  same country
        if (prefixRepository.existsByPrefixNameAndCountryCountryIdAndIsDeletedFalse(prefixDTO.getPrefixName(), country.getCountryId())) {
            log.warn("Prefix name '{}' already exists for country: {}", prefixDTO.getPrefixName(), prefixDTO.getCountryName());
            throw new DuplicateNameException("Prefix name", prefixDTO.getPrefixName() + " for country " + prefixDTO.getCountryName());
        }

        Prefix prefix = prefixMapper.toEntity(prefixDTO, country);
        prefix = prefixRepository.save(prefix);
        log.info("Successfully created prefix with ID: {}", prefix.getPrefixId());

        return prefixMapper.toDto(prefix);
    }

    /**
     * Updates an existing prefix.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Verify prefix with given ID exists → error if not found</li>
     *   <li>Validate prefix format if being changed</li>
     *   <li>Find country by name → error if not found</li>
     *   <li>If prefix OR prefix name is changing:
     *     <ul>
     *       <li>Check if new prefix already exists for this country → error if exists</li>
     *       <li>Check if new prefix name already exists for this country → error if exists</li>
     *     </ul>
     *   </li>
     *   <li>Update all fields and save</li>
     * </ol>
     *
     * <p>Note: Prefix value CAN be duplicated across different countries</p>
     *
     * @param id the prefix ID to update
     * @param prefixDTO the updated prefix data
     * @return updated PrefixDTO
     * @throws ResourceNotFoundException if prefix or country not found
     * @throws DuplicateNameException if new prefix or prefix name already exists for the country
     */
    public PrefixDTO updatePrefix(Long id, PrefixDTO prefixDTO) {
        log.info("Updating prefix with ID: {}", id);

        Prefix existingPrefix = prefixRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Prefix not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefix not found with ID: " + id);
                });

        validatePrefixFormat(prefixDTO.getPrefix());

        Country country = countryRepository.findByName(prefixDTO.getCountryName())
                .orElseThrow(() -> {
                    log.warn("Country not found with name: {}", prefixDTO.getCountryName());
                    return new ResourceNotFoundException("Country not found with name: " + prefixDTO.getCountryName());
                });

        boolean prefixChanged = !existingPrefix.getPrefix().equals(prefixDTO.getPrefix());
        boolean prefixNameChanged = !existingPrefix.getPrefixName().equalsIgnoreCase(prefixDTO.getPrefixName());
        boolean countryChanged = !existingPrefix.getCountry().getCountryId().equals(country.getCountryId());

        if (prefixChanged || countryChanged) {
            if (prefixRepository.existsByPrefixAndCountryCountryIdAndIsDeletedFalse(prefixDTO.getPrefix(), country.getCountryId())) {
                log.warn("Prefix {} already exists for country: {}", prefixDTO.getPrefix(), prefixDTO.getCountryName());
                throw new DuplicateNameException("Prefix", prefixDTO.getPrefix() + " for country " + prefixDTO.getCountryName());
            }
        }

        if (prefixNameChanged || countryChanged) {
            if (prefixRepository.existsByPrefixNameAndCountryCountryIdAndIsDeletedFalse(prefixDTO.getPrefixName(), country.getCountryId())) {
                log.warn("Prefix name '{}' already exists for country: {}", prefixDTO.getPrefixName(), prefixDTO.getCountryName());
                throw new DuplicateNameException("Prefix name", prefixDTO.getPrefixName() + " for country " + prefixDTO.getCountryName());
            }
        }

        existingPrefix.setPrefix(prefixDTO.getPrefix());
        existingPrefix.setPrefixName(prefixDTO.getPrefixName());
        existingPrefix.setCountry(country);
        if (prefixDTO.getPrefixType() != null) {
            existingPrefix.setPrefixType(prefixDTO.getPrefixType());
        }

        existingPrefix = prefixRepository.save(existingPrefix);
        log.info("Successfully updated prefix with ID: {}", id);

        return prefixMapper.toDto(existingPrefix);
    }

    /**
     * Deletes a prefix (soft delete).
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Verify prefix with given ID exists</li>
     *   <li>Check if any active RateDetails references this prefix → error if referenced</li>
     *   <li>Perform soft delete</li>
     * </ol>
     *
     * @param id the prefix ID to delete
     * @throws ResourceNotFoundException if prefix not found
     * @throws ForeignReferenceException if prefix is referenced by active RateDetails
     */
    public void deletePrefix(Long id) {
        log.info("Deleting prefix with ID: {}", id);

        Prefix prefix = prefixRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Prefix not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefix not found with ID: " + id);
                });

        // Check if any active rate detail uses this prefix for this country
        boolean isReferenced = prefixRepository.isReferencedByRateDetails(
                prefix.getPrefix(), prefix.getCountry().getCountryId());

        if (isReferenced) {
            log.warn("Attempt to delete prefix with ID: {} that is referenced by active rate details", id);
            throw new ForeignReferenceException("Prefix cannot be deleted because it is referenced by active rate details.");
        }

        // Perform soft delete
        prefixRepository.deleteById(id);
        log.info("Successfully deleted prefix with ID: {}", id);
    }

    /**
     * Retrieves all prefixes for a specific country by country name.
     *
     * @param countryName the country name
     * @return List of PrefixDTO for the country
     * @throws ResourceNotFoundException if country not found
     */
    public List<PrefixDTO> getPrefixesByCountryName(String countryName) {
        log.debug("Fetching prefixes for country: {}", countryName);
        Country country = countryRepository.findByName(countryName)
                .orElseThrow(() -> {
                    log.warn("Country not found with name: {}", countryName);
                    return new ResourceNotFoundException("Country not found with name: " + countryName);
                });
        List<PrefixDTO> prefixes = prefixRepository.findByCountryCountryId(country.getCountryId()).stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
        log.debug("Retrieved {} prefixes for country: {}", prefixes.size(), countryName);
        return prefixes;
    }

    /**
     * Retrieves all prefixes for a specific country by country ID.
     *
     * @param countryId the country ID
     * @return List of PrefixDTO for the country
     * @throws ResourceNotFoundException if country not found
     */
    public List<PrefixDTO> getPrefixesByCountryId(Long countryId) {
        log.debug("Fetching prefixes for country ID: {}", countryId);
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found with ID: " + countryId));
        return prefixRepository.findByCountryCountryId(country.getCountryId()).stream()
                .map(prefixMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates only the prefix name field without changing prefix value or country.
     *
     * <p>Process:</p>
     * <ol>
     *   <li>Verify prefix with given ID exists</li>
     *   <li>Check if new prefix name already exists for this country → error if exists</li>
     *   <li>Update prefix name and save</li>
     * </ol>
     *
     * @param prefixId the prefix ID
     * @param fields map containing prefixName to update
     * @return updated PrefixDTO
     * @throws ResourceNotFoundException if prefix not found
     * @throws DuplicateNameException if prefix name already exists for the country
     */
    public PrefixDTO updatePrefixName(Long prefixId, Map<String, Object> fields) {
        log.info("Updating prefix name for ID: {}", prefixId);

        Prefix existingPrefix = prefixRepository.findById(prefixId)
                .orElseThrow(() -> {
                    log.warn("Prefix not found with ID: {}", prefixId);
                    return new ResourceNotFoundException("Prefix not found with ID: " + prefixId);
                });

        String newPrefixName = fields.get("prefixName").toString();

        if (!existingPrefix.getPrefixName().equalsIgnoreCase(newPrefixName)) {
            if (prefixRepository.existsByPrefixNameAndCountryCountryIdAndIsDeletedFalse(
                    newPrefixName, existingPrefix.getCountry().getCountryId())) {
                log.warn("Prefix name '{}' already exists for country ID: {}", 
                        newPrefixName, existingPrefix.getCountry().getCountryId());
                throw new DuplicateNameException("Prefix name", 
                        newPrefixName + " for country " + existingPrefix.getCountry().getName());
            }
            existingPrefix.setPrefixName(newPrefixName);
        }

        Prefix updatedPrefix = prefixRepository.save(existingPrefix);
        log.info("Successfully updated prefix name for ID: {}", prefixId);

        return prefixMapper.toDto(updatedPrefix);
    }

    /**
     * Validates the prefix format.
     *
     * <p>Format rules:</p>
     * <ul>
     *   <li>Must contain only digits (0-9)</li>
     *   <li>Must not be empty or blank</li>
     * </ul>
     *
     * @param prefix the prefix value to validate
     * @throws IllegalArgumentException if prefix format is invalid
     */
    private void validatePrefixFormat(String prefix) {
        log.debug("Validating prefix format: {}", prefix);

        if (prefix == null || prefix.isBlank()) {
            log.warn("Prefix cannot be null or blank");
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }

        if (!prefix.matches("^\\d{1,15}$")) {
            log.warn("Invalid prefix format: {}", prefix);
            throw new IllegalArgumentException("Invalid prefix format. It must contain only numbers (e.g., '91', '415', '23801')");
        }
    }
}
