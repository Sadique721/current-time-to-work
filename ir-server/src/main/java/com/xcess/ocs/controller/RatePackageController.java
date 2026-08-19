package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.RatePackageSearchDTO;

import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.Type;
import com.xcess.ocs.service.RatePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-packages")
@Tag(name = "5. Rate Package", description = "Endpoints for managing rate packages")
@Slf4j
public class RatePackageController {

    private final RatePackageService ratePackageService;

    @Autowired
    public RatePackageController(RatePackageService ratePackageService) {
        this.ratePackageService = ratePackageService;
    }


    @Operation(summary = "Create a rate package", description = "Creates a new rate package. Optionally include rate_details in the body to create them inline. Allowed ratePackageType: DESTINATION_BASED, SOURCE_DESTINATION_BASED, ZONE_DESTINATION_BASED.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT — duplicate package name",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<RatePackageDTO> createRatePackage(
            @Valid @RequestBody RatePackageDTO ratePackageDTO) {
        log.info("REST request to create rate package: {} with type: {}",
                ratePackageDTO.getPackageName(),
                ratePackageDTO.getRatePackageType());
        RatePackageDTO createdPackage = ratePackageService.createRatePackage(ratePackageDTO);
        log.info("Rate package created successfully with ID: {}", createdPackage.getRatePackageId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPackage);
    }


    @Operation(summary = "Get all rate packages", description = "Returns a list of all rate packages")
    @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    @GetMapping
    public ResponseEntity<List<RatePackageDTO>> getAllRatePackages() {
        log.info("REST request to get all rate packages");
        List<RatePackageDTO> ratePackages = ratePackageService.getAllRatePackages();
        log.info("Retrieved {} rate packages", ratePackages.size());
        return ResponseEntity.ok(ratePackages);
    }

    @Operation(summary = "Get paginated and filtered list of rate packages",
            description = "Returns a paginated list of rate packages with optional search functionality")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<RatePackageDTO>> getRatePackagesInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with optional rate package search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "RatePackagePageRequest",
                                    value = """
                                        {
                                            "page": 1,
                                            "pageSize": 5,
                                            "searchCriteria": {
                                                "searchTerm": "Basic Package",
                                                "serviceType": "VOICE"
                                            }
                                        }
                                        """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<RatePackageSearchDTO> pageRequestDTO) {
        log.info("REST request to get a filtered list of rate packages in page");
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<RatePackageDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = ratePackageService.searchRatePackages(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered rate packages in a page");
        } else {
            response = ratePackageService.getRatePackagesInPages(pageable);
            log.info("Retrieved all rate packages in a page");
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a rate package by ID", description = "Returns the rate package with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RatePackageDTO> getPackageById(@PathVariable Long id) {
        log.info("REST request to get rate package with ID: {}", id);
        RatePackageDTO ratePackage = ratePackageService.getPackageById(id);
        log.info("Retrieved rate package: {}", ratePackage.getPackageName());
        return ResponseEntity.ok(ratePackage);
    }

    @Operation(summary = "Get rate packages by package type",
            description = "Returns a list of rate packages filtered by DESTINATION_BASED or SOURCE_DESTINATION_BASED type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<List<RatePackageDTO>> getRatePackagesByType(@PathVariable RatePackageType type) {
        log.info("REST request to get rate packages by type: {}", type);
        List<RatePackageDTO> ratePackages = ratePackageService.getRatePackagesByType(type);
        log.info("Retrieved {} rate packages of type: {}", ratePackages.size(), type);
        return ResponseEntity.ok(ratePackages);
    }

    @Operation(
            summary = "Get rate package names dropdown",
            description = "Returns a list of rate package IDs and names filtered by SELLING or BUYING type, Line of Business, and optional Service Type. Used for dropdown menus."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> getPackagesDropdown(
            @Parameter(description = "Package type filter: SELLING or BUYING", required = true)
            @RequestParam("type") Type packageType,
            @Parameter(description = "Service Type filter (optional)")
            @RequestParam(value = "serviceType", required = false) ServiceType serviceType) {
        log.info("REST request to get package dropdown by type: {}, serviceType: {}", packageType, serviceType);
        return ResponseEntity.ok(ratePackageService.getPackageIdAndNameByType(packageType, serviceType));
    }


    @Operation(summary = "Update a rate package", description = "Updates header fields of the rate package with the specified ID. "
            + "If rate_details are included, they replace the existing ones (archived to history). "
            + "If ratePackageType changes, rate_details must be provided.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST — validation error or type change without rate_details",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT — duplicate package name",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RatePackageDTO> updateRatePackage(
            @PathVariable Long id,
            @Valid @RequestBody RatePackageDTO ratePackageDTO) {
        log.info("REST request to update rate package with ID: {}", id);
        RatePackageDTO updatedPackage = ratePackageService.updateRatePackage(id, ratePackageDTO);
        log.info("Rate package updated successfully: {}", updatedPackage.getPackageName());
        return ResponseEntity.ok(updatedPackage);
    }


    @Operation(summary = "Delete a rate package", description = "Deletes the rate package with the specified ID. Fails if the package is referenced by an active association.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT — package is referenced by an active association",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteRatePackage(@PathVariable Long id) {
        log.info("REST request to delete rate package with ID: {}", id);
        ratePackageService.deleteRatePackage(id);
        log.info("Rate package deleted successfully with ID: {}", id);
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }
}