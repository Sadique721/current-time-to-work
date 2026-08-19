package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.PulseSearchDTO;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.service.PulseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pulse")
@Tag(
    name = "4. Pulse",
    description = "Endpoints for managing Pulse configurations"
)
public class PulseController {

    private static final Logger log = LoggerFactory.getLogger(PulseController.class);

    @Autowired
    private PulseService pulseService;

    // Create a new Pulse configuration
    @Operation(
        summary = "Create a new Pulse configuration",
        description = "Creates a new Pulse configuration by entering Pulse object"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "HTTP Status CREATED"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "HTTP Status CONFLICT",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<PulseDTO> createPulse(@Valid @RequestBody PulseDTO pulseDTO) {
        log.info("REST request to create Pulse");
        try {
            PulseDTO createdPulse = pulseService.createPulse(pulseDTO);
            log.info("Successfully created Pulse: {}", createdPulse);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPulse);
        } catch (DataIntegrityViolationException ex) {
            log.error("Duplicate name exception for Pulse: {}", pulseDTO.getPulseName(), ex);
            throw new DuplicateNameException("Pulse", pulseDTO.getPulseName());
        }
    }

    // Get a paginated list of Pulse configurations by POST request
    @Operation(
        summary = "Get a paginated list of Pulse configurations by POST request",
        description = "Returns a paginated list of Pulse configurations"
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
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<PulseDTO>> getPulsesInPagesByPost(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with pulse search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PageRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "PulseSearchRequest",
                                    summary = "Example pulse search request",
                                    value = """
                                                {
                                                  "page": 1,
                                                  "pageSize": 5,
                                                  "searchCriteria": {
                                                    "searchTerm": "Voice_60Sec",
                                                    "serviceType": "VOICE"
                                                  }
                                                }
                                                """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<PulseSearchDTO> pageRequestDTO) {
        log.info("REST request to get paginated Pulse with search criteria");
        
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<PulseDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = pulseService.searchPulses(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered pulses in a page");
        } else {
            response = pulseService.getPulsesInPagesByPost(pageable);
            log.info("Retrieved all pulses in a page");
        }

        return ResponseEntity.ok(response);
    }

    // Get a list of all Pulse configurations
    @Operation(
        summary = "Get all Pulse configurations",
        description = "Returns a list of all Pulse configurations"
    )
    @ApiResponse(
        responseCode = "200",
        description = "HTTP Status OK"
    )
    @GetMapping
    public ResponseEntity<List<PulseDTO>> getAllPulses() {
        log.info("Entering getAllPulses");
        List<PulseDTO> pulses = pulseService.getAllPulses();
        log.info("Successfully retrieved all Pulses");
        return ResponseEntity.ok(pulses);
    }

    // Get a paginated list of Pulse configurations
//    @Operation(
//        summary = "Get a paginated list of Pulse configurations",
//        description = "Returns a paginated list of Pulse configurations"
//    )
//    @ApiResponses({
//            @ApiResponse(
//                    responseCode = "200",
//                    description = "HTTP Status OK"
//            ),
//            @ApiResponse(
//                    responseCode = "400",
//                    description = "HTTP Status BAD REQUEST",
//                    content = @Content(
//                            schema = @Schema(implementation = ErrorResponseDTO.class)
//                    )
//            )
//    })
//    @GetMapping("/in-pages")
//    public ResponseEntity<List<PulseDTO>> getPulsesInPages(@RequestParam (required = false, defaultValue = "1") int pageNumber,
//                                                       @RequestParam (required = false, defaultValue = "5") int pageSize) {
//        if (pageNumber < 1 || pageSize < 1) {
//            throw new InvalidInputException("Page number and size must be greater than 0");
//        }
//        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
//
//        return ResponseEntity.ok(pulseService.getPulsesInPages(pageable));
//    }

    // Get a specific Pulse configuration by ID
    @Operation(
        summary = "Get a specific Pulse configuration by ID",
        description = "Returns the Pulse configuration with the specified ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status NOT FOUND",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<PulseDTO> getPulseById(@PathVariable Long id) {
        log.info("Entering getPulseById with ID: {}", id);
        PulseDTO pulse = pulseService.getPulseById(id);
        log.info("Successfully retrieved Pulse with ID {}", id);
        return ResponseEntity.ok(pulse);
    }

    // Update a Pulse configuration
    @Operation(
        summary = "Update a Pulse configuration",
        description = "Updates the Pulse configuration with the specified ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "HTTP Status OK"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status NOT FOUND",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "HTTP Status CONFLICT",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<PulseDTO> updatePulse(@PathVariable Long id, @RequestBody PulseDTO pulseDTO) {
        log.info("Entering updatePulse with ID: {}", id);
        PulseDTO updatedPulse = pulseService.updatePulse(id, pulseDTO);
        log.info("Successfully updated Pulse with ID {}", id);
        return ResponseEntity.ok(updatedPulse);
    }

    // Delete a Pulse configuration
    @Operation(
        summary = "Delete a Pulse configuration",
        description = "Deletes the Pulse configuration with the specified ID"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "HTTP Status NO CONTENT"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "HTTP Status NOT FOUND",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDTO.class)
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deletePulse(@PathVariable Long id) {
        log.info("Entering deletePulse with ID: {}", id);
        pulseService.deletePulse(id);
        log.info("Successfully deleted Pulse with ID: {}", id);
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }

    @GetMapping("/service-type")
    public ResponseEntity<List<Map<String, Object>>> getPulsesByServiceType(@RequestParam String serviceType) {
        log.info("Entering getPulses by serviceType: {}", serviceType);
        List<Map<String, Object>> pulses = pulseService.getPulsesByServiceType(serviceType);
        log.info("Successfully retrieved Pulses with serviceType: {}", serviceType);
        return ResponseEntity.ok(pulses);
    }
}
