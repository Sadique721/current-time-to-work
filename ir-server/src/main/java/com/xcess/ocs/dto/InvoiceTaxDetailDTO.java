package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTaxDetailDTO {

    private Long id;
    private Integer applyOrder;
    private Long taxConfigId;
    private String taxType;
    private BigDecimal taxRate;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
    private String applyOn;
    private String accumulateFromOrders;
}
