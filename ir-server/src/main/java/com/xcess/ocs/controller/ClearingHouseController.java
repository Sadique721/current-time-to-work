package com.xcess.ocs.controller;

import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.ClearingHouseSearchDTO;
import com.xcess.ocs.service.ClearingHouseService;
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
@RequestMapping("/api/clearing-houses")
@RequiredArgsConstructor
@Tag(name = "2. Clearing House", description = "Endpoints for managing clearing houses (ROAMING)")
@Slf4j
public class ClearingHouseController {

    private final ClearingHouseService clearingHouseService;

    @Operation(summary = "Create a clearing house")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "409", description = "Duplicate name",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping
    public ResponseEntity<ClearingHouseDTO> create(@Valid @RequestBody ClearingHouseDTO dto) {
        log.info("REST request to create clearing house: {}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(clearingHouseService.create(dto));
    }

    @Operation(summary = "Get all clearing houses")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping
    public ResponseEntity<List<ClearingHouseDTO>> getAll() {
        log.info("REST request to get all clearing houses");
        return ResponseEntity.ok(clearingHouseService.getAll());
    }

    @Operation(summary = "Get clearing house by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClearingHouseDTO> getById(@PathVariable Long id) {
        log.info("REST request to get clearing house ID: {}", id);
        return ResponseEntity.ok(clearingHouseService.getById(id));
    }

    @Operation(summary = "Get paginated and filtered list of clearing houses",
               description = "Filter by name, type (DCH/FCH/BOTH), status (ACTIVE/INACTIVE)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<ClearingHouseDTO>> getPaginated(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "ClearingHousePageRequest",
                                    value = """
                                    {
                                        "page": 1,
                                        "pageSize": 10,
                                        "searchCriteria": {
                                            "name": "GSMA",
                                            "type": "DCH",
                                            "status": "ACTIVE"
                                        }
                                    }
                                    """
                            )
                    )
            )
            @Valid @RequestBody PageRequestDTO<ClearingHouseSearchDTO> pageRequestDTO) {
        log.info("REST request to get paginated clearing houses");
        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        ClearingHouseSearchDTO criteria = pageRequestDTO.getSearchCriteria();
        String name = criteria != null ? criteria.getName() : null;
        var type   = criteria != null ? criteria.getType() : null;
        var status = criteria != null ? criteria.getStatus() : null;
        return ResponseEntity.ok(clearingHouseService.search(name, type, status, pageable));
    }

    @Operation(summary = "Get clearing house names for dropdown (active only)")
    @GetMapping("/names")
    public ResponseEntity<List<Map<String, Object>>> getDropdown() {
        return ResponseEntity.ok(clearingHouseService.getDropdown());
    }

    @Operation(summary = "Update a clearing house")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate name",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ClearingHouseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClearingHouseDTO dto) {
        log.info("REST request to update clearing house ID: {}", id);
        return ResponseEntity.ok(clearingHouseService.update(id, dto));
    }

    @Operation(summary = "Get clearing house SFTP configuration",
            description = "Returns the SFTP config fields for a clearing house")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}/sftp-config")
    public ResponseEntity<ClearingHouseSftpConfigDTO> getSftpConfig(@PathVariable Long id) {
        log.info("REST request to get SFTP config for clearing house ID: {}", id);
        return ResponseEntity.ok(clearingHouseService.getSftpConfig(id));
    }

    @Operation(summary = "Update clearing house SFTP configuration",
            description = "Partially updates only the SFTP config fields for a clearing house")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PatchMapping("/{id}/sftp-config")
    public ResponseEntity<ResponseDTO> updateSftpConfig(
            @PathVariable Long id,
            @Valid @RequestBody ClearingHouseSftpConfigDTO dto) {
        log.info("REST request to update SFTP config for clearing house ID: {}", id);
        return ResponseEntity.ok(clearingHouseService.updateSftpConfig(id, dto));
    }

    @Operation(summary = "Delete a clearing house (soft delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
            @ApiResponse(responseCode = "409", description = "Referenced by active partner",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> delete(@PathVariable Long id) {
        log.info("REST request to delete clearing house ID: {}", id);
        clearingHouseService.delete(id);
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }
}
