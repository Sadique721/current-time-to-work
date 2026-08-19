package com.xcess.ocs.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Reorder Packages Request", description = "Request to reorder rate packages within a group")
public class ReorderPackagesRequestDTO {
    
    @Schema(description = "List of packages with their priority and fallback status")
    @NotNull(message = "Packages list is required")
    private List<PackageOrder> packages;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageOrder {
        @Schema(description = "Rate Package ID", example = "1")
        @NotNull(message = "Rate package ID is required")
        private Long ratePackageId;

        @Schema(description = "Priority (1 = highest priority, 2 = second, etc.)", example = "1")
        @NotNull(message = "Priority is required")
        @Min(value = 1, message = "Priority must be at least 1")
        private Integer priority;
    }
}