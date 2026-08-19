package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.roaming.dto.TapFieldMappingDTO;
import com.xcess.ocs.roaming.dto.TapFieldMappingSearchDTO;
import com.xcess.ocs.roaming.service.TapFieldMappingService;
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
@Tag(name = "TAP Field Mapping Management",
     description = "CRUD for the dynamic TAP field mapping dictionary. " +
                   "Field mappings define ASN.1 dot-paths and DTO column bindings used during " +
                   "TAP OUT generation and TAP IN decoding.")
public class TapFieldMappingController {

    private final TapFieldMappingService service;

    @Operation(summary = "List all TAP field mappings",
               description = "Returns the full master dictionary of TAP ASN.1 field paths. " +
                             "Fields with a null callType apply globally across all event types.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Field mapping list returned successfully"))
    @GetMapping("/tap-fields")
    public ResponseEntity<List<TapFieldMappingDTO>> getAllFields() {
        log.info("REST GET /tap-fields");
        return ResponseEntity.ok(service.getAllFields());
    }

    @Operation(summary = "TAP field mappings dropdown",
               description = "Returns id and fieldName for all field mappings. Use for dropdown selection when building profiles.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Dropdown list returned successfully"))
    @GetMapping("/tap-fields/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getFieldDropdown() {
        log.info("REST GET /tap-fields/dropdown");
        return ResponseEntity.ok(service.getFieldDropdown());
    }

    @Operation(summary = "Get a TAP field mapping by ID",
               description = "Returns a single entry from the master TAP field mapping dictionary.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Field mapping returned successfully"),
        @ApiResponse(responseCode = "404", description = "Field mapping not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/tap-fields/{id}")
    public ResponseEntity<TapFieldMappingDTO> getField(
            @Parameter(description = "Unique ID of the TAP field mapping", example = "1", required = true)
            @PathVariable Long id) {
        log.info("REST GET /tap-fields/{}", id);
        return ResponseEntity.ok(service.getField(id));
    }

    @Operation(summary = "Search TAP field mappings (paginated)",
               description = "Returns a paginated, filtered list of TAP field mappings. " +
                             "All search criteria are optional. Supports filtering by callType, " +
                             "fieldName (partial), asnPath (partial), dataType, and isMandatory.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Paginated field mapping list returned successfully"))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Pagination and optional search criteria",
        content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "Filter by callType",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": { "callType": "MOC", "isMandatory": true }
                    }"""),
            @ExampleObject(name = "Search by fieldName",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": { "fieldName": "imsi" }
                    }"""),
            @ExampleObject(name = "No filter — all fields",
                value = """
                    {
                      "page": 1, "pageSize": 10,
                      "searchCriteria": {}
                    }""")
        })
    )
    @PostMapping("/tap-fields/paginated")
    public ResponseEntity<PageResponseDTO<TapFieldMappingDTO>> searchFields(
            @Valid @RequestBody PageRequestDTO<TapFieldMappingSearchDTO> req) {
        log.info("REST POST /tap-fields/paginated");
        Pageable pageable = PageRequest.of(req.getPage() - 1, req.getPageSize());
        TapFieldMappingSearchDTO criteria = req.getSearchCriteria() != null
                ? req.getSearchCriteria() : new TapFieldMappingSearchDTO();
        return ResponseEntity.ok(service.searchFields(criteria, pageable));
    }

    @Operation(summary = "Create a new TAP field mapping",
               description = "Adds a new entry to the master field dictionary. " +
                             "The asnPath must be a valid dot-notation path in the GSMA TAP-0312 ASN.1 structure. " +
                             "Leave callType null for fields that apply to all event types.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Field mapping created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed — fieldName, asnPath, or dataType missing",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "TAP field mapping definition",
        content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "MOC — dialled digits",
                value = """
                    {
                      "callType": "MOC",
                      "fieldName": "dialledDigits",
                      "asnPath": "basicCallInformation.destination.calledNumber",
                      "dataType": "BCD_STRING",
                      "outSourceColumn": "calledNumber",
                      "inTargetColumn": "calledNumber",
                      "defaultValue": null,
                      "isMandatory": false
                    }"""),
            @ExampleObject(name = "GPRS — data volume incoming",
                value = """
                    {
                      "callType": "GPRS",
                      "fieldName": "dataVolumeIncoming",
                      "asnPath": "gprsServiceUsed.dataVolumeIncoming",
                      "dataType": "DECIMAL",
                      "outSourceColumn": "volumeIncoming",
                      "inTargetColumn": "dataVolumeIncoming",
                      "defaultValue": "0",
                      "isMandatory": true
                    }"""),
            @ExampleObject(name = "Global — IMSI (all call types)",
                value = """
                    {
                      "callType": null,
                      "fieldName": "imsi",
                      "asnPath": "basicCallInformation.chargeableSubscriber.simChargeableSubscriber.imsi",
                      "dataType": "BCD_STRING",
                      "outSourceColumn": null,
                      "inTargetColumn": "imsi",
                      "defaultValue": "00000000000000",
                      "isMandatory": true
                    }""")
        })
    )
    @PostMapping("/tap-fields")
    public ResponseEntity<TapFieldMappingDTO> createField(@Valid @RequestBody TapFieldMappingDTO req) {
        log.info("REST POST /tap-fields: fieldName={}", req.getFieldName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createField(req));
    }

    @Operation(summary = "Update a TAP field mapping",
               description = "Fully replaces an existing entry in the master field dictionary. " +
                             "Changes affect all profiles and partners that reference this field.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Field mapping updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Field mapping not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/tap-fields/{id}")
    public ResponseEntity<TapFieldMappingDTO> updateField(
            @Parameter(description = "Unique ID of the TAP field mapping to update", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TapFieldMappingDTO req) {
        log.info("REST PUT /tap-fields/{}", id);
        return ResponseEntity.ok(service.updateField(id, req));
    }

    @Operation(summary = "Delete a TAP field mapping",
               description = "Soft-deletes a field mapping from the master dictionary. " +
                             "Profile-field links referencing this field are also soft-deleted via cascade.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Field mapping deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Field mapping not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/tap-fields/{id}")
    public ResponseEntity<Void> deleteField(
            @Parameter(description = "Unique ID of the TAP field mapping to delete", example = "1", required = true)
            @PathVariable Long id) {
        log.info("REST DELETE /tap-fields/{}", id);
        service.deleteField(id);
        return ResponseEntity.noContent().build();
    }
}
