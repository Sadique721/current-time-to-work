package com.xcess.ocs.controller;

import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.PartnerSearchDTO;
import com.xcess.ocs.service.PartnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.xcess.ocs.dto.ResponseDTO.ok;

/**
 * REST Controller for Partner management.
 * Provides endpoints for CRUD operations, search, and code generation.
 * Base path: /api/partners
 */
@RestController
@RequestMapping("/api/partners")
@Tag(name = "1. Partners", description = "Endpoints for managing partners")
@RequiredArgsConstructor
@Slf4j
public class PartnerController {

    private final PartnerService partnerService;

    /**
     * Creates a new partner.
     * If a deleted partner with the same name or code exists, it will be restored.
     * 
     * @param partnerDTO the partner data to create
     * @return created partner with HTTP 201 status
     */
    @Operation(summary = "Create a new partner", description = "Creates a new partner and returns the created partner")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<PartnerDTO> createPartner(@Valid @RequestBody PartnerDTO partnerDTO) {
        log.info("REST request to create partner: {}", partnerDTO.getPartnerName());
        PartnerDTO createdPartner = partnerService.createPartner(partnerDTO);
        log.info("Partner created successfully with ID: {}", createdPartner.getPartnerId());
        return new ResponseEntity<>(createdPartner, HttpStatus.CREATED);
    }

    /**
     * Retrieves all non-deleted partners.
     * 
     * @return list of all partners with HTTP 200 status
     */
    @Operation(summary = "Get all partners", description = "Returns a list of all partners")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping
    public ResponseEntity<List<PartnerDTO>> getAllPartners() {
        log.info("REST request to get all partners");
        List<PartnerDTO> partners = partnerService.getAllPartners();
        log.info("Retrieved {} partners", partners.size());
        return ResponseEntity.ok(partners);
    }

    /**
     * Retrieves partners in paginated format with optional search criteria.
     * Supports filtering by name, code, type, status, and country.
     * 
     * @param pageRequestDTO pagination parameters and search criteria
     * @return paginated list of partners with HTTP 200 status
     */
    @Operation(summary = "Get paginated and filtered list of partners",
            description = "Returns a paginated list of partners with search functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<PartnerDTO>> getPartnersInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with partner search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PageRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "Search All Fields",
                                    summary = "Search by partner name or type",
                                    value = """
                                                {
                                                    "page": 1,
                                                    "pageSize": 5,
                                                    "searchCriteria": {
                                                        "partnerName": "John",
                                                        "partnerType": "CUSTOMER"
                                                    }
                                                }
                                                """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<PartnerSearchDTO> pageRequestDTO) {
        log.info("REST request to get partners in page with search criteria");

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<PartnerDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = partnerService.searchPartners(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered partners in a page");
        } else {
            response = partnerService.getPartnersInPage(pageable);
            log.info("Retrieved all partners in a page");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a specific partner by its ID.
     * 
     * @param id the partner ID
     * @return partner details with HTTP 200 status
     */
    @Operation(summary = "Get partner by ID", description = "Returns a partner by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<PartnerDTO> getPartnerById(@PathVariable Long id) {
        log.info("REST request to get partner by ID: {}", id);
        PartnerDTO partner = partnerService.getPartnerById(id);
        return ResponseEntity.ok(partner);
    }

    /**
     * Updates an existing partner.
     * Validates that new name/code doesn't conflict with existing deleted partners.
     * 
     * @param id the partner ID to update
     * @param partnerDTO the updated partner data
     * @return updated partner with HTTP 200 status
     */
    @Operation(summary = "Update partner", description = "Updates an existing partner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<PartnerDTO> updatePartner(@PathVariable Long id, @Valid @RequestBody PartnerDTO partnerDTO) {
        log.info("REST request to update partner with ID: {}", id);
        PartnerDTO updatedPartner = partnerService.updatePartner(id, partnerDTO);
        return ResponseEntity.ok(updatedPartner);
    }

    /**
     * Soft deletes a partner by its ID.
     * Partner cannot be deleted if referenced by any active account.
     * 
     * @param id the partner ID to delete
     * @return HTTP 204 No Content on success
     */
    @Operation(summary = "Delete partner", description = "Deletes a partner by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePartner(@PathVariable Long id) {
        log.info("REST request to delete partner with ID: {}", id);
        partnerService.deletePartner(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves IDs and names of all partners.
     * Used for dropdown selections.
     * 
     * @return list of partner IDs and names with HTTP 200 status
     */
    @Operation(summary = "Get all partner IDs and names", description = "Returns IDs and names of all partners for dropdown selections")
    @GetMapping("/partnerIds")
    public ResponseEntity<List<Map<String,Object>>> getAllPartnerIds() {
        List<Map<String,Object>> partnerIds = partnerService.getAllPartnerIds();
        return ResponseEntity.ok(partnerIds);
    }

    @Operation(summary = "Get all partner summaries", description = "Returns partner ID, name and type for all partners")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllPartnerSummaries() {
        log.info("REST request to get all partner summaries");
        List<Map<String, Object>> partners = partnerService.getAllPartnerSummaries();
        return ResponseEntity.ok(partners);
    }

    /**
     * Generates a unique partner code.
     * The code is guaranteed not to be used by any partner (including deleted ones).
     * Format: PTR-XXXXXX (prefix + 6 alphanumeric characters)
     * 
     * @return unique partner code with HTTP 200 status
     */
    @Operation(summary = "Generate unique partner code", description = "Generates a unique partner code that is not used by any partner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/generate-code")
    public ResponseEntity<Map<String, String>> generatePartnerCode() {
        log.info("REST request to generate unique partner code");
        String code = partnerService.generateUniquePartnerCode();
        return ResponseEntity.ok(Map.of("partnerCode", code));
    }

    @Operation(summary = "Get partners by partner type",
               description = "Returns list of partners based on partner type. CUSTOMER shows customer partners, VENDOR shows vendor partners, BOTH shows all partners.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> getPartnersByPartnerType(
            @RequestParam(defaultValue = "BOTH") String partnerType) {
        log.info("REST request to get partners for partner type: {}", partnerType);
        List<Map<String, Object>> partners = partnerService.getPartnersByPartnerType(partnerType);
        log.info("Retrieved {} partners", partners.size());
        return ResponseEntity.ok(partners);
    }

    @Operation(summary = "Get partner SFTP configuration",
            description = "Returns the SFTP config, route type, and clearing house info for a partner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/sftp-config")
    public ResponseEntity<PartnerSftpConfigDTO> getPartnerSftpConfig(@PathVariable Long id) {
        log.info("REST request to get SFTP config for partner ID: {}", id);
        PartnerSftpConfigDTO config = partnerService.getPartnerSftpConfig(id);
        return ResponseEntity.ok(config);
    }

    @Operation(summary = "Update partner SFTP configuration",
            description = "Partially updates the SFTP config, route type, and/or clearing house for a partner")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/sftp-config")
    public ResponseEntity<ResponseDTO> updatePartnerSftpConfig(
            @PathVariable Long id,
            @Valid @RequestBody PartnerSftpConfigDTO dto) {
        log.info("REST request to update SFTP config for partner ID: {}", id);
        ResponseDTO response = partnerService.updatePartnerSftpConfig(id, dto);
        return ResponseEntity.ok(response);
    }

}