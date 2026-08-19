package com.xcess.ocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import org.springframework.web.multipart.MultipartFile;

/**
 * Data Transfer Object for Template Configuration entity.
 * Used for API requests and responses.
 * 
 * @author OCS
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfigurationDTO {
    
    /** Unique identifier of the template (generated automatically) */
    private Long templateId;
    
    /** Name of the template - used as unique identifier */
    @NotBlank(message = "Template name is required")
    @Size(max = 100, message = "Template name must not exceed 100 characters")
    private String templateName;
    
    /** Description of the template (optional) */
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String templateDescription;
    
    /** Path where the template file is stored */
    private String templatePath;
    
    /** Indicates whether the template is active */
    @NotBlank(message = "isActive is required")
    private Boolean isActive;
    
    /** Line of business this template applies to */
    private com.xcess.ocs.entity.LineOfBusiness lineOfBusiness;
    
    /** The XSL template file to be uploaded (only for request) */
    private MultipartFile templateFile;
}