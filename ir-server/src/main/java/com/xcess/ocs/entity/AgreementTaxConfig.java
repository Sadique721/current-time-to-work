package com.xcess.ocs.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agreement_tax_configs", uniqueConstraints = {
    @UniqueConstraint(name = "uk_agreement_tax_order", columnNames = {"agreement_id", "apply_order"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgreementTaxConfigEntity", description = "JPA entity mapping for the agreement_tax_configs join table with ordered tax application")
public class AgreementTaxConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Schema(description = "Association ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    @Schema(description = "The associated agreement")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_config_id", nullable = false)
    @Schema(description = "The associated tax configuration")
    private TaxConfig taxConfig;

    @Column(name = "apply_order", nullable = false)
    @Schema(description = "Order of tax application (1 = first, 2 = second, etc.)", example = "1")
    private Integer applyOrder;

    @Column(name = "accumulate_from_orders", length = 50)
    @Schema(description = "Comma-separated apply order numbers to accumulate from for tax-on-tax", example = "1", nullable = true)
    private String accumulateFromOrders;
}
