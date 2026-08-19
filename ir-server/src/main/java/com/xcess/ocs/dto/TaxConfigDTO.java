package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TaxConfig", description = "Tax configuration details for country-specific tax rules")
public class TaxConfigDTO {

    @Schema(description = "Unique tax config ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long taxConfigId;

    @NotBlank(message = "Tax type is required")
    @Size(max = 20, message = "Tax type must not exceed 20 characters")
    @Schema(description = "Tax type identifier", example = "GST", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taxType;

    @Size(max = 50, message = "Tax name must not exceed 50 characters")
    @Schema(description = "Display name of the tax", example = "Goods and Services Tax")
    private String taxName;

    @NotNull(message = "Standard rate is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Standard rate must be greater than 0")
    @DecimalMax(value = "999.99", message = "Standard rate must not exceed 999.99")
    @Schema(description = "Tax rate in percentage", example = "18.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal standardRate;

    @Schema(description = "Whether input tax credit can be claimed. When true, businesses can offset this tax against their output tax liability.", example = "true", defaultValue = "true")
    private Boolean allowsInputCredit;

    @Schema(description = "Whether this tax config is active", example = "true", defaultValue = "true")
    private Boolean isActive;

    @NotNull(message = "Effective from date is required")
    @Schema(description = "Date from which this tax rate is effective", example = "2017-07-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveFrom;

    @Schema(description = "Date after which this tax rate expires (null = ongoing)", example = "null", nullable = true)
    private LocalDate effectiveTo;

    @Size(max = 20, message = "Apply on must not exceed 20 characters")
    @Schema(description = "Tax calculation basis: BASE (on base amount) or CUMULATIVE (tax-on-tax, includes prior taxes)", example = "BASE", defaultValue = "BASE", allowableValues = {"BASE", "CUMULATIVE"})
    private String applyOn;
}
