package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.roaming.dto.TapProfileGroupDTO;
import com.xcess.ocs.roaming.dto.TapProfileGroupSearchDTO;
import com.xcess.ocs.roaming.service.TapProfileGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/v1/roaming")
@RequiredArgsConstructor
@Tag(name = "TAP Profile Group Management",
     description = "CRUD for TAP profile groups. Groups bundle TAP profiles and are assigned to partners via SFTP settings.")
public class TapProfileGroupController {

    private final TapProfileGroupService service;

    @Operation(summary = "TAP profile groups dropdown")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Dropdown list returned successfully"))
    @GetMapping("/tap-profile-groups/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getGroupDropdown() {
        log.info("REST GET /tap-profile-groups/dropdown");
        return ResponseEntity.ok(service.getGroupDropdown());
    }

    @Operation(summary = "TAP profiles grouped by service type",
               description = "Returns tap profiles bucketed under voice, sms, and usage keys. Used for profile selection dropdowns.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Profiles by service type returned successfully"))
    @GetMapping("/tap-profiles/by-service-type")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getTapProfilesByServiceType() {
        log.info("REST GET /tap-profiles/by-service-type");
        return ResponseEntity.ok(service.getTapProfilesByServiceType());
    }

    @Operation(summary = "List all TAP profile groups")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Group list returned successfully"))
    @GetMapping("/tap-profile-groups")
    public ResponseEntity<List<TapProfileGroupDTO>> getAllGroups() {
        log.info("REST GET /tap-profile-groups");
        return ResponseEntity.ok(service.getAllGroups());
    }

    @Operation(summary = "Get a TAP profile group by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group returned successfully"),
        @ApiResponse(responseCode = "404", description = "Group not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/tap-profile-groups/{id}")
    public ResponseEntity<TapProfileGroupDTO> getGroup(
            @Parameter(description = "Unique ID of the TAP profile group", required = true)
            @PathVariable Long id) {
        log.info("REST GET /tap-profile-groups/{}", id);
        return ResponseEntity.ok(service.getGroup(id));
    }

    @Operation(summary = "Search TAP profile groups (paginated)")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Paginated group list returned successfully"))
    @PostMapping("/tap-profile-groups/paginated")
    public ResponseEntity<PageResponseDTO<TapProfileGroupDTO>> searchGroups(
            @Valid @RequestBody PageRequestDTO<TapProfileGroupSearchDTO> req) {
        log.info("REST POST /tap-profile-groups/paginated");
        Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize());
        TapProfileGroupSearchDTO criteria = req.getSearchCriteria() != null
                ? req.getSearchCriteria() : new TapProfileGroupSearchDTO();
        return ResponseEntity.ok(service.searchGroups(criteria, pageable));
    }

    @Operation(summary = "Create a new TAP profile group")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Group created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/tap-profile-groups")
    public ResponseEntity<TapProfileGroupDTO> createGroup(@Valid @RequestBody TapProfileGroupDTO req) {
        log.info("REST POST /tap-profile-groups: name={}", req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(req));
    }

    @Operation(summary = "Update a TAP profile group")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Group updated successfully"),
        @ApiResponse(responseCode = "404", description = "Group not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/tap-profile-groups/{id}")
    public ResponseEntity<TapProfileGroupDTO> updateGroup(
            @Parameter(description = "Unique ID of the TAP profile group to update", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TapProfileGroupDTO req) {
        log.info("REST PUT /tap-profile-groups/{}", id);
        return ResponseEntity.ok(service.updateGroup(id, req));
    }

    @Operation(summary = "Delete a TAP profile group")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Group deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Group not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/tap-profile-groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "Unique ID of the TAP profile group to delete", required = true)
            @PathVariable Long id) {
        log.info("REST DELETE /tap-profile-groups/{}", id);
        service.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}
