package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_tax_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTaxDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "apply_order", nullable = false)
    private Integer applyOrder;

    @Column(name = "tax_config_id")
    private Long taxConfigId;

    @Column(name = "tax_type", length = 20)
    private String taxType;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "taxable_amount", precision = 19, scale = 6)
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount", precision = 19, scale = 6)
    private BigDecimal taxAmount;

    @Column(name = "apply_on", length = 20)
    private String applyOn;

    @Column(name = "accumulate_from_orders", length = 50)
    private String accumulateFromOrders;
}
