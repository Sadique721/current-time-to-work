package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgreementTaxConfig", description = "Association between an agreement and a tax config with ordered application")
public class AgreementTaxConfigDTO {

    @Schema(description = "Association ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @NotNull(message = "Tax config ID is required")
    @Schema(description = "ID of the tax config to apply", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taxConfigId;

    @Schema(description = "Name of the tax config", example = "Goods and Services Tax", accessMode = Schema.AccessMode.READ_ONLY)
    private String taxConfigName;

    @Schema(description = "Tax type", example = "GST", accessMode = Schema.AccessMode.READ_ONLY)
    private String taxType;

    @NotNull(message = "Apply order is required")
    @Min(value = 1, message = "Apply order must be at least 1")
    @Schema(description = "Order of tax application (1 = first, 2 = second, etc.)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer applyOrder;

    @Schema(description = "Comma-separated apply order numbers to accumulate from for tax-on-tax", example = "1", nullable = true)
    private String accumulateFromOrders;
}
