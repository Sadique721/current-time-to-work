package com.xcess.ocs.roaming.service;

import com.xcess.ocs.dto.xml.*;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "RoamingTapOutInvoice")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class RoamingTapOutInvoiceXmlDTO {

    private String invoiceId;
    private String generatedDate;
    private String currency;
    private String taxCalculationDate;
    private Double totalInvoiceAmount;

    private CompanyInfo companyInfo;
    private com.xcess.ocs.dto.xml.Agreement agreement;
    private BillTo billTo;
    private NetSettlement netSettlement;

    @XmlElementWrapper(name = "TapFiles")
    @XmlElement(name = "TapFile")
    private List<TapFileSection> tapFiles = new ArrayList<>();

    @XmlElementWrapper(name = "TaxLineItems")
    @XmlElement(name = "TaxLineItem")
    private List<InvoiceXmlDTO.TaxLineItem> taxLineItems;

    // ── Per TAP file section ──────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class TapFileSection {
        private String fileName;
        private Integer sequenceNo;
        private String senderTadig;
        private String recipientTadig;
        private String generatedAt;
        private Long totalCdrs;
        private BigDecimal tapCharge;       // raw TAP charge from TapFileRecord
        private BigDecimal totalOurCharge;  // sum of outgoingTotalCost for this file

        @XmlElementWrapper(name = "CdrLines")
        @XmlElement(name = "CdrLine")
        private List<CdrLine> cdrLines = new ArrayList<>();
    }

    // ── Per CDR line ──────────────────────────────────────────────────────────

    @XmlAccessorType(XmlAccessType.FIELD)
    @Data
    public static class CdrLine {
        private String serviceType;         // VOICE / SMS / USAGE
        private String callingNumber;
        private String calledNumber;
        private String startTime;
        private Long durationSec;           // VOICE only
        private BigDecimal totalUsage;      // USAGE only
        private String measurementUnit;     // USAGE only
        private Integer smsCount;           // SMS only
        private BigDecimal appliedRate;
        private BigDecimal ourCharge;       // outgoingTotalCost
        private String currency;
    }
}
