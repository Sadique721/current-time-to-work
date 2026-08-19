package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "12. Product Plan" , description = "Schema to hold product plan details")
public class ProductPlanDTO {
    @Schema(description = "Product plan ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long productPlanId;

    @Schema(description = "Name of the product plan", example = "Premium Plan")
    @NotEmpty(message = "Product plan name is required")
    @Size(min = 6, max = 100, message = "Name must be between 6 and 100 characters")
    private String name;

    @Schema(description = "Description of the product plan", example = "Premium plan for premium customers")
    @NotBlank(message = "Description is required")
    @Size(min = 6, max = 100, message = "Description must be between 6 and 100 characters")
    private String description;

    @Schema(description = "Type of the package", example = "SELLING", allowableValues = {"SELLING", "BUYING"})
    @NotNull(message = "Package type is required")
    private String packageType;

    @Schema(description = "Rate package groups associated with the product plan")
    private List<ProductPlanAssociationDTO> ratePackageGroups;
}