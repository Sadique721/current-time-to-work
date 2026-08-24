package com.xcess.ocs.service;

import com.xcess.ocs.constants.enums.NetPayableBy;
import com.xcess.ocs.dto.TemplateConfigurationDTO;
import com.xcess.ocs.dto.search.TemplateConfigurationSearchDTO;
import com.xcess.ocs.dto.xml.*;
import com.xcess.ocs.entity.TemplateConfiguration;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.AgreementRepository;
import com.xcess.ocs.repository.TemplateConfigurationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing Template Configuration operations.
 * Handles CRUD operations for XSL template files including file storage,
 * validation, and database management.
 */
@Slf4j
@Service
public class TemplateConfigurationService {

    /**
     * Base directory for storing template files on the filesystem.
     * Configured via application.properties.
     */
    @Value("${template.storage.path:src/main/resources/xslt/templates}")
    private String templateStoragePath;

    /**
     * Repository for TemplateConfiguration entity.
     * Provides database operations for template records.
     */
    @Autowired
    private TemplateConfigurationRepository templateConfigurationRepository;

    /**
     * Repository for Agreement entity.
     * Used to check if template is in use by active agreements.
     */
    @Autowired
    private AgreementRepository agreementRepository;
    
    /**
     * Service for PDF generation operations.
     * Used to generate PDF previews from template configurations.
     */
    @Autowired
    private PdfGenerationService pdfGenerationService;

    /**
     * Creates a new template configuration.
     * Validates the template file, checks for duplicates, saves the file to disk,
     * and creates a database record.
     *
     * @param dto Contains template name, description, file, and isActive status
     * @return TemplateConfigurationDTO with created template details
     * @throws RuntimeException if validation fails or duplicate exists
     */
    @Transactional
    public TemplateConfigurationDTO createTemplate(TemplateConfigurationDTO dto) {
        log.info("Creating new template: {}", dto.getTemplateName());

        // Validate input fields
        validateCreateRequest(dto);

        // Check for duplicate template name (non-deleted records only)
        if (templateConfigurationRepository.existsByTemplateNameAndIsDeletedFalse(dto.getTemplateName())) {
            throw new RuntimeException("Template name already exists");
        }

        // Validate the XSL template file
        validateTemplateFile(dto.getTemplateFile());

        // Calculate content hash and check for duplicate content
        String contentHash = calculateContentHash(dto.getTemplateFile());
        if (templateConfigurationRepository.existsByTemplateContentHashAndIsDeletedFalse(contentHash)) {
            throw new RuntimeException("Template with same content already exists");
        }

        // Save the template file to disk
        String filePath = saveTemplateFile(dto.getTemplateFile());

        // Create and save the template configuration entity
        TemplateConfiguration template = new TemplateConfiguration();
        template.setTemplateName(dto.getTemplateName());
        template.setTemplateDescription(dto.getTemplateDescription());
        template.setTemplatePath(filePath);
        template.setTemplateContentHash(contentHash);
        template.setIsActive(dto.getIsActive());
        template.setLineOfBusiness(dto.getLineOfBusiness());

        TemplateConfiguration savedTemplate = templateConfigurationRepository.save(template);
        log.info("Template created successfully with ID: {}", savedTemplate.getTemplateId());

        return toDTO(savedTemplate);
    }

