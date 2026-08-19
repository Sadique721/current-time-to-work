package com.xcess.ocs.controller;

import com.xcess.ocs.dto.AgreementDTO;
import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.dto.search.AgreementSearchDTO;
import com.xcess.ocs.service.AgreementService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/agreements")
@RequiredArgsConstructor
@Tag(name = "Agreement", description = "Agreement management APIs for billing and settlement configuration")
public class AgreementController {

    private final AgreementService agreementService;

    @PostMapping
    @Operation(summary = "Create a new agreement", description = "Creates a new agreement with settlement types, line of business, accounts, and tax configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Agreement created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation failure", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Agreement code already exists", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AgreementDTO> createAgreement(@Valid @RequestBody AgreementDTO dto) {
        log.info("Creating agreement: {}", dto.getAgreementCode());
        AgreementDTO created = agreementService.createAgreement(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agreement by ID", description = "Returns the agreement details for the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement found"),
            @ApiResponse(responseCode = "404", description = "Agreement not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AgreementDTO> getAgreement(@PathVariable Long id) {
        log.info("Fetching agreement: {}", id);
        return ResponseEntity.ok(agreementService.getAgreementById(id));
    }

    @GetMapping
    @Operation(summary = "Get all agreements", description = "Returns a list of all active agreements")
    @ApiResponse(responseCode = "200", description = "List of agreements retrieved successfully")
    public ResponseEntity<List<AgreementDTO>> getAllAgreements() {
        log.info("Fetching all agreements");
        return ResponseEntity.ok(agreementService.getAllAgreements());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing agreement", description = "Updates the agreement with the specified ID. Agreement code cannot be modified.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agreement updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation failure", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Agreement not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<AgreementDTO> updateAgreement(@PathVariable Long id, @Valid @RequestBody AgreementDTO dto) {
        log.info("Updating agreement: {}", id);
        return ResponseEntity.ok(agreementService.updateAgreement(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an agreement", description = "Soft-deletes the agreement with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Agreement deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Agreement not found", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteAgreement(@PathVariable Long id) {
        log.info("Deleting agreement: {}", id);
        agreementService.deleteAgreement(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/paginated")
    @Operation(summary = "Search agreements with pagination", description = "Returns a paginated list of agreements, optionally filtered by search term")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of agreements retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination parameters", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<AgreementDTO>> getAgreementsInPage(
            @Valid @RequestBody PageRequestDTO<AgreementSearchDTO> pageRequestDTO) {
        log.info("REST request to get a filtered list of agreements in page");
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<AgreementDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = agreementService.searchAgreements(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered agreements in a page");
        } else {
            response = agreementService.getAgreementsInPage(pageable);
            log.info("Retrieved all agreements in a page");
        }

        return ResponseEntity.ok(response);
    }

}
