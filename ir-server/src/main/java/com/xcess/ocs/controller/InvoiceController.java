package com.xcess.ocs.controller;

import com.xcess.ocs.dto.InvoiceDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.InvoiceSearchDTO;
import com.xcess.ocs.service.InvoiceGenerationService;
import com.xcess.ocs.service.InvoiceService;
import com.xcess.ocs.constants.enums.SettlementType;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceGenerationService invoiceGenerationService;

    @Value("${invoice.storage.base-path}")
    private String basePath;

    @PostMapping("/paginated")
    public ResponseEntity<PageResponseDTO<InvoiceDTO>> getInvoicesInPage(
            @Valid @RequestBody PageRequestDTO<InvoiceSearchDTO> pageRequestDTO) {
        log.info("REST request to get invoices in page with search criteria");

        Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        PageResponseDTO<InvoiceDTO> response;

        if (pageRequestDTO.getSearchCriteria() != null) {
            response = invoiceService.searchInvoices(pageRequestDTO.getSearchCriteria(), pageable);
            log.info("Retrieved filtered invoices in a page");
        } else {
            response = invoiceService.getInvoicesInPages(pageable);
            log.info("Retrieved all invoices in a page");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoice(@PathVariable Long id) {
        log.info("Fetching invoice: {}", id);
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping("/agreement/{agreementId}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByAgreement(@PathVariable Long agreementId) {
        log.info("Fetching invoices for agreement: {}", agreementId);
        return ResponseEntity.ok(invoiceService.getInvoicesByAgreement(agreementId));
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<String> getInvoiceXml(@PathVariable Long id) {
        log.info("Fetching XML for invoice: {}", id);
        String xml = invoiceService.getInvoiceXml(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".xml");
        
        return ResponseEntity.ok().headers(headers).body(xml);
    }

    @Autowired
    private com.xcess.ocs.roaming.service.RoamingTapOutInvoiceService roamingTapOutInvoiceService;

    @PostMapping("/generate")
    public ResponseEntity<InvoiceDTO> generateInvoice(
            @RequestParam Long agreementId,
            @RequestParam LocalDate billingStart,
            @RequestParam LocalDate billingEnd,
            @RequestParam String settlementType) {
        log.info("Manually generating {} invoice for agreement: {}, period: {} to {}", 
                settlementType, agreementId, billingStart, billingEnd);
        
        SettlementType settlementTypeEnum = SettlementType.fromString(settlementType);
        if (SettlementType.ROAMING_TAP_OUT == settlementTypeEnum) {
            com.xcess.ocs.entity.Invoice invoiceEntity = roamingTapOutInvoiceService.generateInvoice(agreementId, billingStart, billingEnd);
            if (invoiceEntity == null) {
                return ResponseEntity.badRequest().build();
            }
            return ResponseEntity.ok(invoiceService.getInvoiceById(invoiceEntity.getInvoiceId()));
        }
        
        InvoiceDTO invoice = invoiceGenerationService.generateInvoiceDTO(agreementId, billingStart, billingEnd, settlementType);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<Resource> getInvoicePdf(@PathVariable Long id) {
        log.info("Fetching PDF for invoice: {}", id);
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        
        if (invoice.getPdfFilePath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        String fullPath = Paths.get(basePath, invoice.getPdfFilePath()).toString();
        Resource resource = new FileSystemResource(fullPath);
        
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + id + ".pdf");
        
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @PostMapping("/{id}/generate-pdf")
    public ResponseEntity<?> generatePdfForInvoice(@PathVariable Long id) {
        log.info("Generating PDF for invoice: {}", id);
        invoiceService.regeneratePdf(id);
        return ResponseEntity.ok().build();
    }

}
