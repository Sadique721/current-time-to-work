package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "invoices")
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    public static final String SETTLEMENT_INCOMING = "INCOMING";
    public static final String SETTLEMENT_OUTGOING = "OUTGOING";
    public static final String SETTLEMENT_NET = "NET";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number", unique = true)
    private String invoiceNumber;

    @ManyToOne
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "billing_cycle_start", nullable = false)
    private LocalDate billingCycleStart;

    @Column(name = "billing_cycle_end", nullable = false)
    private LocalDate billingCycleEnd;

    @Column(name = "settlement_type", nullable = false)
    private String settlementType;

    @Lob
    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "customer_total", precision = 19, scale = 6)
    private BigDecimal customerTotal;

    @Column(name = "vendor_total", precision = 19, scale = 6)
    private BigDecimal vendorTotal;

    @Column(name = "net_amount", precision = 19, scale = 6)
    private BigDecimal netAmount;

    @Column(name = "net_payable_by")
    private String netPayableBy;

    @Column(name = "generated_date")
    private LocalDateTime generatedDate;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column(name = "pdf_checksum")
    private String pdfChecksum;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @Column(name = "pdf_error_reason", columnDefinition = "TEXT")
    private String pdfErrorReason;

    // Tax fields
    @Column(name = "tax_type", length = 20)
    private String taxType;

    @Column(name = "taxable_amount", precision = 19, scale = 6)
    private BigDecimal taxableAmount;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", precision = 19, scale = 6)
    private BigDecimal taxAmount;

    @Column(name = "total_invoice_amount", precision = 19, scale = 6)
    private BigDecimal totalInvoiceAmount;

    @Column(name = "currency", length = 10)
    private String currency;

    // Phase 2 audit fields
    @Column(name = "tax_calculation_date")
    private LocalDate taxCalculationDate;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceTaxDetail> invoiceTaxDetails = new ArrayList<>();
}
