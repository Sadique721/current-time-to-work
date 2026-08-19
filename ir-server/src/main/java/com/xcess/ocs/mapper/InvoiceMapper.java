package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.InvoiceDTO;
import com.xcess.ocs.entity.Invoice;

public class InvoiceMapper {

    public static InvoiceDTO mapToDTO(Invoice invoice) {
        if (invoice == null) return null;
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceId(invoice.getInvoiceId());
        if (invoice.getAgreement() != null) {
            dto.setAgreementId(invoice.getAgreement().getAgreementId());
            dto.setAgreementCode(invoice.getAgreement().getAgreementCode());
        }
        dto.setBillingCycleStart(invoice.getBillingCycleStart());
        dto.setBillingCycleEnd(invoice.getBillingCycleEnd());
        dto.setStatus(invoice.getStatus());
        dto.setCustomerTotal(invoice.getCustomerTotal());
        dto.setVendorTotal(invoice.getVendorTotal());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setNetPayableBy(invoice.getNetPayableBy());
        dto.setGeneratedDate(invoice.getGeneratedDate());
        dto.setPdfFilePath(invoice.getPdfFilePath());
        dto.setPdfChecksum(invoice.getPdfChecksum());
        dto.setPdfGeneratedAt(invoice.getPdfGeneratedAt());
        dto.setPdfErrorReason(invoice.getPdfErrorReason());

        // Tax fields
        dto.setTaxType(invoice.getTaxType());
        dto.setTaxableAmount(invoice.getTaxableAmount());
        dto.setTaxRate(invoice.getTaxRate());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setTotalInvoiceAmount(invoice.getTotalInvoiceAmount());
        dto.setCurrency(invoice.getCurrency());

        // Phase 2 audit fields
        dto.setTaxCalculationDate(invoice.getTaxCalculationDate());

        return dto;
    }
}
