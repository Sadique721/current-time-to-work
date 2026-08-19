package com.xcess.ocs.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long invoiceId;
    private String invoiceNumber;
    private Long agreementId;
    private String agreementCode;
    private LocalDate billingCycleStart;
    private LocalDate billingCycleEnd;
    private String settlementType;
    private String status;
    private BigDecimal customerTotal;
    private BigDecimal vendorTotal;
    private BigDecimal netAmount;
    private String netPayableBy;
    private LocalDateTime generatedDate;
    private String pdfFilePath;
    private String pdfChecksum;
    private LocalDateTime pdfGeneratedAt;
    private String pdfErrorReason;

    // Tax fields
    private String taxType;
    private BigDecimal taxableAmount;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalInvoiceAmount;
    private String currency;

    // Phase 2 audit fields
    private LocalDate taxCalculationDate;
    private List<InvoiceTaxDetailDTO> invoiceTaxDetails;
}
