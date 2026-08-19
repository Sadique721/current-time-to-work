package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.PrefixSearchDTO;
import com.xcess.ocs.entity.PrefixType;
import com.xcess.ocs.service.PrefixService;
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
@RequestMapping("/api/prefixes")
@RequiredArgsConstructor
@Tag(name = "3. Prefix", description = "Endpoints for managing prefixes")
@Slf4j
public class PrefixController {

        private final PrefixService prefixService;

        @Operation(summary = "Get all prefixes", description = "Returns a list of all prefixes")
        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        @GetMapping
        public ResponseEntity<List<PrefixDTO>> getAllPrefixes() {
                log.info("REST request to get all prefixes");
                List<PrefixDTO> prefixes = prefixService.getAllPrefixes();
                log.info("Retrieved {} prefixes", prefixes.size());
                return ResponseEntity.ok(prefixes);
        }

        @Operation(summary = "Get paginated and filtered list of prefixes", description = "Returns a paginated list of prefixes with search functionality")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping("/paginated")
        public ResponseEntity<PageResponseDTO<PrefixDTO>> getPrefixesInPage(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Pagination request with prefix search criteria", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageRequestDTO.class), examples = @ExampleObject(name = "Search All Fields", summary = "Search term across all fields", value = """
                                        {
                                            "page": 1,
                                            "pageSize": 5,
                                            "searchCriteria": {
                                                "searchTerm": "india"
                                            }
                                        }
                                        """))) @Valid @RequestBody PageRequestDTO<PrefixSearchDTO> pageRequestDTO) {
                log.info("REST request to get prefixes in page with search criteria");

                Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
                PageResponseDTO<PrefixDTO> response;

                if (pageRequestDTO.getSearchCriteria() != null) {
                        response = prefixService.searchPrefixes(pageRequestDTO.getSearchCriteria(), pageable);
                        log.info("Retrieved filtered prefixes in a page");
                } else {
                        response = prefixService.getPrefixesInPage(pageable);
                        log.info("Retrieved all prefixes in a page");
                }

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get prefixes by list of IDs",
                description = "Returns prefix details for the given list of IDs. Used by zone DROPDOWN mode resolution.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                                content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/by-ids")
        public ResponseEntity<List<PrefixDTO>> getPrefixesByIds(@RequestParam List<Long> ids) {
                log.info("REST request to get prefixes by IDs: {}", ids);
                return ResponseEntity.ok(prefixService.getPrefixesByIds(ids));
        }

        @Operation(summary = "Get prefixes by type",
                description = "Returns all prefixes filtered by type: INTERCONNECT or ROAMING")
        @ApiResponse(responseCode = "200", description = "HTTP Status OK")
        @GetMapping("/by-type")
        public ResponseEntity<List<PrefixDTO>> getPrefixesByType(@RequestParam PrefixType prefixType) {
                log.info("REST request to get prefixes by type: {}", prefixType);
                return ResponseEntity.ok(prefixService.getPrefixesByType(prefixType));
        }

        @Operation(summary = "Get a prefix by ID", description = "Returns the prefix with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<PrefixDTO> getPrefixById(@PathVariable Long id) {
                log.info("REST request to get prefix with ID: {}", id);
                PrefixDTO prefix = prefixService.getPrefixById(id);
                log.info("Retrieved prefix: {}", prefix.getPrefix());
                return ResponseEntity.ok(prefix);
        }

        @Operation(summary = "Create a new prefix", description = "Creates a new prefix by entering PrefixDTO")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping
        public ResponseEntity<ResponseDTO> createPrefix(@Valid @RequestBody PrefixDTO prefixDTO) {
                log.info("REST request to create prefix: {}", prefixDTO.getPrefix());
                prefixService.createPrefix(prefixDTO);
                log.info("Prefix created successfully");
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ResponseDTO.ok(ResponseConstants.MESSAGE_201));
        }

        @Operation(summary = "Delete a prefix", description = "Deletes the prefix with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<ResponseDTO> deletePrefix(@PathVariable Long id) {
                log.info("REST request to delete prefix with ID: {}", id);
                prefixService.deletePrefix(id);
                log.info("Prefix deleted successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
        }

        @Operation(summary = "Update a prefix", description = "Updates the prefix with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/{id}")
        public ResponseEntity<ResponseDTO> updatePrefix(
                        @PathVariable Long id,
                        @Valid @RequestBody PrefixDTO prefixDTO) {
                log.info("REST request to update prefix with ID: {}", id);
                prefixService.updatePrefix(id, prefixDTO);
                log.info("Prefix updated successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }

        @Operation(summary = "Update a prefix", description = "Updates the prefix with the specified ID")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                        @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PatchMapping("/{prefixId}")
        public ResponseEntity<ResponseDTO> updatePrefixFields(
                        @PathVariable Long prefixId,
                        @RequestBody Map<String, Object> updateRequest) {
                log.info("REST request to partially update prefix with ID: {}", prefixId);
                prefixService.updatePrefixName(prefixId, updateRequest);
                log.info("Prefix partially updated successfully");
                return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_UPDATE));
        }
}