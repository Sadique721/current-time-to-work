package com.xcess.ocs.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ZoneDTO;
import com.xcess.ocs.dto.ZonePrefixOptionDTO;
import com.xcess.ocs.dto.search.ZoneSearchDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.entity.PrefixInputMode;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.InvalidInputException;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.ZoneMapper;
import com.xcess.ocs.ratingengine.service.ZoneLookupService;
import com.xcess.ocs.repository.CountryRepository;
import com.xcess.ocs.repository.PrefixRepository;
import com.xcess.ocs.repository.ZoneRepository;
import com.xcess.ocs.util.PaginationUtils;
import com.xcess.ocs.validator.PrefixPatternValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;
    private final ZoneLookupService zoneLookupService;
    private final PrefixRepository prefixRepository;
    private final CountryRepository countryRepository;

    public ZoneDTO createZone(ZoneDTO dto) {
        log.info("Creating new zone: {}", dto.getZoneName());

        if (zoneRepository.existsByZoneNameAndIsDeletedFalse(dto.getZoneName())) {
            throw new DuplicateNameException("Zone", dto.getZoneName());
        }

        dto.setPrefixPattern(resolvePrefixPattern(dto));

        Zone entity = zoneMapper.toEntity(dto);
        Zone saved = zoneRepository.save(entity);
        log.info("Successfully created zone with ID: {}", saved.getZoneId());

        rebuildAllTries();
        return zoneMapper.toDto(saved);
    }

    public ZoneDTO getZoneById(Long id) {
        log.debug("Fetching zone with ID: {}", id);
        Zone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));
        return zoneMapper.toDto(zone);
    }

    public ZoneDTO updateZone(Long id, ZoneDTO dto) {
        log.info("Updating zone with ID: {}", id);

        Zone existing = zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));

        if (!existing.getZoneName().equalsIgnoreCase(dto.getZoneName())) {
            if (zoneRepository.existsByZoneNameAndIsDeletedFalse(dto.getZoneName())) {
                throw new DuplicateNameException("Zone", dto.getZoneName());
            }
        }

        dto.setPrefixPattern(resolvePrefixPattern(dto));

        Zone entity = zoneMapper.toEntity(dto);
        entity.setZoneId(id);
        Zone saved = zoneRepository.save(entity);
        log.info("Successfully updated zone with ID: {}", id);

        rebuildAllTries();
        return zoneMapper.toDto(saved);
    }

    public void deleteZone(Long id) {
        log.info("Deleting zone with ID: {}", id);
        if (!zoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zone not found with id: " + id);
        }
        zoneRepository.deleteById(id);
        log.info("Successfully deleted zone with ID: {}", id);
        rebuildAllTries();
    }

    public PageResponseDTO<ZoneDTO> getZonesInPage(Pageable pageable) {
        Page<Zone> page = zoneRepository.findAll(pageable);
        List<ZoneDTO> dtos = page.getContent().stream().map(zoneMapper::toDto).collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(dtos, page);
    }

    public PageResponseDTO<ZoneDTO> searchZones(ZoneSearchDTO search, Pageable pageable) {
        String zoneName          = search != null ? search.getZoneName()       : null;
        String description       = search != null ? search.getDescription()    : null;
        Integer priority         = search != null ? search.getPriority()       : null;
        PrefixInputMode inputMode = search != null ? search.getPrefixInputMode() : null;

        Page<Zone> page = zoneRepository.searchZones(zoneName, description, priority, inputMode, pageable);
        List<ZoneDTO> dtos = page.getContent().stream().map(zoneMapper::toDto).collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(dtos, page);
    }

    public List<Map<String, Object>> getAllZoneNames() {
        return zoneRepository.findAll().stream()
                .map(z -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("zoneId", z.getZoneId());
                    map.put("zoneName", z.getZoneName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns the flat combined list of prefix table entries and country entries
     * for the zone prefix dropdown.
     *
     * @param prefixType optional filter — INTERCONNECT, ROAMING, or null for all
     * @param search     optional search term matched against prefix value, name, country name
     */
    public List<ZonePrefixOptionDTO> getPrefixOptions(PrefixType prefixType, String search) {
        String searchTerm = (search != null && search.isBlank()) ? null : search;

        List<ZonePrefixOptionDTO> options = new ArrayList<>();

        // --- PREFIX TABLE entries ---
        List<Prefix> prefixes = prefixRepository.findForZoneDropdown(prefixType, searchTerm);
        for (Prefix p : prefixes) {
            String countryName = p.getCountry() != null ? p.getCountry().getName() : "";
            String countryCode = p.getCountry() != null ? p.getCountry().getCountryCode() : "";
            String label = countryName + " - " + p.getPrefixName() + " (" + p.getPrefix() + ")";
            options.add(new ZonePrefixOptionDTO(
                    p.getPrefixId(), label, p.getPrefix(),
                    "PREFIX", p.getPrefixType(), countryName, countryCode));
        }

        // --- COUNTRY entries (country code as prefix value) ---
        // Only include country entries when not filtering by a specific prefixType
        // (country codes are type-agnostic — they work for both INTERCONNECT and ROAMING)
        if (prefixType == null) {
            List<Country> countries = countryRepository.findAll();
            for (Country c : countries) {
                if (c.getCountryCode() == null || c.getCountryCode().isBlank()) continue;
                if (searchTerm != null &&
                    !c.getName().toLowerCase().contains(searchTerm.toLowerCase()) &&
                    !c.getCountryCode().contains(searchTerm)) continue;

                String label = c.getName() + " (Country Code: " + c.getCountryCode() + ")";
                options.add(new ZonePrefixOptionDTO(
                        c.getCountryId(), label, c.getCountryCode(),
                        "COUNTRY", null, c.getName(), c.getCountryCode()));
            }
        }

        return options;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Dispatches to the correct resolution strategy based on prefixInputMode.
     * Always returns a normalized, validated prefixPattern string ready for DB storage.
     */
    private String resolvePrefixPattern(ZoneDTO dto) {
        if (dto.getPrefixInputMode() == null) {
            throw new InvalidInputException("Prefix input mode is required");
        }
        return switch (dto.getPrefixInputMode()) {
            case MANUAL   -> resolveManual(dto.getRawPrefixPattern());
            case DROPDOWN -> resolveDropdown(dto.getSelectedPrefixIds(), dto.getSelectedCountryIds());
        };
    }

    /**
     * MANUAL mode: validate and normalize the raw typed string.
     */
    private String resolveManual(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Raw prefix pattern is required when prefix input mode is MANUAL");
        }
        return PrefixPatternValidator.validateAndNormalize(raw);
    }

    /**
     * DROPDOWN mode: resolve selected prefix IDs and country IDs to their actual
     * digit values, merge, deduplicate, sort, and return normalized string.
     */
    private String resolveDropdown(List<Long> prefixIds, List<Long> countryIds) {
        boolean noPrefixes  = CollectionUtils.isEmpty(prefixIds);
        boolean noCountries = CollectionUtils.isEmpty(countryIds);

        if (noPrefixes && noCountries) {
            throw new InvalidInputException(
                    "At least one prefix or country must be selected when prefix input mode is DROPDOWN");
        }

        Set<String> values = new LinkedHashSet<>();

        if (!noPrefixes) {
            List<Prefix> found = prefixRepository.findAllByIds(prefixIds);
            // Validate all requested IDs were found
            if (found.size() != prefixIds.size()) {
                Set<Long> foundIds = found.stream().map(Prefix::getPrefixId).collect(Collectors.toSet());
                List<Long> missing = prefixIds.stream().filter(id -> !foundIds.contains(id)).toList();
                throw new InvalidInputException("Prefix IDs not found: " + missing);
            }
            found.stream().map(Prefix::getPrefix).forEach(values::add);
        }

        if (!noCountries) {
            List<Country> found = countryRepository.findAllById(countryIds);
            // Validate all requested IDs were found
            if (found.size() != countryIds.size()) {
                Set<Long> foundIds = found.stream().map(Country::getCountryId).collect(Collectors.toSet());
                List<Long> missing = countryIds.stream().filter(id -> !foundIds.contains(id)).toList();
                throw new InvalidInputException("Country IDs not found: " + missing);
            }
            found.stream()
                 .map(Country::getCountryCode)
                 .filter(cc -> cc != null && !cc.isBlank())
                 .forEach(values::add);
        }

        if (values.isEmpty()) {
            throw new InvalidInputException("No valid prefix values could be resolved from the selected items");
        }

        // Run through the same normalization (dedup + sort) — values are already digit strings from DB
        return values.stream().sorted().collect(Collectors.joining(","));
    }

    private void rebuildAllTries() {
        try {
            zoneLookupService.warmUpTrieCache();
        } catch (Exception e) {
            log.warn("Failed to rebuild trie cache after zone change: {}", e.getMessage());
        }
    }
}
