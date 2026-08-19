package com.xcess.ocs.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "tax_configs")
@SQLDelete(sql = "UPDATE tax_configs SET is_deleted = true, deleted_at = NOW() WHERE tax_config_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class TaxConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tax_config_id")
    @Schema(description = "Unique tax config ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long taxConfigId;

    @NotBlank(message = "Tax type is required")
    @Size(max = 20, message = "Tax type must not exceed 20 characters")
    @Column(name = "tax_type", nullable = false, length = 20)
    @Schema(description = "Tax type identifier", example = "GST")
    private String taxType;

    @Size(max = 50, message = "Tax name must not exceed 50 characters")
    @Column(name = "tax_name", length = 50)
    @Schema(description = "Display name of the tax", example = "Goods and Services Tax")
    private String taxName;

    @NotNull(message = "Standard rate is required")
    @Column(name = "standard_rate", nullable = false, precision = 5, scale = 2)
    @Schema(description = "Tax rate in percentage", example = "18.00")
    private BigDecimal standardRate;

    @Column(name = "allows_input_credit")
    @Schema(description = "Whether input tax credit can be claimed", example = "true", defaultValue = "true")
    private Boolean allowsInputCredit = true;

    @Column(name = "is_active")
    @Schema(description = "Whether this tax config is active", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @NotNull(message = "Effective from date is required")
    @Column(name = "effective_from", nullable = false)
    @Schema(description = "Date from which this tax rate is effective", example = "2017-07-01")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    @Schema(description = "Date after which this tax rate expires (null = ongoing)", example = "null", nullable = true)
    private LocalDate effectiveTo;

    @Column(name = "apply_on", length = 20)
    @Schema(description = "Tax calculation basis: BASE or CUMULATIVE", example = "BASE", defaultValue = "BASE")
    private String applyOn = "BASE";
}