    /**
     * Updates an existing template configuration.
     * Validates the template ID, checks for duplicate name (if changed),
     * validates file (if provided), and updates the record.
     *
     * @param templateId  The ID of the template to update
     * @param dto  Contains updated template name, description, file, and isActive
     * @return TemplateConfigurationDTO with updated template details
     * @throws RuntimeException if validation fails or template not found
     */
    @Transactional
    public TemplateConfigurationDTO updateTemplate(Long templateId, TemplateConfigurationDTO dto) {
        log.info("Updating template with ID: {}", templateId);

        // Find the existing template
        TemplateConfiguration existingTemplate = templateConfigurationRepository
                .findByTemplateIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        // Check if template name is changing
        if (dto.getTemplateName() != null && 
            !dto.getTemplateName().equals(existingTemplate.getTemplateName())) {
            // Check for duplicate name in non-deleted records
            if (templateConfigurationRepository.existsByTemplateNameAndIsDeletedFalse(dto.getTemplateName())) {
                throw new RuntimeException("Template name already exists");
            }
        }

        // Validate file if provided
        if (dto.getTemplateFile() != null && !dto.getTemplateFile().isEmpty()) {
            validateTemplateFile(dto.getTemplateFile());

            // Calculate new content hash and check for duplicate content (if content changed)
            String newContentHash = calculateContentHash(dto.getTemplateFile());
            if (!newContentHash.equals(existingTemplate.getTemplateContentHash())) {
                if (templateConfigurationRepository.existsByTemplateContentHashAndIsDeletedFalse(newContentHash)) {
                    throw new RuntimeException("Template with same content already exists");
                }
            }
        }

        // Update all fields from request
        if (dto.getTemplateName() != null) {
            existingTemplate.setTemplateName(dto.getTemplateName());
        }
        if (dto.getTemplateDescription() != null) {
            existingTemplate.setTemplateDescription(dto.getTemplateDescription());
        }
        if (dto.getIsActive() != null) {
            existingTemplate.setIsActive(dto.getIsActive());
        }
        if (dto.getLineOfBusiness() != null) {
            existingTemplate.setLineOfBusiness(dto.getLineOfBusiness());
        }

        // Handle file update if provided
        if (dto.getTemplateFile() != null && !dto.getTemplateFile().isEmpty()) {
            // Delete old file
            deleteTemplateFile(existingTemplate.getTemplatePath());

            // Save new file
            String newFilePath = saveTemplateFile(dto.getTemplateFile());
            existingTemplate.setTemplatePath(newFilePath);

            // Update content hash
            String newContentHash = calculateContentHash(dto.getTemplateFile());
            existingTemplate.setTemplateContentHash(newContentHash);
        }

        TemplateConfiguration updatedTemplate = templateConfigurationRepository.save(existingTemplate);
        log.info("Template updated successfully with ID: {}", updatedTemplate.getTemplateId());

        return toDTO(updatedTemplate);
    }

    /**
     * Deletes a template configuration (soft delete).
     * Checks if template is in use by active agreements before deletion.
     *
     * @param templateId The ID of the template to delete
     * @throws RuntimeException if template not found or in use by active agreements
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        log.info("Deleting template with ID: {}", templateId);

        // Find the template
        TemplateConfiguration template = templateConfigurationRepository
                .findByTemplateIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        // Check if template is in use by active agreements (check all three template types)
        long incomingUsage = agreementRepository.countByIncomingSettlementTemplate_TemplateIdAndIsDeletedFalse(templateId);
        long outgoingUsage = agreementRepository.countByOutgoingSettlementTemplate_TemplateIdAndIsDeletedFalse(templateId);
        long netUsage = agreementRepository.countByNetSettlementTemplate_TemplateIdAndIsDeletedFalse(templateId);
        long usageCount = incomingUsage + outgoingUsage + netUsage;
        if (usageCount > 0) {
            throw new RuntimeException("Cannot delete: Template is in use by active agreements");
        }

        // Perform soft delete (handled by @SQLDelete annotation on entity)
        templateConfigurationRepository.delete(template);
        
        // Optionally delete the file from disk
        deleteTemplateFile(template.getTemplatePath());

        log.info("Template deleted successfully with ID: {}", templateId);
    }

    /**
     * Retrieves a template configuration by ID.
     *
     * @param templateId The ID of the template to retrieve
     * @return TemplateConfigurationDTO with template details
     * @throws ResourceNotFoundException if template not found
     */
    public TemplateConfigurationDTO getTemplateById(Long templateId) {
        log.info("Retrieving template with ID: {}", templateId);
        
        TemplateConfiguration template = templateConfigurationRepository
                .findByTemplateIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        return toDTO(template);
    }

    /**
     * Searches templates with pagination and optional search term.
     * If search term is null or empty, returns all non-deleted templates.
     *
     * @param searchDTO Search criteria containing searchTerm (can be null)
     * @param pageable  Pagination information
     * @return PageResponseDTO containing paginated template results
     */
    public Page<TemplateConfigurationDTO> searchTemplates(TemplateConfigurationSearchDTO searchDTO, Pageable pageable) {
        log.info("Searching templates with criteria: {}", searchDTO);
        
        String searchTerm = searchDTO != null ? searchDTO.getSearchTerm() : null;

        Page<TemplateConfiguration> templatePage = templateConfigurationRepository
                .searchTemplates(searchTerm, pageable);

        return templatePage.map(this::toDTO);
    }

