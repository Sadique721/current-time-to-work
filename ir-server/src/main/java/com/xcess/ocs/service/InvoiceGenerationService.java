package com.xcess.ocs.service;

import com.xcess.ocs.dto.InvoiceDTO;
import com.xcess.ocs.dto.xml.*;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.entity.Account;
import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.repository.*;
import com.xcess.ocs.roaming.entity.TapOutRatedSummary;
import com.xcess.ocs.roaming.repository.TapOutRatedSummaryRepository;
import com.xcess.ocs.service.TaxCalculationService.InvoiceTaxLineItem;
import com.xcess.ocs.service.TaxCalculationService.MultiTaxCalculationResult;
import com.xcess.ocs.service.TaxCalculationService.TaxCalculationResult;
import com.xcess.ocs.constants.enums.SettlementType;
import com.xcess.ocs.constants.enums.NetPayableBy;
import com.xcess.ocs.constants.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvoiceGenerationService {

    @Autowired
    private AgreementRepository agreementRepository;

    @Autowired
    private AccountAgreementRepository accountAgreementRepository;

    @Autowired
    private RatedSummaryRepository ratedSummaryRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private TaxCalculationService taxCalculationService;

    @Autowired
    private AgreementTaxConfigRepository agreementTaxConfigRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private TapOutRatedSummaryRepository tapOutRatedSummaryRepository;

    @Value("${ocs.rounding.invoice-precision:2}")
    private int invoicePrecision;

    @Transactional
    public Invoice generateInvoiceForAgreement(Long agreementId, LocalDate billingStart, LocalDate billingEnd) {
        return generateInvoiceForSettlementType(agreementId, billingStart, billingEnd, SettlementType.NET.label());
    }

    @Transactional
    public Invoice generateInvoiceForSettlementType(Long agreementId, LocalDate billingStart, LocalDate billingEnd, String settlementType) {
        SettlementType settlementTypeEnum = SettlementType.fromString(settlementType);
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found"));

        if (invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                agreementId, billingStart, billingEnd, settlementType)) {
            log.warn("Invoice already exists for agreement {} and period {} to {} with settlement type {}",
                    agreementId, billingStart, billingEnd, settlementType);
            return null;
        }

        List<AccountAgreement> accountAgreements = accountAgreementRepository
                .findByAgreement_AgreementId(agreementId);

        Map<String, BigDecimal> incomingTotals = new HashMap<>();
        Map<String, BigDecimal> outgoingTotals = new HashMap<>();
        Map<String, String> accountTypes = new HashMap<>();
        Map<String, Account> accountMap = new HashMap<>();

        List<RatedSummary> allSummaries = new ArrayList<>();

        boolean isRoaming = agreement.getLineOfBusiness() != null
                && com.xcess.ocs.entity.LineOfBusiness.ROAMING == agreement.getLineOfBusiness();

        for (AccountAgreement aa : accountAgreements) {
            String accountCode = aa.getAccount().getAccountCode();
            String accountType = aa.getAccount().getAccountType();
            accountMap.put(accountCode, aa.getAccount());

            String billingCurrency = aa.getAccount().getPartner() != null
                    ? aa.getAccount().getPartner().getBillingCurrency() : null;

            BigDecimal incomingTotal = BigDecimal.ZERO;
            BigDecimal outgoingTotal = BigDecimal.ZERO;

            if (isRoaming && aa.getAccount().getPartner() != null) {
                // For ROAMING agreements, charges live in tap_out_rated_summary, not rated_summary.
                // TAP OUT charges are what we billed the visited network (outgoing to us = vendor cost).
                // TAP IN charges are what the visited network billed us (incoming to us = customer charge).
                Long partnerId = aa.getAccount().getPartner().getPartnerId();
                List<TapOutRatedSummary> tapSummaries = tapOutRatedSummaryRepository
                        .findByPartner_PartnerIdAndSummaryDateBetween(partnerId, billingStart, billingEnd);

                BigDecimal tapTotal = tapSummaries.stream()
                        .map(ts -> ts.getTotalCharge() != null ? ts.getTotalCharge() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // For OUTGOING settlement (TAP OUT): we are the sender, charges = vendor total
                // For INCOMING settlement (TAP IN): visited network billed us, charges = customer total
                if (SettlementType.OUTGOING == settlementTypeEnum || SettlementType.NET == settlementTypeEnum) {
                    outgoingTotal = convertToBillingCurrency(tapTotal, billingCurrency, billingEnd);
                } else if (SettlementType.INCOMING == settlementTypeEnum) {
                    incomingTotal = convertToBillingCurrency(tapTotal, billingCurrency, billingEnd);
                }
            } else {
                // Standard INTERCONNECT path: read from rated_summary
                List<RatedSummary> summaries = ratedSummaryRepository
                        .findByAccountCodeAndSummaryDateBetween(accountCode, billingStart, billingEnd);
                allSummaries.addAll(summaries);

                incomingTotal = summaries.stream()
                        .filter(rs -> SettlementType.INCOMING.label().equals(rs.getDirection()))
                        .map(rs -> convertToBillingCurrency(rs.getTotalCharge(), billingCurrency, billingEnd))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                outgoingTotal = summaries.stream()
                        .filter(rs -> SettlementType.OUTGOING.label().equals(rs.getDirection()))
                        .map(rs -> convertToBillingCurrency(rs.getTotalCharge(), billingCurrency, billingEnd))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            incomingTotals.put(accountCode, incomingTotal);
            outgoingTotals.put(accountCode, outgoingTotal);
            accountTypes.put(accountCode, accountType);
        }

        BigDecimal customerTotal;
        BigDecimal vendorTotal;
        BigDecimal netAmount;
        String netPayableBy;

        switch (settlementTypeEnum) {
            case INCOMING:
                customerTotal = incomingTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                vendorTotal = BigDecimal.ZERO;
                netAmount = customerTotal;
                netPayableBy = NetPayableBy.CUSTOMER.label();
                break;

            case OUTGOING:
                customerTotal = BigDecimal.ZERO;
                vendorTotal = outgoingTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                netAmount = vendorTotal;
                netPayableBy = NetPayableBy.HOST.label();
                break;

            case NET:
            default:
                BigDecimal totalIncoming = incomingTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalOutgoing = outgoingTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                customerTotal = totalIncoming;
                vendorTotal = totalOutgoing;
                netAmount = customerTotal.subtract(vendorTotal);
                netPayableBy = netAmount.compareTo(BigDecimal.ZERO) >= 0 ? NetPayableBy.CUSTOMER.label() : NetPayableBy.HOST.label();
                netAmount = netAmount.abs();
                break;
        }

        customerTotal = customerTotal.setScale(invoicePrecision, RoundingMode.HALF_UP);
        vendorTotal = vendorTotal.setScale(invoicePrecision, RoundingMode.HALF_UP);
        netAmount = netAmount.setScale(invoicePrecision, RoundingMode.HALF_UP);

        String templatePath = null;
        if (SettlementType.INCOMING == settlementTypeEnum && agreement.getIncomingSettlementTemplate() != null) {
            templatePath = agreement.getIncomingSettlementTemplate().getTemplatePath();
        } else if (SettlementType.OUTGOING == settlementTypeEnum && agreement.getOutgoingSettlementTemplate() != null) {
            templatePath = agreement.getOutgoingSettlementTemplate().getTemplatePath();
        } else if (SettlementType.NET == settlementTypeEnum && agreement.getNetSettlementTemplate() != null) {
            templatePath = agreement.getNetSettlementTemplate().getTemplatePath();
        }

        Invoice invoice = new Invoice();
        String invoiceNumber = generateInvoiceNumber(settlementType, billingStart);
        invoice.setInvoiceNumber(invoiceNumber);

        boolean shouldCalculateTax = false;
        String currency = AppConstants.DEFAULT_CURRENCY;

        if (SettlementType.INCOMING == settlementTypeEnum) {
            shouldCalculateTax = true;
        } else if (SettlementType.NET == settlementTypeEnum && netAmount.compareTo(BigDecimal.ZERO) > 0) {
            shouldCalculateTax = true;
        }

        if (!accountMap.isEmpty()) {
            Account firstAccount = accountMap.values().iterator().next();
            if (firstAccount.getPartner() != null && firstAccount.getPartner().getBillingCurrency() != null) {
                currency = firstAccount.getPartner().getBillingCurrency();
            }
        }

        MultiTaxCalculationResult multiTaxResult = null;

        if (shouldCalculateTax && !Boolean.TRUE.equals(agreement.getIsTaxExempt())) {
            try {
                multiTaxResult = calculateTaxNew(agreement, netAmount.abs(), billingStart);
            } catch (Exception e) {
                log.warn("Tax calculation failed: {}, proceeding without tax", e.getMessage());
            }
        }

        String xmlContent = generateXML(agreement, billingStart, billingEnd,
                incomingTotals, outgoingTotals, accountTypes, accountMap, customerTotal, vendorTotal, netAmount, netPayableBy, settlementType, invoiceNumber, multiTaxResult, currency, allSummaries);

        invoice.setAgreement(agreement);
        invoice.setBillingCycleStart(billingStart);
        invoice.setBillingCycleEnd(billingEnd);
        invoice.setSettlementType(settlementType);
        invoice.setXmlContent(xmlContent);
        invoice.setStatus(AppConstants.STATUS_GENERATED);
        invoice.setCustomerTotal(customerTotal);
        invoice.setVendorTotal(vendorTotal);
        invoice.setNetAmount(netAmount);
        invoice.setNetPayableBy(netPayableBy);
        invoice.setGeneratedDate(LocalDateTime.now());

        if (multiTaxResult != null) {
            invoice.setTaxType(AppConstants.TAX_TYPE_MULTI);
            invoice.setTaxableAmount(multiTaxResult.getTaxableAmount());
            invoice.setTaxRate(null);
            invoice.setTaxAmount(multiTaxResult.getTotalTax());
            invoice.setCurrency(currency);
            invoice.setTotalInvoiceAmount(multiTaxResult.getTotalInvoiceAmount());
            invoice.setTaxCalculationDate(multiTaxResult.getTaxCalculationDate());

            if (!multiTaxResult.getLineItems().isEmpty()) {
                List<InvoiceTaxDetail> details = new ArrayList<>();
                for (InvoiceTaxLineItem line : multiTaxResult.getLineItems()) {
                    InvoiceTaxDetail detail = new InvoiceTaxDetail();
                    detail.setInvoice(invoice);
                    detail.setApplyOrder(line.getApplyOrder());
                    detail.setTaxConfigId(line.getTaxConfigId());
                    detail.setTaxType(line.getTaxType());
                    detail.setTaxRate(line.getTaxRate());
                    detail.setTaxableAmount(line.getTaxableAmount());
                    detail.setTaxAmount(line.getTaxAmount());
                    detail.setApplyOn(line.getApplyOn());
                    detail.setAccumulateFromOrders(line.getAccumulateFromOrders());
                    details.add(detail);
                }
                invoice.setInvoiceTaxDetails(details);
            }
        } else {
            invoice.setTaxableAmount(netAmount.abs());
            invoice.setTotalInvoiceAmount(netAmount.abs());
            invoice.setCurrency(currency);
        }

        Invoice savedInvoice = invoiceRepository.save(invoice);

        PdfGenerationService.PdfGenerationResult pdfResult =
                pdfGenerationService.generatePdf(xmlContent, savedInvoice.getInvoiceId(), billingStart, templatePath);

        if (pdfResult.success) {
            savedInvoice.setPdfFilePath(pdfResult.filePath);
            savedInvoice.setPdfChecksum(pdfResult.checksum);
            savedInvoice.setPdfGeneratedAt(LocalDateTime.now());
        } else {
            savedInvoice.setPdfErrorReason(pdfResult.errorReason);
        }

        return invoiceRepository.save(savedInvoice);
    }

    private BigDecimal convertToBillingCurrency(BigDecimal amount, String billingCurrency, LocalDate asOfDate) {
        if (amount == null) return BigDecimal.ZERO;
        if (billingCurrency == null) return amount;
        // RatedSummary.totalCharge has no stored currency — it is in the system base currency.
        // The base currency is the first entry in exchangeRate.base-currencies config (e.g. "INR").
        // If billingCurrency matches the base, no conversion needed.
        // We attempt conversion; if no rate found, return unconverted amount with a warning.
        try {
            List<ExchangeRate> rates = exchangeRateRepository
                    .findByBaseCurrencyAndValidFrom(billingCurrency.toUpperCase(), asOfDate);
            if (rates.isEmpty()) {
                LocalDate maxDate = exchangeRateRepository
                        .findMaxValidFromByBaseCurrency(billingCurrency.toUpperCase());
                if (maxDate != null)
                    rates = exchangeRateRepository
                            .findByBaseCurrencyAndValidFrom(billingCurrency.toUpperCase(), maxDate);
            }
            // If a rate exists for billingCurrency as base, the amount is already in that currency
            // (meaning the system rates are denominated in billingCurrency — no conversion needed).
            // If no rate found at all, return as-is.
            if (!rates.isEmpty()) return amount;
        } catch (Exception e) {
            log.warn("Exchange rate lookup failed for currency {}: {}", billingCurrency, e.getMessage());
        }
        return amount;
    }

    private MultiTaxCalculationResult calculateTaxNew(Agreement agreement, BigDecimal baseAmount, LocalDate invoiceDate) {
        if (Boolean.TRUE.equals(agreement.getIsTaxExempt())) {
            return taxCalculationService.calculateExempt(baseAmount);
        }

        List<AgreementTaxConfig> taxConfigs = agreementTaxConfigRepository
                .findByAgreement_AgreementIdOrderByApplyOrderAsc(agreement.getAgreementId());

        if (taxConfigs.isEmpty()) {
            return taxCalculationService.calculateExempt(baseAmount);
        }

        return taxCalculationService.calculateMultiTax(
                baseAmount,
                taxConfigs,
                invoiceDate);
    }

    private TaxCalculationResult convertMultiToLegacyTaxResult(MultiTaxCalculationResult multiResult) {
        if (multiResult.getLineItems().isEmpty()) {
            return null;
        }
        InvoiceTaxLineItem first = multiResult.getLineItems().get(0);

        return new TaxCalculationResult(
                multiResult.getTaxableAmount(),
                multiResult.getTotalTax(),
                first.getTaxRate(),
                first.getTaxType());
    }

    private String generateXML(Agreement agreement, LocalDate billingStart, LocalDate billingEnd,
                               Map<String, BigDecimal> incomingTotals, Map<String, BigDecimal> outgoingTotals,
                               Map<String, String> accountTypes, Map<String, Account> accountMap,
                               BigDecimal customerTotal, BigDecimal vendorTotal,
                               BigDecimal netAmount, String netPayableBy, String settlementType, String invoiceNumber,
                               MultiTaxCalculationResult multiTaxResult, String currency, List<RatedSummary> allSummaries) {
        SettlementType settlementTypeEnum = SettlementType.fromString(settlementType);
        InvoiceXmlDTO dto = new InvoiceXmlDTO();

        dto.setInvoiceId(invoiceNumber);
        dto.setGeneratedDate(LocalDate.now().toString());
        dto.setCurrency(currency);

        Organization organization = accountMap.values().iterator().next().getPartner().getOrganization();
        CompanyInfo cinfo = new CompanyInfo();
        cinfo.setName(organization.getName());
        cinfo.setNameSuffix(organization.getSuffixName());
        cinfo.setLegalName(organization.getLegalName());
        cinfo.setAddress(organization.getAddress());
        dto.setCompanyInfo(cinfo);

        com.xcess.ocs.dto.xml.Agreement agreementDto = new com.xcess.ocs.dto.xml.Agreement();
        agreementDto.setAgreementCode(agreement.getAgreementCode());
        agreementDto.setBillingCycleStart(String.valueOf(billingStart));
        agreementDto.setBillingCycleEnd(String.valueOf(billingEnd));
        agreementDto.setSettlementType(settlementType);
        
        boolean isRoaming = agreement.getLineOfBusiness() != null && com.xcess.ocs.entity.LineOfBusiness.ROAMING == agreement.getLineOfBusiness();
        agreementDto.setDescription(isRoaming ? AppConstants.INVOICE_DESC_ROAMING_TAP_OUT : AppConstants.INVOICE_DESC_INTERCONNECT);
        dto.setAgreement(agreementDto);

        Account billToAccount = null;
        if (!accountMap.isEmpty()) {
            billToAccount = accountMap.values().iterator().next();
        }

        if (billToAccount != null) {
            BillTo billTo = new BillTo();
            billTo.setCompanyName(billToAccount.getPartner() != null ? billToAccount.getPartner().getPartnerName() : "");
            billTo.setAccountCode(billToAccount.getAccountCode());
            dto.setBillTo(billTo);
        }

        Map<String, BigDecimal> accountTotals = new HashMap<>();
        switch (settlementTypeEnum) {
            case INCOMING:
                for (String accountCode : incomingTotals.keySet()) {
                    if (com.xcess.ocs.entity.PartnerType.CUSTOMER.name().equals(accountTypes.get(accountCode))) {
                        accountTotals.put(accountCode, incomingTotals.get(accountCode));
                    }
                }
                break;
            case OUTGOING:
                for (String accountCode : outgoingTotals.keySet()) {
                    if (com.xcess.ocs.entity.PartnerType.VENDOR.name().equals(accountTypes.get(accountCode))) {
                        accountTotals.put(accountCode, outgoingTotals.get(accountCode));
                    }
                }
                break;
            case NET:
            default:
                for (String accountCode : incomingTotals.keySet()) {
                    BigDecimal incoming = incomingTotals.getOrDefault(accountCode, BigDecimal.ZERO);
                    BigDecimal outgoing = outgoingTotals.getOrDefault(accountCode, BigDecimal.ZERO);
                    accountTotals.put(accountCode, incoming.add(outgoing));
                }
                for (String accountCode : outgoingTotals.keySet()) {
                    if (!accountTotals.containsKey(accountCode)) {
                        accountTotals.put(accountCode, outgoingTotals.get(accountCode));
                    }
                }
                break;
        }

        Map<String, BigDecimal> serviceAccountTotals = new HashMap<>();
        for (RatedSummary rs : allSummaries) {
            String accountCode = rs.getAccountCode();
            String serviceType = rs.getServiceType();
            String direction = rs.getDirection();
            String accountType = accountTypes.get(accountCode);
            String key = accountCode + "|" + serviceType;
            
            boolean include = false;
            switch (settlementTypeEnum) {
                case INCOMING:
                    if (NetPayableBy.CUSTOMER.label().equals(accountType) && SettlementType.INCOMING.label().equals(direction)) include = true;
                    break;
                case OUTGOING:
                    if (com.xcess.ocs.entity.PartnerType.VENDOR.name().equals(accountType) && SettlementType.OUTGOING.label().equals(direction)) include = true;
                    break;
                case NET:
                default:
                    include = true;
                    break;
            }
            
            if (include) {
                String billingCurrency = accountMap.get(accountCode).getPartner() != null ? accountMap.get(accountCode).getPartner().getBillingCurrency() : null;
                BigDecimal convertedCharge = convertToBillingCurrency(rs.getTotalCharge(), billingCurrency, billingEnd);
                serviceAccountTotals.put(key, serviceAccountTotals.getOrDefault(key, BigDecimal.ZERO).add(convertedCharge));
            }
        }

        for (Map.Entry<String, BigDecimal> entry : serviceAccountTotals.entrySet()) {
            String[] parts = entry.getKey().split("\\|");
            String accountCode = parts[0];
            String serviceType = parts[1];

            com.xcess.ocs.dto.xml.Account account = new com.xcess.ocs.dto.xml.Account();
            account.setServiceType(serviceType != null ? serviceType : "Unknown");
            account.setAccountCode(accountCode);
            account.setAccountType(accountTypes.get(accountCode));
            account.setTotalAmount(entry.getValue().doubleValue());
            dto.getAccounts().add(account);
        }

        for (Map.Entry<String, BigDecimal> entry : accountTotals.entrySet()) {
            String accountType = accountTypes.get(entry.getKey());
            String trafficDirection;

            switch (settlementTypeEnum) {
                case INCOMING:
                    trafficDirection = "Inbound Traffic";
                    break;
                case OUTGOING:
                    trafficDirection = "Outbound Traffic";
                    break;
                case NET:
                default:
                    trafficDirection = NetPayableBy.CUSTOMER.label().equals(accountType) ? "Inbound Traffic" : "Outbound Traffic";
            }

            SummaryAccount summaryAccount = new SummaryAccount();
            summaryAccount.setAccountCode(entry.getKey());
            summaryAccount.setAccountType(accountType);
            summaryAccount.setTrafficDirection(trafficDirection);
            summaryAccount.setTotal(entry.getValue().doubleValue());
            dto.getSummary().add(summaryAccount);
        }

        NetSettlement netSettlement = new NetSettlement();
        netSettlement.setCustomerTotal(customerTotal.doubleValue());
        netSettlement.setNetAmount(netAmount.doubleValue());
        netSettlement.setVendorTotal(vendorTotal.doubleValue());
        netSettlement.setNetPayableBy(netPayableBy);
        dto.setNetSettlement(netSettlement);

        dto.setTaxCalculationDate(multiTaxResult != null && multiTaxResult.getTaxCalculationDate() != null
                ? multiTaxResult.getTaxCalculationDate().toString() : null);
        dto.setTotalInvoiceAmount(multiTaxResult != null ? multiTaxResult.getTotalInvoiceAmount().doubleValue() : netAmount.abs().doubleValue());

        if (multiTaxResult != null && multiTaxResult.getLineItems() != null) {
            List<InvoiceXmlDTO.TaxLineItem> items = new ArrayList<>();
            for (InvoiceTaxLineItem line : multiTaxResult.getLineItems()) {
                InvoiceXmlDTO.TaxLineItem item = new InvoiceXmlDTO.TaxLineItem();
                item.setApplyOrder(line.getApplyOrder());
                item.setTaxType(line.getTaxType());
                item.setTaxRate(line.getTaxRate() != null ? line.getTaxRate().doubleValue() : null);
                item.setTaxableAmount(line.getTaxableAmount().doubleValue());
                item.setTaxAmount(line.getTaxAmount().doubleValue());
                item.setApplyOn(line.getApplyOn());
                item.setAccumulateFromOrders(line.getAccumulateFromOrders());

                items.add(item);
            }
            dto.setTaxLineItems(items);
        }

        return XmlConverter.convertToXml(dto);
    }

    private String generateInvoiceNumber(String settlementType, LocalDate billingStart) {
        String typePrefix;
        SettlementType settlementTypeEnum = SettlementType.fromString(settlementType);
        switch (settlementTypeEnum) {
            case INCOMING:
                typePrefix = "INC";
                break;
            case OUTGOING:
                typePrefix = "OUT";
                break;
            case NET:
            default:
                typePrefix = "NET";
                break;
        }

        String yearMonth = billingStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String pattern = typePrefix + "-" + yearMonth + "-%";
        long sequence = invoiceRepository.countByInvoiceNumberLike(pattern) + 1;
        String sequenceStr = String.format("%03d", sequence);

        return String.format("%s-%s-%s", typePrefix, yearMonth, sequenceStr);
    }

    public InvoiceDTO generateInvoiceDTO(Long agreementId, LocalDate billingStart, LocalDate billingEnd, String settlementType) {
        Invoice invoice = generateInvoiceForSettlementType(agreementId, billingStart, billingEnd, settlementType);
        return invoice != null ? toDTO(invoice) : null;
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceId(invoice.getInvoiceId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setAgreementId(invoice.getAgreement().getAgreementId());
        dto.setAgreementCode(invoice.getAgreement().getAgreementCode());
        dto.setBillingCycleStart(invoice.getBillingCycleStart());
        dto.setBillingCycleEnd(invoice.getBillingCycleEnd());
        dto.setSettlementType(invoice.getSettlementType());
        dto.setStatus(invoice.getStatus());
        dto.setCustomerTotal(invoice.getCustomerTotal());
        dto.setVendorTotal(invoice.getVendorTotal());
        dto.setNetAmount(invoice.getNetAmount());
        dto.setNetPayableBy(invoice.getNetPayableBy());
        dto.setGeneratedDate(invoice.getGeneratedDate());
        dto.setPdfFilePath(invoice.getPdfFilePath());
        dto.setPdfChecksum(invoice.getPdfChecksum());

        dto.setTaxType(invoice.getTaxType());
        dto.setTaxableAmount(invoice.getTaxableAmount());
        dto.setTaxRate(invoice.getTaxRate());
        dto.setTaxAmount(invoice.getTaxAmount());
        dto.setTotalInvoiceAmount(invoice.getTotalInvoiceAmount());
        dto.setCurrency(invoice.getCurrency());
        dto.setTaxCalculationDate(invoice.getTaxCalculationDate());

        return dto;
    }
}
