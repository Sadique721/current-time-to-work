package com.xcess.ocs.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.TaxConfigDTO;
import com.xcess.ocs.dto.search.TaxConfigSearchDTO;
import com.xcess.ocs.entity.TaxConfig;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.TaxConfigMapper;
import com.xcess.ocs.repository.TaxConfigRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaxConfigService {

    private final TaxConfigRepository taxConfigRepository;
    private final TaxConfigMapper taxConfigMapper;

    public List<TaxConfigDTO> getAllTaxConfigs() {
        log.debug("Fetching all tax configs");
        return taxConfigRepository.findAll().stream()
                .map(taxConfigMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TaxConfigDTO getTaxConfigById(Long id) {
        log.debug("Fetching tax config with ID: {}", id);
        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found with id: " + id));
        return taxConfigMapper.toDTO(taxConfig);
    }

    public PageResponseDTO<TaxConfigDTO> searchTaxConfigs(TaxConfigSearchDTO searchDTO, Pageable pageable) {
        String searchTerm = searchDTO != null ? searchDTO.getSearchTerm() : null;
        Page<TaxConfig> taxConfigPage = taxConfigRepository.searchTaxConfigs(searchTerm, pageable);
        List<TaxConfigDTO> dtos = taxConfigPage.getContent().stream()
                .map(taxConfigMapper::toDTO)
                .collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(dtos, taxConfigPage);
    }

    public PageResponseDTO<TaxConfigDTO> getAllTaxConfigsPaged(Pageable pageable) {
        Page<TaxConfig> taxConfigPage = taxConfigRepository.findAll(pageable);
        List<TaxConfigDTO> dtos = taxConfigPage.getContent().stream()
                .map(taxConfigMapper::toDTO)
                .collect(Collectors.toList());
        return PaginationUtils.buildGetResponseDTO(dtos, taxConfigPage);
    }

    @Transactional
    public TaxConfigDTO createTaxConfig(TaxConfigDTO dto) {
        log.info("Creating new tax config: {}", dto.getTaxType());
        TaxConfig taxConfig = taxConfigMapper.toEntity(dto);
        if (taxConfig.getEffectiveFrom() == null) {
            taxConfig.setEffectiveFrom(LocalDate.now());
        }
        TaxConfig saved = taxConfigRepository.save(taxConfig);
        log.info("Successfully created tax config with ID: {}", saved.getTaxConfigId());
        return taxConfigMapper.toDTO(saved);
    }

    @Transactional
    public TaxConfigDTO updateTaxConfig(Long id, TaxConfigDTO dto) {
        log.info("Updating tax config with ID: {}", id);
        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found with id: " + id));

        if (taxConfig.getStandardRate().compareTo(dto.getStandardRate()) != 0) {
            log.info("Tax rate change detected! Enforcing immutability rule.");
            // Expire old config
            taxConfig.setEffectiveTo(LocalDate.now());
            taxConfig.setIsActive(false);
            taxConfigRepository.save(taxConfig);

            // Spawn new config
            TaxConfig newConfig = taxConfigMapper.toEntity(dto);
            newConfig.setTaxConfigId(null);
            newConfig.setEffectiveFrom(LocalDate.now().plusDays(1));
            newConfig.setEffectiveTo(null);
            newConfig.setIsActive(true);
            TaxConfig saved = taxConfigRepository.save(newConfig);
            return taxConfigMapper.toDTO(saved);
        } else {
            // Update in-place for non-rate modifications
            TaxConfig updatedEntity = taxConfigMapper.toEntity(dto);
            updatedEntity.setTaxConfigId(taxConfig.getTaxConfigId());
            updatedEntity.setEffectiveFrom(taxConfig.getEffectiveFrom());
            TaxConfig updated = taxConfigRepository.save(updatedEntity);
            return taxConfigMapper.toDTO(updated);
        }
    }

    @Transactional
    public void deleteTaxConfig(Long id) {
        log.info("Deleting tax config with ID: {}", id);
        TaxConfig taxConfig = taxConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tax config not found with id: " + id));
        taxConfigRepository.delete(taxConfig);
        log.info("Successfully deleted tax config with ID: {}", id);
    }

    public List<Map<String, Object>> getTaxConfigIdAndNameList() {
        log.debug("Fetching tax config IDs and names for dropdown");
        return taxConfigRepository.findAll().stream()
                .map(tc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("taxConfigId", tc.getTaxConfigId());
                    map.put("taxName", tc.getTaxName() + " (" + tc.getTaxType() + " - " + tc.getStandardRate() + "%)");
                    map.put("rate",tc.getStandardRate());
                    map.put("applyOn",tc.getApplyOn());
                    return map;
                })
                .collect(Collectors.toList());
    }
}
