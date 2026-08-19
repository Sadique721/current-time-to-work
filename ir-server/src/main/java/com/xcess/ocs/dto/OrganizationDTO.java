package com.xcess.ocs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Data Transfer Object for Organization entity.
 * Used for API requests and responses.
 * 
 * @author OCS
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    
    /** Unique identifier of the organization (generated automatically) */
    private Long organizationId;
    
    /** Name of the organization - used as unique identifier */
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;
    
    /** Optional suffix name for the organization (e.g., "Inc", "LLC") */
    @Size(max = 100, message = "Suffix name must not exceed 100 characters")
    private String suffixName;
    
    /** Legal/registered name of the organization */
    @NotBlank(message = "Legal name is required")
    @Size(max = 255, message = "Legal name must not exceed 255 characters")
    private String legalName;
    
    /** Physical address of the organization */
    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
}
