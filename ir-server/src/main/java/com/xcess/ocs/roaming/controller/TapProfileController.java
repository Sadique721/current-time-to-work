package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.roaming.dto.TapProfileDTO;
import com.xcess.ocs.roaming.dto.TapProfileSearchDTO;
import com.xcess.ocs.roaming.service.TapProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Slf4j
@RestController
@RequestMapping("api/v1/roaming")
@RequiredArgsConstructor
@Tag(name = "TAP Profile Management",
     description = "CRUD for TAP profiles. Profiles group field mappings and are assigned to partners via tap_out_config.")
public class TapProfileController {

    private final TapProfileService service;

    @Operation(summary = "TAP profiles dropdown",
               description = "Returns id and profileName for all profiles. Use for dropdown selection when assigning a profile to a ROAMING partner.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Dropdown list returned successfully"))
    @GetMapping("/tap-profiles/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getProfileDropdown() {
        log.info("REST GET /tap-profiles/dropdown");
        return ResponseEntity.ok(service.getProfileDropdown());
    }

    @Operation(summary = "List all TAP profiles",
               description = "Returns all named TAP profiles including their field mapping overrides.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Profile list returned successfully"))
    @GetMapping("/tap-profiles")
    public ResponseEntity<List<TapProfileDTO>> getAllProfiles() {
        log.info("REST GET /tap-profiles");
        return ResponseEntity.ok(service.getAllProfiles());
    }

    @Operation(summary = "Get a TAP profile by ID",
               description = "Returns a single TAP profile with all its field mapping overrides.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/tap-profiles/{id}")
    public ResponseEntity<TapProfileDTO> getProfile(
            @Parameter(description = "Unique ID of the TAP profile", example = "101", required = true)
            @PathVariable Long id) {
        log.info("REST GET /tap-profiles/{}", id);
        return ResponseEntity.ok(service.getProfile(id));
    }

    @Operation(summary = "Search TAP profiles (paginated)",
               description = "Returns a paginated, filtered list of TAP profiles. " +
                             "Supports filtering by profileName (partial), description (partial), and isActive.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Paginated profile list returned successfully"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Pagination and optional search criteria",
        content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "Filter active profiles",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": { "isActive": true }
                    }"""),
            @ExampleObject(name = "Search by name",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": { "profileName": "EU" }
                    }"""),
            @ExampleObject(name = "No filter — all profiles",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": {}
                    }""")
        })
    )
    @PostMapping("/tap-profiles/paginated")
    public ResponseEntity<PageResponseDTO<TapProfileDTO>> searchProfiles(
            @Valid @RequestBody PageRequestDTO<TapProfileSearchDTO> req) {
        log.info("REST POST /tap-profiles/paginated");
        Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize());
        TapProfileSearchDTO criteria = req.getSearchCriteria() != null
                ? req.getSearchCriteria() : new TapProfileSearchDTO();
        return ResponseEntity.ok(service.searchProfiles(criteria, pageable));
    }

    @Operation(summary = "Create a new TAP profile",
               description = "Creates a named profile and links the specified field mappings to it.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Profile created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed — profileName missing",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Referenced tapFieldMappingId does not exist",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Profile definition with optional field mapping overrides",
        content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "EU Standard Profile",
                value = """
                    {
                      "profileName": "EU_Standard_Profile",
                      "description": "Standard field mappings for European Union roaming partners",
                      "isActive": true,
                      "fieldMappings": [
                        { "tapFieldMappingId": 1, "customDefaultValue": null, "isMandatoryOverride": true },
                        { "tapFieldMappingId": 2, "customDefaultValue": "99999999999999", "isMandatoryOverride": null }
                      ]
                    }"""),
            @ExampleObject(name = "GPRS Data-only Profile",
                value = """
                    {
                      "profileName": "GPRS_Data_Profile",
                      "description": "High-precision GPRS data fields for data-heavy roaming partners",
                      "isActive": true,
                      "fieldMappings": [
                        { "tapFieldMappingId": 3, "customDefaultValue": "0", "isMandatoryOverride": true }
                      ]
                    }""")
        })
    )
    @PostMapping("/tap-profiles")
    public ResponseEntity<TapProfileDTO> createProfile(@Valid @RequestBody TapProfileDTO req) {
        log.info("REST POST /tap-profiles: profileName={}", req.getProfileName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createProfile(req));
    }

    @Operation(summary = "Update a TAP profile",
               description = "Fully replaces the profile's metadata and field mapping overrides.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Profile or referenced tapFieldMappingId not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/tap-profiles/{id}")
    public ResponseEntity<TapProfileDTO> updateProfile(
            @Parameter(description = "Unique ID of the TAP profile to update", example = "101", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TapProfileDTO req) {
        log.info("REST PUT /tap-profiles/{}", id);
        return ResponseEntity.ok(service.updateProfile(id, req));
    }

    @Operation(summary = "Delete a TAP profile",
               description = "Soft-deletes the profile.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Profile not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/tap-profiles/{id}")
    public ResponseEntity<Void> deleteProfile(
            @Parameter(description = "Unique ID of the TAP profile to delete", example = "101", required = true)
            @PathVariable Long id) {
        log.info("REST DELETE /tap-profiles/{}", id);
        service.deleteProfile(id);
        return ResponseEntity.noContent().build();
    }
}
