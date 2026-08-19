package com.xcess.ocs.controller;

import com.xcess.ocs.dto.SourceCdrConfigurationDTO;
import com.xcess.ocs.service.SourceCdrConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/source-cdr-config")
@Tag(name = "Source CDR Configuration", description = "API for managing source CDR configurations")
public class SourceCdrConfigurationController {

    private final SourceCdrConfigurationService sourceCdrConfigurationService;

    @GetMapping("/source/{sourceId}")
    @Operation(summary = "Get all CDR configurations for a source",
            description = "Retrieves all CDR field configurations for a specific source ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved configurations",
            content = @Content(schema = @Schema(implementation = SourceCdrConfigurationDTO.class)))
    public ResponseEntity<List<SourceCdrConfigurationDTO>> getConfigurationsBySourceId(
            @Parameter(description = "ID of the source to retrieve configurations for", required = true)
            @PathVariable Long sourceId) {
        log.info("REST request to get Source CDR Configurations for source ID: {}", sourceId);
        List<SourceCdrConfigurationDTO> configurations = sourceCdrConfigurationService.getConfigurationsBySourceId(sourceId);
        log.info("Successfully retrieved Source CDR Configurations for source ID: {}", sourceId);
        return ResponseEntity.ok(configurations);
    }

    @PutMapping("/update-sequences/{sourceId}")
    @Operation(
        summary = "Update field sequences for a source", 
        description = "Updates the sequence numbers for specified CDR IDs of a source. " +
                    "Enter sequences of all CDR IDs for a given source to update properly."
    )
    @ApiResponse(responseCode = "200", description = "Sequences updated successfully",
            content = @Content(schema = @Schema(implementation = SourceCdrConfigurationDTO.class)))
    public ResponseEntity<List<SourceCdrConfigurationDTO>> updateSequences(
            @Parameter(description = "ID of the source to update sequences for", required = true)
            @PathVariable Long sourceId,
            @Parameter(description = "List of field updates with IDs and new sequences", required = true)
            @RequestBody List<SourceCdrConfigurationDTO> fieldUpdates) {

        log.info("REST request to update sequences for Source CDR Configuration with source ID: {}", sourceId);
        log.debug("Field sequences to update: {}", fieldUpdates);

        // Validate the request for duplicate sequences
        Map<Integer, Long> sequencesToFieldIds = new HashMap<>();
        for (SourceCdrConfigurationDTO update : fieldUpdates) {
            if (update.getSequence() < 1 || update.getSequence() > 10) {
                throw new IllegalArgumentException("Sequence Number must be from 1 to 10");
            }
            if (sequencesToFieldIds.containsKey(update.getSequence())) {
                throw new IllegalArgumentException(
                        String.format("Sequence update failed: Sequence %d is used multiple times for field IDs %d and %d. " +
                                        "Each sequence must be unique.",
                                update.getSequence(),
                                sequencesToFieldIds.get(update.getSequence()),
                                update.getId())
                );
            }
            sequencesToFieldIds.put(update.getSequence(), update.getId());
        }

        // Process updates if validation passes
        List<SourceCdrConfigurationDTO> results = new ArrayList<>();
        for (SourceCdrConfigurationDTO update : fieldUpdates) {
            try {
                log.debug("Updating sequence for field ID {} to {} for source ID: {}",
                        update.getId(), update.getSequence(), sourceId);
                SourceCdrConfigurationDTO result = sourceCdrConfigurationService.updateSequence(
                        sourceId, update.getId(), update.getSequence());
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to update sequence for field ID {} to {} for source ID: {}",
                        update.getId(), update.getSequence(), sourceId, e);
                throw new IllegalArgumentException("Sequence update failed: " + e.getMessage());
            }
        }
        log.info("Successfully updated sequences for Source CDR Configuration with source ID: {}", sourceId);
        return ResponseEntity.ok(results);
    }
}
