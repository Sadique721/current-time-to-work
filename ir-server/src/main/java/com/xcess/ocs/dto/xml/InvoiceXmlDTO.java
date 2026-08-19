package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Invoice")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class InvoiceXmlDTO {

    private String invoiceId;
    private String generatedDate;
    private String currency;

    private String taxCalculationDate;
    private Double totalInvoiceAmount;

    private CompanyInfo companyInfo;
    private Agreement agreement;
    private BillTo billTo;

    @XmlElementWrapper(name = "Accounts")
    @XmlElement(name = "Account")
    private List<Account> accounts;

    @XmlElementWrapper(name = "Summary")
    @XmlElement(name = "SummaryAccount")
    private List<SummaryAccount> summary;

    @XmlElementWrapper(name = "TaxLineItems")
    @XmlElement(name = "TaxLineItem")
    private List<TaxLineItem> taxLineItems;

    private NetSettlement netSettlement;
    private Footer footer;

    public List<SummaryAccount> getSummary() {
        if(summary == null){
            summary = new ArrayList<SummaryAccount>();
        }
        return summary;
    }

    public List<Account> getAccounts() {
        if(accounts == null){
            accounts = new ArrayList<Account>();
        }
        return accounts;
    }

    @Data
    public static class TaxLineItem {
        private Integer applyOrder;
        private String taxType;
        private Double taxRate;
        private Double taxableAmount;
        private Double taxAmount;
        private String applyOn;
        private String accumulateFromOrders;
    }
}
