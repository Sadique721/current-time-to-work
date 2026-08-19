package com.xcess.ocs.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ZoneDTO;
import com.xcess.ocs.dto.ZonePrefixOptionDTO;
import com.xcess.ocs.dto.search.ZoneSearchDTO;
import com.xcess.ocs.entity.PrefixType;
import com.xcess.ocs.service.ZoneService;
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

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zones", description = "Endpoints for managing billing zones for INTERCONNECT and ROAMING rating")
@RequiredArgsConstructor
@Slf4j
public class ZoneController {

    private final ZoneService zoneService;

    @Operation(summary = "Create a new zone")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ZoneDTO> createZone(@Valid @RequestBody ZoneDTO zoneDTO) {
        log.info("REST request to create zone: {}", zoneDTO.getZoneName());
        ZoneDTO created = zoneService.createZone(zoneDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @Operation(summary = "Get zone by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ZoneDTO> getZoneById(@PathVariable Long id) {
        return ResponseEntity.ok(zoneService.getZoneById(id));
    }

    @Operation(summary = "Get paginated and filtered list of zones")
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<ZoneDTO>> getZonesInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "Search Example",
                                    value = """
                                            {
                                                "page": 1,
                                                "pageSize": 10,
                                                "searchCriteria": {
                                                    "zoneName": "EU"
                                                }
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<ZoneSearchDTO> pageRequestDTO) {

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<ZoneDTO> response = pageRequestDTO.getSearchCriteria() != null
                ? zoneService.searchZones(pageRequestDTO.getSearchCriteria(), pageable)
                : zoneService.getZonesInPage(pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update zone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ZoneDTO> updateZone(@PathVariable Long id, @Valid @RequestBody ZoneDTO zoneDTO) {
        log.info("REST request to update zone with ID: {}", id);
        return ResponseEntity.ok(zoneService.updateZone(id, zoneDTO));
    }

    @Operation(summary = "Delete zone", description = "Soft deletes a zone by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        log.info("REST request to delete zone with ID: {}", id);
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all zone IDs and names for dropdown")
    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> getAllZoneNames() {
        return ResponseEntity.ok(zoneService.getAllZoneNames());
    }

    @Operation(
        summary = "Get prefix options for zone dropdown",
        description = "Returns a flat combined list of prefix table entries (INTERCONNECT/ROAMING) " +
                      "and country entries (by countryCode) for the zone prefix multi-select dropdown. " +
                      "Use prefixType=INTERCONNECT or prefixType=ROAMING to filter by type. " +
                      "Use search param for text filtering by prefix value, name, or country name."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/prefix-options")
    public ResponseEntity<List<ZonePrefixOptionDTO>> getPrefixOptions(
            @RequestParam(required = false) PrefixType prefixType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(zoneService.getPrefixOptions(prefixType, search));
    }
}
