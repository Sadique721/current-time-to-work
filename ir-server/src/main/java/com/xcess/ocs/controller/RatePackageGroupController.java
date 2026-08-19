package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.request.ReorderPackagesRequestDTO;
import com.xcess.ocs.dto.search.RatePackageGroupSearchDTO;
import com.xcess.ocs.entity.RatePackageGroup;
import com.xcess.ocs.service.RatePackageGroupService;
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
@RequestMapping("/api/rate-package-groups")
@RequiredArgsConstructor
@Tag(name = "7. Rate Package Group", description = "Endpoints for rate package groups")
@Slf4j
public class RatePackageGroupController {

        private final RatePackageGroupService ratePackageGroupService;

        @Operation(summary = "Create a rate package group", description = "Create a new rate package group after entering RatePackageGroupDTO")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping
        public ResponseEntity<ResponseDTO> createRatePackageGroup(
                        @Valid @RequestBody RatePackageGroupDTO dto) {
                log.info("REST request to create rate package group: {}", dto.getRatePackageGroupName());
                ratePackageGroupService.createRatePackageGroup(dto);
                log.info("Rate package group created successfully");
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseDTO.ok(ResponseConstants.MESSAGE_201));
        }

        @Operation(summary = "Get all rate package groups", description = "Returns a list of all rate package groups")
        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        @GetMapping
        public ResponseEntity<List<RatePackageGroupDTO>> getAllRatePackageGroups() {
                log.info("REST request to get all rate package groups");
                List<RatePackageGroupDTO> groups = ratePackageGroupService.getAllRatePackageGroups();
                log.info("Retrieved {} rate package groups", groups.size());
                return ResponseEntity.ok(groups);
        }

        @Operation(summary = "Get paginated and filtered list of rate package groups",
                description = "Returns a paginated list of rate package groups with search functionality")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping("/paginated")
        public ResponseEntity<PageResponseDTO<RatePackageGroupDTO>> getRatePackageGroupsInPage(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        required = true,
                        description = "Pagination request with rate package group search criteria",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = PageRequestDTO.class),
                                examples = @ExampleObject(
                                        name = "RatePackageGroupPageRequest",
                                        value = """
                                    {
                                        "page": 1,
                                        "pageSize": 5,
                                        "searchCriteria": {
                                            "searchTerm": "Basic Group",
                                            "packageType": "SELLING"
                                        }
                                    }
                                    """
                                )
                        )
                )
                @Valid @RequestBody PageRequestDTO<RatePackageGroupSearchDTO> pageRequestDTO) {
                log.info("REST request to get a filtered list of rate package groups in page");
                Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
                PageResponseDTO<RatePackageGroupDTO> response;

                if (pageRequestDTO.getSearchCriteria() != null) {
                        response = ratePackageGroupService.searchRatePackageGroups(
                                pageRequestDTO.getSearchCriteria().getSearchTerm(),
                                pageRequestDTO.getSearchCriteria().getPackageType(),
                                pageable
                        );
                        log.info("Retrieved filtered rate package groups in a page");
                } else {
                        response = ratePackageGroupService.getRatePackageGroupsInPages(pageable);
                        log.info("Retrieved all rate package groups in a page");
                }

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get a specific rate package group by ID", description = "Returns the rate package group with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<RatePackageGroupDTO> getRatePackageGroup(@PathVariable Long id) {
                log.info("REST request to get rate package group with ID: {}", id);
                RatePackageGroupDTO group = ratePackageGroupService.getRatePackageGroupById(id);
                log.info("Retrieved rate package group: {}", group.getRatePackageGroupName());
                return ResponseEntity.ok(group);
        }

        @Operation(summary = "Update a rate package group", description = "Updates the rate package group with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/{id}")
        public ResponseEntity<ResponseDTO> updateRatePackageGroup(
                        @PathVariable Long id,
                        @Valid @RequestBody RatePackageGroupDTO dto) {
                log.info("REST request to update rate package group with ID: {}", id);
                ratePackageGroupService.updateRatePackageGroup(id, dto);
                log.info("Rate package group updated successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }

        @Operation(summary = "Delete a rate package group", description = "Deletes the rate package group with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ResponseDTO> deleteRatePackageGroup(@PathVariable Long id) {
                log.info("REST request to delete rate package group with ID: {}", id);
                ratePackageGroupService.deleteRatePackageGroup(id);
                log.info("Rate package group deleted successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
        }


        @GetMapping("/names")
        public ResponseEntity<List<Map<String, Object>>> getPackageGroupsDropdown(
                @RequestParam("type") RatePackageGroup.PackageType packageType) {
                log.info("REST request to get package Groups dropdown by type: {}", packageType);
                return ResponseEntity.ok(ratePackageGroupService.getPackageIdAndNameByType(packageType));
        }

        @Operation(summary = "Get rate package groups by type grouped by service type",
                   description = "Returns a map with voice, sms, usage keys, each containing a list of package objects filtered by package type")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        })
        @GetMapping("/by-criteria")
        public ResponseEntity<Map<String, List<Map<String, Object>>>> getRatePackageGroupsByTypeAndService(
                        @RequestParam String type) {
                log.info("REST request to get rate package groups with type: {}", type);
                Map<String, List<Map<String, Object>>> result = ratePackageGroupService.getRatePackageGroupsByTypeAndService(type);
                log.info("Retrieved rate package groups grouped by service type");
                return ResponseEntity.ok(result);
        }

        @Operation(summary = "Reorder rate packages in a group",
                   description = "Updates the priority order and fallback selection for rate packages within a rate package group. Used for drag-and-drop reordering in UI.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/{id}/packages")
        public ResponseEntity<ResponseDTO> reorderPackages(
                        @PathVariable Long id,
                        @Valid @RequestBody ReorderPackagesRequestDTO request) {
                log.info("REST request to reorder packages for group ID: {}", id);
                ratePackageGroupService.reorderPackages(id, request);
                log.info("Packages reordered successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }
}