package com.xcess.ocs.controller;

import com.xcess.ocs.constants.CsvConstants;
import com.xcess.ocs.constants.ResponseConstants;
import com.xcess.ocs.dto.*;
import com.xcess.ocs.service.RateDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import com.xcess.ocs.exception.ResourceNotFoundException;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rate-details")
@Tag(name = "6. Rate Details", description = "Endpoints for managing rate details")
public class RateDetailsController {
    private final RateDetailsService rateDetailsService;

    public RateDetailsController(RateDetailsService rateDetailsService) {
        this.rateDetailsService = rateDetailsService;
    }

    @Operation(summary = "Create or replace rate details", description = "Replaces all rate details for the given rate package ID. "
            + "Existing details are archived to history before the new batch is saved. "
            + "Supported type param value: 'replace'. "
            + "Validates against the package type: DESTINATION_BASED, SOURCE_DESTINATION_BASED, or ZONE_DESTINATION_BASED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("{ratePackageId}")
    public ResponseEntity<?> createRateDetail(
            @PathVariable("ratePackageId") Long ratePackageId,
            @RequestParam(name = "type", required = true) String type,
            @RequestBody List<RateDetailDTO> rateDetailDTOs) {
        log.info("Received create request with type '{}'", type);

        if (!CsvConstants.OP_REPLACE.equalsIgnoreCase(type)) {
            return ResponseEntity.badRequest().body("Invalid operation type. Only '" + CsvConstants.OP_REPLACE + "' is supported for now.");
        }
        log.info("Creating new rate details for rate package ID: {}", ratePackageId);

        List<RateDetailDTO> createdDetails = rateDetailsService.createRateDetail(ratePackageId,rateDetailDTOs);

        log.info("Rate details created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDetails);
    }

    @Operation(summary = "Get all rate details", description = "Returns a list of all rate details")
    @ApiResponse(responseCode = "200", description = "HTTP Status OK")
    @GetMapping
    public ResponseEntity<List<RateDetailDTO>> getAllRateDetails() {
        log.info("Fetching all rate details");
        List<RateDetailDTO> rateDetails = rateDetailsService.getAllRateDetails();
        log.info("Retrieved {} rate details", rateDetails.size());
        return ResponseEntity.ok(rateDetails);
    }

    @Operation(summary = "Get a paginated list of rate details", description = "Returns a paginated list of rate details")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<RateDetailDTO>> getRateDetailsInPages(@Valid @RequestBody PageRequestDTO pageRequestDTO) {
        log.info("Fetching paginated rate details");
        int pageNumber = pageRequestDTO.getPage();
        int pageSize = pageRequestDTO.getPageSize();

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        PageResponseDTO<RateDetailDTO> response = rateDetailsService.getRateDetailsInPages(pageable);
        log.info("Successfully retrieved paginated rate details");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get a specific rate detail by ID", description = "Returns the rate detail with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<RateDetailDTO> getRateDetailById(@PathVariable Long id) {
        log.info("Fetching rate detail with ID: {}", id);
        RateDetailDTO rateDetail = rateDetailsService.getRateDetailById(id);
        log.info("Retrieved rate detail successfully");
        return ResponseEntity.ok(rateDetail);
    }

    @Operation(summary = "Update a rate detail", description = "Updates the rate detail with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<RateDetailDTO> updateRateDetail(@PathVariable Long id,
            @RequestBody RateDetailDTO rateDetailDTO) {
        log.info("Updating rate detail with ID: {}", id);
        RateDetailDTO updatedDetail = rateDetailsService.updateRateDetail(id, rateDetailDTO);
        log.info("Updated rate detail successfully");
        return ResponseEntity.ok(updatedDetail);
    }

    @Operation(summary = "Delete a rate detail", description = "Deletes the rate detail with the specified ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "HTTP Status NO CONTENT"),
            @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDTO> deleteRateDetail(@PathVariable Long id) {
        log.warn("Deleting rate detail with ID: {}", id);
        rateDetailsService.deleteRateDetail(id);
        log.warn("Rate detail with ID {} deleted successfully", id);
        return ResponseEntity.ok(ResponseDTO.ok(ResponseConstants.MESSAGE_200_DELETE));
    }

    @Operation(summary = "Upload rate details from a file", description = "Uploads rate details from a CSV or XLSX file for a specific rate package. "
            + "Existing details are archived to history before the new ones are saved. "
            + "Supported type param value: 'replace'. "
            + "File columns must match the package type: DESTINATION_BASED (5 cols), SOURCE_DESTINATION_BASED (7 cols), ZONE_DESTINATION_BASED (4 cols).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
            @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST", content = @Content(schema = @Schema(implementation = RateDetailUploadResponse.class))),
            @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = RateDetailUploadResponse.class)))
    })
    @PostMapping("/upload/{ratePackageId}")
    public ResponseEntity<RateDetailUploadResponse> uploadRateDetails(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "type", required = true) String type,
            @PathVariable Long ratePackageId) {

        log.info("Uploading rate details from file: {} for rate package ID: {} with type: {}",
                file.getOriginalFilename(), ratePackageId, type);
        try {
            if ("replace".equalsIgnoreCase(type)) {
                RateDetailUploadResponse response = rateDetailsService.processFile(file, ratePackageId);
                log.info("File processed successfully: {} records processed", response.getRecordsProcessed());
                return ResponseEntity.ok(response);
            }else {
                return ResponseEntity.badRequest().body(
                        new RateDetailUploadResponse(0, "Unsupported type: " + type));
            }
        } catch (ResourceNotFoundException e) {
            // Handle resource not found (like rate package not found) as a 404 error
            log.warn("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new RateDetailUploadResponse(0, "Resource not found: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Handle validation errors (like date format issues) as a 400 error
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    new RateDetailUploadResponse(0, "Validation error: " + e.getMessage()));
        } catch (IOException e) {
            log.error("File processing error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    new RateDetailUploadResponse(0, "File processing error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Internal server error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    new RateDetailUploadResponse(0, "Internal server error: " + e.getMessage()));
        }
    }