    public List<Map<String, Object>> getAllTemplateIds(com.xcess.ocs.entity.LineOfBusiness lineOfBusiness) {
        log.info("Retrieving all active template IDs and names");
        
        return templateConfigurationRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .filter(t -> lineOfBusiness == null || lineOfBusiness.equals(t.getLineOfBusiness()))
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("templateId", t.getTemplateId());
                    map.put("templateName", t.getTemplateName());
                    return map;
                })
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Validates create request fields.
     * Throws RuntimeException if required fields are missing or invalid.
     *
     * @param dto The DTO to validate
     */
    private void validateCreateRequest(TemplateConfigurationDTO dto) {
        if (dto.getTemplateName() == null || dto.getTemplateName().trim().isEmpty()) {
            throw new RuntimeException("Template name is required");
        }
        if (dto.getTemplateFile() == null || dto.getTemplateFile().isEmpty()) {
            throw new RuntimeException("Template file is required");
        }
        if (dto.getIsActive() == null) {
            throw new RuntimeException("isActive field is required");
        }

        if(dto.getLineOfBusiness()==null)
        {
            throw new RuntimeException("Line of Business field is required");
        }
    }

    /**
     * Validates the template file.
     * Checks for valid extension (.xsl or .xslt) and valid XML/XSLT content.
     *
     * @param file The multipart file to validate
     * @throws RuntimeException if file is invalid
     */
    private void validateTemplateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Template file is required");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || 
            (!fileName.toLowerCase().endsWith(".xsl") && !fileName.toLowerCase().endsWith(".xslt"))) {
            throw new RuntimeException("Only .xsl or .xslt files are allowed");
        }

        try {
            // Try to parse as XML to verify it's valid XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Verify it's well-formed XML and contains XSLT elements
            try (InputStream inputStream = file.getInputStream()) {
                org.w3c.dom.Document doc = builder.parse(inputStream);
                
                // Check for XSLT root element
                org.w3c.dom.Element root = doc.getDocumentElement();
                if (!root.getNodeName().equals("xsl:stylesheet") && 
                    !root.getNodeName().equals("xsl:transform")) {
                    throw new RuntimeException("Not a valid XSLT template - missing xsl:stylesheet root element");
                }
            }
        } catch (Exception e) {
            log.error("Template file validation failed: {}", e.getMessage());
            if (e.getMessage().contains("Not a valid XSLT")) {
                throw new RuntimeException(e.getMessage());
            }
            throw new RuntimeException("Invalid XML format in template file");
        }
    }

    /**
     * Calculates SHA-256 hash of the template file content.
     * Used for duplicate content detection.
     *
     * @param file The file to calculate hash for
     * @return Base64 encoded SHA-256 hash string
     */
    private String calculateContentHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate content hash", e);
        }
    }

    /**
     * Saves the template file to the filesystem.
     * Creates the directory if it doesn't exist.
     *
     * @param file       The file to save
     * @return The path where the file was saved
     */
    private String saveTemplateFile(MultipartFile file) {
        try {
            // Create directory if it doesn't exist
            Path directoryPath = Paths.get(templateStoragePath);
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

            // Generate filename:  {timestamp}.xsl for new templates

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = "new_" + timestamp + ".xsl";

            Path filePath = directoryPath.resolve(fileName);
            Files.write(filePath, file.getBytes());

            log.info("Template file saved to: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save template file", e);
        }
    }

    /**
     * Deletes the template file from the filesystem.
     *
     * @param filePath The path of the file to delete
     */
    private void deleteTemplateFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    log.info("Template file deleted: {}", filePath);
                }
            } catch (IOException e) {
                log.warn("Failed to delete template file: {}", filePath, e);
            }
        }
    }

    /**
     * Converts TemplateConfiguration entity to TemplateConfigurationDTO.
     *
     * @param template The entity to convert
     * @return The corresponding DTO
     */
    private TemplateConfigurationDTO toDTO(TemplateConfiguration template) {
        TemplateConfigurationDTO dto = new TemplateConfigurationDTO();
        dto.setTemplateId(template.getTemplateId());
        dto.setTemplateName(template.getTemplateName());
        dto.setTemplateDescription(template.getTemplateDescription());
        dto.setTemplatePath(template.getTemplatePath());
        dto.setIsActive(template.getIsActive());
        dto.setLineOfBusiness(template.getLineOfBusiness());
        
        return dto;
    }

    /**
     * Generates a sample XML content for PDF preview purposes.
     * Creates a sample invoice XML structure that matches the InvoiceXmlDTO structure
     * which is used by the XSLT template for PDF generation.
     * Uses JAXB to properly serialize the XML to match the template expectations.
     *
     * @return Sample XML content string suitable for PDF generation
     */
    public String generateSampleXmlContent() {
        log.info("Generating sample XML content for template preview");

        InvoiceXmlDTO dto = new InvoiceXmlDTO();
        dto.setInvoiceId("1");
        dto.setGeneratedDate(java.time.LocalDate.now().toString());
        dto.setCurrency("Rs.");

        CompanyInfo companyInfo = new CompanyInfo();
        companyInfo.setName("Interconnect");
        companyInfo.setNameSuffix("Billing");
        companyInfo.setLegalName("Carrier Services International");
        companyInfo.setAddress("123 Global Exchange Plaza, Floor 42, Telecom Tower, London, UK EC1A 1BB");
        dto.setCompanyInfo(companyInfo);

        com.xcess.ocs.dto.xml.Agreement agreement = new com.xcess.ocs.dto.xml.Agreement();
        agreement.setAgreementCode("SAMPLE-AGR-001");
        agreement.setBillingCycleStart("2026-01-01");
        agreement.setBillingCycleEnd("2026-01-31");
        agreement.setDescription("Master Interconnect Settlement Agreement");
        dto.setAgreement(agreement);

        BillTo billTo = new BillTo();
        billTo.setCompanyName("Sample Partner Ltd");
        billTo.setAccountCode("ACC001");
        dto.setBillTo(billTo);

        Account account1 = new Account();
        account1.setServiceType("Voice");
        account1.setAccountCode("ACC001");
        account1.setAccountType("CUSTOMER");
        account1.setTotalAmount(10000.0);
        dto.getAccounts().add(account1);

        Account account2 = new Account();
        account2.setServiceType("Voice");
        account2.setAccountCode("ACC002");
        account2.setAccountType("VENDOR");
        account2.setTotalAmount(5000.0);
        dto.getAccounts().add(account2);

        SummaryAccount summary1 = new SummaryAccount();
        summary1.setAccountCode("ACC001");
        summary1.setAccountType("CUSTOMER");
        summary1.setTrafficDirection("Inbound Traffic");
        summary1.setTotal(10000.0);
        dto.getSummary().add(summary1);

        SummaryAccount summary2 = new SummaryAccount();
        summary2.setAccountCode("ACC002");
        summary2.setAccountType("VENDOR");
        summary2.setTrafficDirection("Outbound Traffic");
        summary2.setTotal(5000.0);
        dto.getSummary().add(summary2);

        NetSettlement netSettlement = new NetSettlement();
        netSettlement.setCustomerTotal(10000.0);
        netSettlement.setVendorTotal(5000.0);
        netSettlement.setNetAmount(5000.0);
        netSettlement.setNetPayableBy(NetPayableBy.CUSTOMER.label());
        dto.setNetSettlement(netSettlement);

        return XmlConverter.convertToXml(dto);
    }

    /**
     * Generates a PDF preview for the specified template configuration.
     * Fetches the template, generates sample XML, transforms it to PDF,
     * and returns the PDF as byte array without saving to filesystem.
     *
     * @param templateId        The ID of the template configuration
     * @return                 byte array containing PDF content
     * @throws ResourceNotFoundException if template not found
     */
    public byte[] generatePdfPreview(Long templateId) {
        log.info("Generating PDF preview for template ID: {}", templateId);
        
        TemplateConfiguration template = templateConfigurationRepository
                .findByTemplateIdAndIsDeletedFalse(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        
        if (template.getTemplatePath() == null) {
            throw new ResourceNotFoundException("Template path not found");
        }
        
        String xmlContent = generateSampleXmlContent();
        
        byte[] pdfBytes = pdfGenerationService.generatePdfAsBytes(xmlContent, template.getTemplatePath());
        
        if (pdfBytes == null) {
            throw new RuntimeException("PDF generation failed");
        }
        
        log.info("PDF preview generated successfully for template ID: {}", templateId);
        return pdfBytes;
    }
}