package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.CountrySearchDTO;
import com.xcess.ocs.service.CountryService;
import com.xcess.ocs.service.PrefixService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

/**
 * REST Controller for Country operations.
 * Provides HTTP endpoints for CRUD operations on countries.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>GET /api/countries - Get all countries</li>
 *   <li>POST /api/countries/paginated - Search with pagination</li>
 *   <li>GET /api/countries/{id} - Get country by ID</li>
 *   <li>POST /api/countries - Create new country</li>
 *   <li>PUT /api/countries/{id} - Update country</li>
 *   <li>DELETE /api/countries/{id} - Delete country</li>
 *   <li>GET /api/countries/{id}/prefixes - Get prefixes for a country</li>
 * </ul>
 * 
 * @see CountryService
 * @see CountryDTO
 */
@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@Tag(name = "2. Country", description = "Endpoints for managing countries")
@Slf4j
public class CountryController {

    private final CountryService countryService;
    private final PrefixService prefixService;

    /**
     * Get all active (non-deleted) countries.
     * 
     * @return List of CountryDTO
     */
    @Operation(summary = "Get all countries", description = "Returns a list of all active countries")
    @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    @GetMapping
    public ResponseEntity<List<CountryDTO>> getAllCountries() {
        log.info("REST request to get all countries");
        List<CountryDTO> countries = countryService.getAllCountries();
        log.info("Retrieved {} countries", countries.size());
        return ResponseEntity.ok(countries);
    }

    /**
     * Search countries with pagination and optional search term.
     * 
     * @param pageRequestDTO pagination request containing page number, size, and search criteria
     * @return Paginated list of countries
     */
    @Operation(summary = "Get paginated list of countries", description = "Returns a paginated list of countries")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<CountryDTO>> getCountriesInPage(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Pagination request with search criteria",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = PageRequestDTO.class
                            ),
                            examples = @ExampleObject(
                                    name = "CountryPageRequest",
                                    value = """
                                            {
                                                "page": 1,
                                                "pageSize": 5,
                                                "searchCriteria": {
                                                    "searchTerm": "US"
                                                }
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<CountrySearchDTO> pageRequestDTO) {
        log.info("REST request to get a filtered list of countries in page");
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<CountryDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = countryService.searchCountries(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered countries in a page");
        } else {
            response = countryService.getCountriesInPage(pageable);
            log.info("Retrieved all countries in a page");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get country by ID.
     * 
     * @param id the country ID
     * @return CountryDTO
     */
    @Operation(summary = "Get a country by ID", description = "Returns the country with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CountryDTO> getCountryById(@PathVariable Long id) {
        log.info("REST request to get country with ID: {}", id);
        CountryDTO country = countryService.getCountryById(id);
        log.info("Retrieved country: {}", country.getName());
        return ResponseEntity.ok(country);
    }

    /**
     * Create a new country.
     * Allows duplicate country codes (e.g., USA and Canada both use "1").
     * 
     * @param countryDTO the country data
     * @return Success response
     */
    @Operation(summary = "Create a new country", description = "Creates a new country. Duplicate country codes are allowed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
            @ApiResponse(responseCode = "417", description = "HTTP Status EXPECTATION FAILED", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ResponseDTO> createCountry(@Valid @RequestBody CountryDTO countryDTO) {
        log.info("REST request to create country: {}", countryDTO.getName());
        CountryResponseDTO response = countryService.createCountry(countryDTO);
        if (response.isSuccess()) {
            log.info("Country created successfully");
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseDTO.ok(ResponseConstants.MESSAGE_201));
        }
        log.warn("Failed to create country: {}", response.getMessage());
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body(ResponseDTO.failed(response.getMessage()));
    }

    /**
     * Update an existing country.
     * 
     * @param id the country ID to update
     * @param countryDTO the updated country data
     * @return Success response
     */
    @Operation(summary = "Update a country", description = "Updates the country with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "417", description = "EXPECTATION FAILED", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> updateCountry(@PathVariable Long id,
                                                     @Valid @RequestBody CountryDTO countryDTO) {
        log.info("REST request to update country with ID: {}", id);
        CountryResponseDTO response = countryService.updateCountry(id, countryDTO);
        if (response.isSuccess()) {
            log.info("Country updated successfully");
            return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }
        log.warn("Failed to update country: {}", response.getMessage());
        return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                .body(ResponseDTO.failed(response.getMessage()));
    }

    /**
     * Delete a country (soft delete).
     * Blocks deletion if country is referenced by active Prefix or RateDetails.
     * 
     * @param id the country ID to delete
     * @return Success response
     */
    @Operation(summary = "Delete a country", description = "Soft deletes the country. Blocks if referenced by active Prefix or RateDetails.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteCountry(@PathVariable Long id) {
        log.info("REST request to delete country with ID: {}", id);
        countryService.deleteCountry(id);
        log.info("Country deleted successfully");
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }

    /**
     * Get country names and codes for dropdown.
     *
     * @return List of maps containing country name and country code
     */
    @Operation(summary = "Get country names and codes for dropdown",
               description = "Returns list of country names and codes for dropdown menus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> getCountryNames() {
        log.info("REST request to get country names and codes");
        List<Map<String, Object>> countries = countryService.getCountryNameAndCodeList();
        log.info("Retrieved {} countries", countries.size());
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/currencyCodes")
    public ResponseEntity<List<Map<String, Object>>> getCountrycurrencyCode() {
        log.info("REST request to get country currencyCode");
        List<Map<String, Object>> countries = countryService.getCountrycurrencyCode();
        log.info("Retrieved {} countries", countries.size());
        return ResponseEntity.ok(countries);
    }

    /**
     * Get country ISO codes and names for dropdown.
     *
     * @return List of maps containing ISO code and country name
     */
    @Operation(summary = "Get country ISO codes and names for dropdown",
               description = "Returns list of ISO codes and country names for dropdown menus")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/iso-codes")
    public ResponseEntity<List<Map<String, Object>>> getCountryIsoCodes() {
        log.info("REST request to get country ISO codes and names");
        List<Map<String, Object>> countries = countryService.getCountryIsoCodeList();
        log.info("Retrieved {} countries", countries.size());
        return ResponseEntity.ok(countries);
    }

    /**
     * Get all prefixes for a specific country.
     * 
     * @param id the country ID
     * @return List of PrefixDTO
     */
    @Operation(summary = "Get all prefixes for a country", description = "Returns a list of all prefixes for the country with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    })
    @GetMapping("/{id}/prefixes")
    public ResponseEntity<List<PrefixDTO>> getCountryPrefixes(@PathVariable Long id) {
        log.info("REST request to get prefixes for country with ID: {}", id);
        CountryDTO country = countryService.getCountryById(id);
        List<PrefixDTO> prefixes = prefixService.getPrefixesByCountryName(country.getName());
        log.info("Retrieved {} prefixes for country: {}", prefixes.size(), country.getName());
        return ResponseEntity.ok(prefixes);
    }
}