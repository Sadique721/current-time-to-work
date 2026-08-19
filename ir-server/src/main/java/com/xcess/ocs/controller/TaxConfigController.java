package com.xcess.ocs.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.dto.TaxConfigDTO;
import com.xcess.ocs.dto.search.TaxConfigSearchDTO;
import com.xcess.ocs.service.TaxConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tax-configs")
@Tag(name = "TaxConfig", description = "Tax configuration management APIs")
public class TaxConfigController {

    private final TaxConfigService taxConfigService;

    @GetMapping
    @Operation(summary = "Get all tax configs", description = "Returns a list of all active tax configurations")
    @ApiResponse(responseCode = "200", description = "List of tax configs retrieved successfully")
    public ResponseEntity<List<TaxConfigDTO>> getAllTaxConfigs() {
        log.info("REST request to get all tax configs");
        List<TaxConfigDTO> dtos = taxConfigService.getAllTaxConfigs();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax config by ID", description = "Returns the tax config with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax config found"),
            @ApiResponse(responseCode = "404", description = "Tax config not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaxConfigDTO> getTaxConfig(@PathVariable Long id) {
        log.info("REST request to get tax config: {}", id);
        TaxConfigDTO dto = taxConfigService.getTaxConfigById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @Operation(summary = "Create new tax config", description = "Creates a new tax configuration with the provided details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax config created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaxConfigDTO> createTaxConfig(@Valid @RequestBody TaxConfigDTO dto) {
        log.info("REST request to create tax config: {}", dto.getTaxType());
        TaxConfigDTO created = taxConfigService.createTaxConfig(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tax config", description = "Updates the tax config with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax config updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Tax config not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<TaxConfigDTO> updateTaxConfig(@PathVariable Long id, @Valid @RequestBody TaxConfigDTO dto) {
        log.info("REST request to update tax config: {}", id);
        TaxConfigDTO updated = taxConfigService.updateTaxConfig(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete tax config", description = "Soft-deletes the tax config with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tax config deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tax config not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<ResponseDTO> deleteTaxConfig(@PathVariable Long id) {
        log.info("REST request to delete tax config: {}", id);
        taxConfigService.deleteTaxConfig(id);
        return ResponseEntity.ok(ResponseDTO.ok("Tax config deleted successfully"));
    }

    @GetMapping("/names")
    @Operation(summary = "Get tax config names for dropdown", description = "Returns list of tax config IDs and display names for dropdown menus")
    @ApiResponse(responseCode = "200", description = "List of tax config names retrieved successfully")
    public ResponseEntity<List<Map<String, Object>>> getTaxConfigNames() {
        log.info("REST request to get tax config names for dropdown");
        List<Map<String, Object>> taxConfigs = taxConfigService.getTaxConfigIdAndNameList();
        return ResponseEntity.ok(taxConfigs);
    }

    @PostMapping("/paginated")
    @Operation(summary = "Search tax configs with pagination", description = "Returns a paginated list of tax configs with optional search term filtering by country code, tax type, tax name, or region code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of tax configs retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<TaxConfigDTO>> searchTaxConfigs(
            @Valid @RequestBody PageRequestDTO<TaxConfigSearchDTO> pageRequestDTO) {
        log.info("REST request to search tax configs with pagination: page={}, size={}",
                pageRequestDTO.getPage(), pageRequestDTO.getPageSize());
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<TaxConfigDTO> response;
        if (pageRequestDTO.getSearchCriteria() != null) {
            response = taxConfigService.searchTaxConfigs(pageRequestDTO.getSearchCriteria(), pageable);
        } else {
            response = taxConfigService.getAllTaxConfigsPaged(pageable);
        }
        return ResponseEntity.ok(response);
    }
}
