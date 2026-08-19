package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "ProductPlanSearchDTO", description = "Search criteria for product plans")
public class ProductPlanSearchDTO {
    @Schema(description = "Search term for name or description", example = "Premium Plan")
    private String searchTerm;

    @Schema(description = "Package type (SELLING or BUYING)", example = "SELLING")
    private String packageType;
}