package com.xcess.ocs.controller;

import com.xcess.ocs.dto.OrganizationDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.ResponseDTO;
import com.xcess.ocs.dto.search.OrganizationSearchDTO;
import com.xcess.ocs.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Organization operations.
 * Provides HTTP endpoints for CRUD operations on organizations.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/organizations - Get all organizations</li>
 *   <li>GET /api/organizations/{id} - Get organization by ID</li>
 *   <li>POST /api/organizations - Create new organization</li>
 *   <li>PUT /api/organizations/{id} - Update organization</li>
 *   <li>DELETE /api/organizations/{id} - Delete organization</li>
 * </ul>
 * 
 * @see OrganizationService
 * @see OrganizationDTO
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Tag(name = "Organization", description = "Organization management APIs")
public class OrganizationController {

    private final OrganizationService organizationService;

    /**
     * Get all active organizations.
     * 
     * @return List of OrganizationDTO
     */
    @GetMapping
    @Operation(summary = "Get all organizations", description = "Returns a list of all active organizations")
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        List<OrganizationDTO> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

    /**
     * Get organization by ID.
     * 
     * @param id the organization ID
     * @return OrganizationDTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get organization by ID", description = "Returns the organization with the specified ID")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@PathVariable Long id) {
        OrganizationDTO organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }

    /**
     * Create a new organization.
     * 
     * @param dto the organization data
     * @return Created OrganizationDTO
     */
    @PostMapping
    @Operation(summary = "Create new organization", description = "Creates a new organization with validation")
    public ResponseEntity<OrganizationDTO> createOrganization(@Valid @RequestBody OrganizationDTO dto) {
        OrganizationDTO created = organizationService.createOrganization(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing organization.
     * 
     * @param id the organization ID to update
     * @param dto the updated organization data
     * @return Updated OrganizationDTO
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update organization", description = "Updates the organization with the specified ID")
    public ResponseEntity<OrganizationDTO> updateOrganization(
            @PathVariable Long id, @Valid @RequestBody OrganizationDTO dto) {
        OrganizationDTO updated = organizationService.updateOrganization(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an organization (soft delete).
     * 
     * @param id the organization ID to delete
     * @return Success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete organization", description = "Soft deletes the organization with the specified ID")
    public ResponseEntity<ResponseDTO> deleteOrganization(@PathVariable Long id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(ResponseDTO.ok("Organization deleted successfully"));
    }

    /**
     * Search organizations with pagination and optional search term.
     * 
     * @param pageRequestDTO pagination request containing page number, size, and search criteria
     * @return Paginated list of organizations
     */
    @PostMapping("/paginated")
    @Operation(summary = "Search organizations with pagination", 
               description = "Returns paginated list of organizations with optional search term")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<OrganizationDTO>> searchOrganizations(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with optional organization search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = com.xcess.ocs.dto.PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "OrganizationPageRequest",
                                    value = """
                                    {
                                        "page": 1,
                                        "pageSize": 10,
                                        "searchCriteria": {
                                            "searchTerm": "Tech"
                                        }
                                    }
                                    """
                            )
                    )
            )
            @Valid @RequestBody com.xcess.ocs.dto.PageRequestDTO<OrganizationSearchDTO> pageRequestDTO) {
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<OrganizationDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = organizationService.searchOrganizations(pageRequestDTO.getSearchCriteria(), pageable);
        } else {
            response = organizationService.getOrganizationsInPage(pageable);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get list of organization IDs and names for dropdown.
     * Used for UI dropdown selections.
     * 
     * @return List of objects containing organizationId and name
     */
    @GetMapping("/names")
    @Operation(summary = "Get organization names for dropdown", 
               description = "Returns list of organization IDs and names for dropdown menus")
    public ResponseEntity<List<Map<String, Object>>> getOrganizationNames() {
        List<Map<String, Object>> organizations = organizationService.getOrganizationIdAndNameList();
        return ResponseEntity.ok(organizations);
    }
}
