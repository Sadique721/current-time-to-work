package com.xcess.ocs.controller;

import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.TemplateConfigurationDTO;
import com.xcess.ocs.dto.search.TemplateConfigurationSearchDTO;
import com.xcess.ocs.service.PdfGenerationService;
import com.xcess.ocs.service.TemplateConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Template Configuration operations.
 * Provides HTTP endpoints for CRUD operations on XSL template configurations.
 * 
 * <p>Endpoints:</p>
 * <ul>
 *   <li>POST /api/templates - Create new template</li>
 *   <li>PUT /api/templates/{id} - Update template</li>
 *   <li>DELETE /api/templates/{id} - Delete template (soft delete)</li>
 *   <li>GET /api/templates/{id} - Get template by ID</li>
 *   <li>POST /api/templates/paginated - Search templates with pagination</li>
 * </ul>
 * 
 * @see TemplateConfigurationService
 * @see TemplateConfigurationDTO
 */
@Slf4j
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@Tag(name = "Template Configuration", description = "XSL template configuration management APIs")
public class TemplateConfigurationController {

    private final TemplateConfigurationService templateConfigurationService;
    private final PdfGenerationService pdfGenerationService;

    /**
     * Creates a new template configuration.
     * Accepts multipart form data with template file, name, description, and active status.
     * Validates the file as valid XSLT and checks for duplicate name/content.
     *
     * @param templateName        Name of the template (form field)
     * @param templateDescription Description of the template (form field, optional)
     * @param templateFile        XSL template file (form field, required)
     * @param isActive            Active status (form field, required)
     * @return Created TemplateConfigurationDTO
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create new template", 
               description = "Creates a new XSL template configuration with file upload validation")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Template created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed or duplicate template",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<TemplateConfigurationDTO> createTemplate(
            @RequestParam("templateName") String templateName,
            @RequestParam(value = "templateDescription", required = false) String templateDescription,
            @RequestParam("templateFile") MultipartFile templateFile,
            @RequestParam("isActive") Boolean isActive,
            @RequestParam(value = "lineOfBusiness", required = false) com.xcess.ocs.entity.LineOfBusiness lineOfBusiness) {
        
        log.info("Creating new template: {}", templateName);
        
        TemplateConfigurationDTO dto = new TemplateConfigurationDTO();
        dto.setTemplateName(templateName);
        dto.setTemplateDescription(templateDescription);
        dto.setTemplateFile(templateFile);
        dto.setIsActive(isActive);
        dto.setLineOfBusiness(lineOfBusiness);

        TemplateConfigurationDTO created = templateConfigurationService.createTemplate(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing template configuration.
     * All fields are required. If template name changes, validates for uniqueness.
     * If file is provided, validates and replaces existing file.
     *
     * @param id                  Template ID to update (path variable)
     * @param templateName        Updated template name (form field)
     * @param templateDescription Updated template description (form field)
     * @param templateFile        Updated template file (form field, optional - if not provided, keeps existing)
     * @param isActive            Updated active status (form field)
     * @return Updated TemplateConfigurationDTO
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update template", 
               description = "Updates an existing XSL template configuration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed or duplicate template",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template not found",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<TemplateConfigurationDTO> updateTemplate(
            @PathVariable Long id,
            @RequestParam("templateName") String templateName,
            @RequestParam(value = "templateDescription", required = false) String templateDescription,
            @RequestParam(value = "templateFile", required = false) MultipartFile templateFile,
            @RequestParam("isActive") Boolean isActive,
            @RequestParam(value = "lineOfBusiness", required = false) com.xcess.ocs.entity.LineOfBusiness lineOfBusiness) {
        
        log.info("Updating template with ID: {}", id);
        
        TemplateConfigurationDTO dto = new TemplateConfigurationDTO();
        dto.setTemplateName(templateName);
        dto.setTemplateDescription(templateDescription);
        dto.setTemplateFile(templateFile);
        dto.setIsActive(isActive);
        dto.setLineOfBusiness(lineOfBusiness);

        TemplateConfigurationDTO updated = templateConfigurationService.updateTemplate(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a template configuration (soft delete).
     * Checks if template is in use by active agreements before deletion.
     *
     * @param id Template ID to delete (path variable)
     * @return Success message
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", 
               description = "Soft deletes the template configuration with the specified ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Template is in use by active agreements",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Template not found",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        log.info("Deleting template with ID: {}", id);
        templateConfigurationService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a template configuration by ID.
     *
     * @param id Template ID to retrieve (path variable)
     * @return TemplateConfigurationDTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", 
               description = "Returns the template configuration with the specified ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template found"),
        @ApiResponse(responseCode = "404", description = "Template not found",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<TemplateConfigurationDTO> getTemplateById(@PathVariable Long id) {
        log.info("Retrieving template with ID: {}", id);
        TemplateConfigurationDTO template = templateConfigurationService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    /**
     * Searches templates with pagination and optional filters.
     * Filters can be applied by template name, isActive status, and description.
     * If searchCriteria is null or empty, returns all non-deleted templates.
     *
     * @param pageRequestDTO Pagination request containing page number, size, and search criteria
     * @return Paginated list of TemplateConfigurationDTO
     */
    @PostMapping("/paginated")
    @Operation(summary = "Search templates with pagination", 
               description = "Returns paginated list of templates with optional search criteria")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Templates retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination request",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<PageResponseDTO<TemplateConfigurationDTO>> searchTemplates(
            @Valid @RequestBody PageRequestDTO<TemplateConfigurationSearchDTO> pageRequestDTO) {
        
        log.info("Searching templates with pagination");
        
        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1, 
                pageRequestDTO.getPageSize()
        );

        Page<TemplateConfigurationDTO> templatePage = templateConfigurationService
                .searchTemplates(pageRequestDTO.getSearchCriteria(), pageable);

        PageResponseDTO<TemplateConfigurationDTO> response = new PageResponseDTO<>();
        response.setPageDetails(new com.xcess.ocs.dto.PaginationDetailsDTO(
                templatePage.getTotalPages(),
                templatePage.getTotalElements(),
                templatePage.getSize(),
                templatePage.getNumber() + 1
        ));
        response.setContent(templatePage.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * Generates and returns a PDF preview using the specified template configuration.
     * 
     * <p>This endpoint accepts a template ID. It fetches the template configuration,
     * generates sample XML content, and uses the PDF generation service to transform
     * the XML content into a PDF using the template's XSLT file.</p>
     * 
     * <p>The generated PDF is returned as a file download with content-type application/pdf.
     * The PDF is generated in-memory and is not saved to the filesystem.</p>
     * 
     * @param templateId    The ID of the template configuration to use for PDF generation
     * @return              PDF file as response with content-disposition header for download
     * @see                 TemplateConfigurationDTO
     * @see                 PdfGenerationService
     */
    @GetMapping("/{templateId}/preview-pdf")
    @Operation(summary = "Generate PDF preview from template", 
               description = "Generates a PDF preview using the specified template configuration and sample XML content")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF preview generated successfully"),
        @ApiResponse(responseCode = "404", description = "Template configuration not found",
                content = @Content(schema = @Schema(implementation = com.xcess.ocs.dto.ErrorResponseDTO.class)))
    })
    public ResponseEntity<byte[]> generatePdfPreview(@PathVariable Long templateId) {
        
        log.info("Generating PDF preview for template ID: {}", templateId);
        
        byte[] pdfBytes = templateConfigurationService.generatePdfPreview(templateId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "template-" + templateId + ".pdf");
        
        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    @GetMapping("/templateIds")
    @Operation(summary = "Get all template IDs and names", 
               description = "Returns a list of all template IDs and names for dropdown selections")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template IDs retrieved successfully")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllTemplateIds(
            @RequestParam(value = "lineOfBusiness", required = false) com.xcess.ocs.entity.LineOfBusiness lineOfBusiness) {
        log.info("Retrieving all template IDs and names");
        List<Map<String, Object>> templateIds = templateConfigurationService.getAllTemplateIds(lineOfBusiness);
        return ResponseEntity.ok(templateIds);
    }

}