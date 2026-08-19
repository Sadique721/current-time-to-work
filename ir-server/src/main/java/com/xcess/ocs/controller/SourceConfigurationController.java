package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.SourceConfigurationSearchDTO;
import com.xcess.ocs.service.SourceConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/api/source-config")
@Tag(name = "Source Configuration", description = "API for managing source configurations")
@Slf4j
public class SourceConfigurationController {

    @Autowired
    private SourceConfigurationService sourceConfigurationService;

    @GetMapping
    @Operation(summary = "Get all source configurations",
            description = "Retrieves all source configurations from the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved configurations",
            content = @Content(schema = @Schema(implementation = SourceConfigurationDTO.class)))
    public ResponseEntity<List<SourceConfigurationDTO>> getAllConfigurations() {
        log.info("REST request to get all Source Configurations");
        List<SourceConfigurationDTO> configurations = sourceConfigurationService.getAllConfigurations();
        log.info("Successfully retrieved all Source Configurations");
        return ResponseEntity.ok(configurations);
    }

    @PostMapping("/paginated")
    @Operation(
            summary = "Get a paginated list of source configurations by POST request",
            description = "Returns a paginated list of source configurations with search functionality"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "HTTP Status BAD REQUEST",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    public ResponseEntity<PageResponseDTO<SourceConfigurationDTO>> getConfigurationsInPagesByPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with source configuration search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PageRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "SourceConfigSearchRequest",
                                    summary = "Example source configuration search request",
                                    value = """
                                                {
                                                  "page": 1,
                                                  "pageSize": 5,
                                                  "searchCriteria": {
                                                    "searchTerm": "topic1",
                                                    "status": "enabled"
                                                  }
                                                }
                                                """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<SourceConfigurationSearchDTO> pageRequestDTO) {

        log.info("REST request to get paginated Source Configurations with search criteria");
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<SourceConfigurationDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            log.info("Searching with criteria: {}", pageRequestDTO.getSearchCriteria());
            response = sourceConfigurationService.searchSourceConfigurations(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered source configurations in a page");
        } else {
            log.info("No search criteria provided, retrieving all source configurations");
            response = sourceConfigurationService.getConfigurationsInPagesByPost(pageable);
            log.info("Retrieved all source configurations in a page");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get source configuration by ID",
            description = "Retrieves a specific source configuration by its ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved configuration",
            content = @Content(schema = @Schema(implementation = SourceConfigurationDTO.class)))
    public ResponseEntity<SourceConfigurationDTO> getConfigurationById(
            @Parameter(description = "ID of the configuration to retrieve", required = true)
            @PathVariable Long id) {
        log.info("REST request to get Source Configuration with ID: {}", id);
        SourceConfigurationDTO configuration = sourceConfigurationService.getConfigurationById(id);
        log.info("Successfully retrieved Source Configuration with ID: {}", id);
        return ResponseEntity.ok(configuration);
    }

    @PostMapping
    @Operation(summary = "Create a new source configuration",
            description = "Creates a new source configuration entry in the system")
    @ApiResponse(responseCode = "201", description = "Configuration created successfully",
            content = @Content(schema = @Schema(implementation = SourceConfigurationDTO.class)))
    public ResponseEntity<SourceConfigurationDTO> createSourceConfiguration(@Valid @RequestBody
                                                         @Parameter(description = "Source configuration details",
                                                                 required = true)
                                                         SourceConfigurationDTO dto) {
        log.info("REST request to create Source Configuration: {}", dto.getTopicName());
        SourceConfigurationDTO createdConfiguration = sourceConfigurationService.createSourceConfiguration(dto);
        log.info("Successfully created Source Configuration with ID: {}", createdConfiguration.getSourceId());
        return new ResponseEntity<>(createdConfiguration, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a source configuration",
            description = "Updates an existing source configuration by ID")
    @ApiResponse(responseCode = "200", description = "Configuration updated successfully",
            content = @Content(schema = @Schema(implementation = SourceConfigurationDTO.class)))
    public ResponseEntity<SourceConfigurationDTO> updateSourceConfiguration(
            @Parameter(description = "ID of the configuration to update", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated configuration details", required = true)
            @RequestBody SourceConfigurationDTO dto) {
        log.info("REST request to update Source Configuration with ID: {}", id);
        SourceConfigurationDTO updatedConfiguration = sourceConfigurationService.updateSourceConfiguration(id, dto);
        log.info("Successfully updated Source Configuration with ID: {}", id);
        return ResponseEntity.ok(updatedConfiguration);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a source configuration",
            description = "Deletes an existing source configuration by ID")
    @ApiResponse(responseCode = "204", description = "Configuration deleted successfully")
    public ResponseEntity<ResponseDTO> deleteSourceConfiguration(
            @Parameter(description = "ID of the configuration to delete", required = true)
            @PathVariable Long id) {
        log.info("REST request to delete Source Configuration with ID: {}", id);
        sourceConfigurationService.deleteSourceConfiguration(id);
        log.info("Successfully deleted Source Configuration with ID: {}", id);
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }
}