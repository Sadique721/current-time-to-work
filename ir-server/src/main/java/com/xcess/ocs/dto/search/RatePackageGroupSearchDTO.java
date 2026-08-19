package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "RatePackageGroupSearchDTO", description = "Search criteria for rate package groups")
public class RatePackageGroupSearchDTO {
    @Schema(description = "Search term for name or description", example = "Basic Group")
    private String searchTerm;

    @Schema(description = "Package type (SELLING or BUYING)", example = "SELLING")
    private String packageType;


}