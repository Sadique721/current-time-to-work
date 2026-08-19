package com.xcess.ocs.service;

import com.xcess.ocs.dto.InvoiceDTO;
import com.xcess.ocs.dto.InvoiceTaxDetailDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.InvoiceSearchDTO;
import com.xcess.ocs.entity.Invoice;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.InvoiceRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return toDTO(invoice);
    }

    public List<InvoiceDTO> getInvoicesByAgreement(Long agreementId) {
        return invoiceRepository.findByAgreement_AgreementId(agreementId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public String getInvoiceXml(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return invoice.getXmlContent();
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceId(invoice.getInvoiceId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setAgreementId(invoice.getAgreement().getAgreementId());
        dto.setAgreementCode(invoice.getAgreement().getAgreementCode());
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

        if (invoice.getInvoiceTaxDetails() != null) {
            dto.setInvoiceTaxDetails(invoice.getInvoiceTaxDetails().stream()
                    .map(detail -> {
                        InvoiceTaxDetailDTO detailDto = new InvoiceTaxDetailDTO();
                        detailDto.setId(detail.getId());
                        detailDto.setApplyOrder(detail.getApplyOrder());
                        detailDto.setTaxConfigId(detail.getTaxConfigId());
                        detailDto.setTaxType(detail.getTaxType());
                        detailDto.setTaxRate(detail.getTaxRate());
                        detailDto.setTaxableAmount(detail.getTaxableAmount());
                        detailDto.setTaxAmount(detail.getTaxAmount());
                        detailDto.setApplyOn(detail.getApplyOn());
                        detailDto.setAccumulateFromOrders(detail.getAccumulateFromOrders());
                        return detailDto;
                    })
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public PageResponseDTO<InvoiceDTO> getInvoicesInPages(Pageable pageable) {
        log.debug("Fetching invoices in pages");
        Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);
        List<InvoiceDTO> invoiceDTOs = invoicePage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        log.info("Retrieved {} invoices in a page", invoiceDTOs.size());
        return PaginationUtils.buildGetResponseDTO(invoiceDTOs, invoicePage);
    }

    public PageResponseDTO<InvoiceDTO> searchInvoices(InvoiceSearchDTO invoiceSearchDTO, Pageable pageable) {
        log.debug("Searching invoices with criteria: {}", invoiceSearchDTO);
        String searchTerm = invoiceSearchDTO != null ? invoiceSearchDTO.getSearchTerm() : null;
        String status = invoiceSearchDTO != null ? invoiceSearchDTO.getStatus() : null;
        var billingCycleStartFrom = invoiceSearchDTO != null ? invoiceSearchDTO.getBillingCycleStartFrom() : null;
        var billingCycleStartTo = invoiceSearchDTO != null ? invoiceSearchDTO.getBillingCycleStartTo() : null;
        Long agreementId = invoiceSearchDTO != null ? invoiceSearchDTO.getAgreementId() : null;
        String taxType = invoiceSearchDTO != null ? invoiceSearchDTO.getTaxType() : null;

        Page<Invoice> invoicesPage = invoiceRepository.searchInvoices(
                searchTerm,
                status,
                billingCycleStartFrom,
                billingCycleStartTo,
                agreementId,
                taxType,
                pageable
        );
        List<InvoiceDTO> invoices = invoicesPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        log.debug("Found {} invoices matching criteria", invoices.size());
        return PaginationUtils.buildGetResponseDTO(invoices, invoicesPage);
    }

    @Transactional
    public void regeneratePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getXmlContent() == null) {
            throw new RuntimeException("No XML content available for PDF generation");
        }
        
        String settlementType = invoice.getSettlementType();
        String templatePath = null;
        
        if ("INCOMING".equals(settlementType) && invoice.getAgreement().getIncomingSettlementTemplate() != null) {
            templatePath = invoice.getAgreement().getIncomingSettlementTemplate().getTemplatePath();
        } else if (("OUTGOING".equals(settlementType) || "ROAMING_TAP_OUT".equals(settlementType)) && invoice.getAgreement().getOutgoingSettlementTemplate() != null) {
            templatePath = invoice.getAgreement().getOutgoingSettlementTemplate().getTemplatePath();
        } else if ("NET".equals(settlementType) && invoice.getAgreement().getNetSettlementTemplate() != null) {
            templatePath = invoice.getAgreement().getNetSettlementTemplate().getTemplatePath();
        }
        
        PdfGenerationService.PdfGenerationResult result = pdfGenerationService.generatePdf(
            invoice.getXmlContent(), 
            invoice.getInvoiceId(), 
            invoice.getBillingCycleStart(),
            templatePath
        );
        
        if (result.success) {
            invoice.setPdfFilePath(result.filePath);
            invoice.setPdfChecksum(result.checksum);
            invoice.setPdfGeneratedAt(LocalDateTime.now());
            invoice.setPdfErrorReason(null);
        } else {
            invoice.setPdfErrorReason(result.errorReason);
        }
        
        invoiceRepository.save(invoice);
    }
}