@Operation(summary = "Download CSV template", description = "Downloads a CSV template for the given ratePackageType. "
        + "Allowed values: DESTINATION_BASED, SOURCE_DESTINATION_BASED, ZONE_DESTINATION_BASED.")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid ratePackageType")
})
@GetMapping("/template/csv/{ratePackageType}")
public ResponseEntity<?> downloadCsvTemplate(@PathVariable("ratePackageType") String ratePackageType) throws IOException {
    String filename;
    if (CsvConstants.TYPE_DESTINATION_BASED.equalsIgnoreCase(ratePackageType)) {
        filename = CsvConstants.FILE_DESTINATION_BASED_TEMPLATE;
    } else if (CsvConstants.TYPE_SOURCE_DESTINATION_BASED.equalsIgnoreCase(ratePackageType)) {
        filename = CsvConstants.FILE_SOURCE_DESTINATION_BASED_TEMPLATE;
    } else if (CsvConstants.TYPE_ZONE_DESTINATION_BASED.equalsIgnoreCase(ratePackageType)) {
        filename = CsvConstants.FILE_ZONE_DESTINATION_BASED_TEMPLATE;
    } else {
        return ResponseEntity.badRequest().body("Invalid ratePackageType. Allowed: "
                + CsvConstants.TYPE_DESTINATION_BASED + ", "
                + CsvConstants.TYPE_SOURCE_DESTINATION_BASED + ", or "
                + CsvConstants.TYPE_ZONE_DESTINATION_BASED + ".");
    }
    ClassPathResource resource = new ClassPathResource("templates/" + filename);
    byte[] fileBytes = StreamUtils.copyToByteArray(resource.getInputStream());
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=" + filename)
            .header("Content-Type", "text/csv")
            .body(fileBytes);
}

    private static final java.util.Map<String, String[]> EXCEL_HEADERS = java.util.Map.of(
            CsvConstants.TYPE_DESTINATION_BASED, new String[]{CsvConstants.HEADER_DESTINATION_PREFIX, CsvConstants.HEADER_DESTINATION_PREFIX_NAME, CsvConstants.HEADER_RATE, CsvConstants.HEADER_START_TIME, CsvConstants.HEADER_END_TIME},
            CsvConstants.TYPE_SOURCE_DESTINATION_BASED, new String[]{CsvConstants.HEADER_DESTINATION_PREFIX, CsvConstants.HEADER_DESTINATION_PREFIX_NAME, CsvConstants.HEADER_RATE, CsvConstants.HEADER_START_TIME, CsvConstants.HEADER_END_TIME, CsvConstants.HEADER_SOURCE_PREFIX, CsvConstants.HEADER_SOURCE_PREFIX_NAME},
            CsvConstants.TYPE_ZONE_DESTINATION_BASED, new String[]{CsvConstants.HEADER_ZONE_NAME, CsvConstants.HEADER_RATE, CsvConstants.HEADER_START_TIME, CsvConstants.HEADER_END_TIME}
    );

    private static final java.util.Map<String, Object[][]> EXCEL_SAMPLE_DATA = java.util.Map.of(
            "DESTINATION_BASED", new Object[][]{
                    {"91403", "India-Hyderabadi", 0.025, "01-01-2024 00:00", "31-12-2024 23:59"},
                    {"44207", "UK-London", 0.018, "01-01-2024 00:00", "31-12-2024 23:59"},
                    {"12345", "US-NewYork", 0.030, "01-01-2024 00:00", "31-12-2024 23:59"}
            },
            "SOURCE_DESTINATION_BASED", new Object[][]{
                    {"91403", "India-Hyderabadi", 0.025, "01-01-2024 00:00", "31-12-2024 23:59", "19325", "US-telecom"},
                    {"44207", "UK-London", 0.018, "01-01-2024 00:00", "31-12-2024 23:59", "61400", "Australia-Optus"},
                    {"49301", "Germany-Berlin", 0.022, "01-01-2024 00:00", "31-12-2024 23:59", "81301", "Japan-NTT"}
            },
            "ZONE_DESTINATION_BASED", new Object[][]{
                    {"South Asia", 0.025, "01-01-2024 00:00", "31-12-2024 23:59"},
                    {"North America", 0.030, "01-01-2024 00:00", "31-12-2024 23:59"},
                    {"Europe", 0.020, "01-01-2024 00:00", "31-12-2024 23:59"}
            }
    );

    @GetMapping("/template/excel/{ratePackageType}")
    public ResponseEntity<?> downloadExcelTemplate(@PathVariable("ratePackageType") String ratePackageType) throws IOException {
        String key = ratePackageType.toUpperCase();
        if (!EXCEL_HEADERS.containsKey(key)) {
            return ResponseEntity.badRequest().body("Invalid ratePackageType. Allowed: DESTINATION_BASED, SOURCE_DESTINATION_BASED, or ZONE_DESTINATION_BASED.");
        }

        String filename = key.toLowerCase() + "_template.xlsx";
        String[] headers = EXCEL_HEADERS.get(key);
        Object[][] rows = EXCEL_SAMPLE_DATA.get(key);

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Rate Details");
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < rows.length; r++) {
                org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(r + 1);
                for (int i = 0; i < rows[r].length; i++) {
                    org.apache.poi.ss.usermodel.Cell cell = dataRow.createCell(i);
                    if (rows[r][i] instanceof Double) cell.setCellValue((Double) rows[r][i]);
                    else cell.setCellValue(rows[r][i].toString());
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(out.toByteArray());
        }
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewRateDetailsFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "ratePackageType", required = true) String ratePackageType) {
        try {
            List<RateDetailDTO> rateDetails = rateDetailsService.parseRateDetailsFile(file, ratePackageType);
            return ResponseEntity.ok(rateDetails);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("File processing error: " + e.getMessage());
        }
    }

}
