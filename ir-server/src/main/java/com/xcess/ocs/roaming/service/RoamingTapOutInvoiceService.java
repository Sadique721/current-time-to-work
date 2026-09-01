package com.xcess.ocs.roaming.service;

import com.xcess.ocs.constants.AppConstants;
import com.xcess.ocs.constants.enums.NetPayableBy;
import com.xcess.ocs.constants.enums.SettlementType;
import com.xcess.ocs.dto.xml.*;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.repository.AgreementRepository;
import com.xcess.ocs.repository.AgreementTaxConfigRepository;
import com.xcess.ocs.repository.InvoiceRepository;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.service.PdfGenerationService;
import com.xcess.ocs.service.TaxCalculationService;
import com.xcess.ocs.service.TaxCalculationService.InvoiceTaxLineItem;
import com.xcess.ocs.service.TaxCalculationService.MultiTaxCalculationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoamingTapOutInvoiceService {

    public static final String SETTLEMENT_TYPE = SettlementType.ROAMING_TAP_OUT.label();

    private final AgreementRepository agreementRepository;
    private final AgreementTaxConfigRepository agreementTaxConfigRepository;
    private final InvoiceRepository invoiceRepository;
    private final TapFileRecordRepository tapFileRecordRepository;
    private final VoiceRatedCdrRepository voiceRatedCdrRepository;
    private final SmsRatedCdrRepository smsRatedCdrRepository;
    private final UsageRatedCdrRepository usageRatedCdrRepository;
    private final TaxCalculationService taxCalculationService;
    private final PdfGenerationService pdfGenerationService;

    @Value("${ocs.rounding.invoice-precision:2}")
    private int invoicePrecision;

    @Transactional
    public Invoice generateInvoice(Long agreementId, LocalDate cycleStart, LocalDate cycleEnd) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new RuntimeException("Agreement not found: " + agreementId));

        if (invoiceRepository.existsByAgreement_AgreementIdAndBillingCycleStartAndBillingCycleEndAndSettlementType(
                agreementId, cycleStart, cycleEnd, SETTLEMENT_TYPE)) {
            log.warn("TAP OUT invoice already exists for agreement={} period={} to {}", agreementId, cycleStart, cycleEnd);
            return null;
        }

        Partner partner = resolvePartner(agreement);
        if (partner == null) {
            log.error("TAP OUT invoice skipped: no partner found for agreement={}", agreement.getAgreementCode());
            return null;
        }

        String homePlmn = partner.getHplmn();
        if (homePlmn == null) {
            log.error("TAP OUT invoice skipped: no hplmn on partner={}", partner.getPartnerCode());
            return null;
        }

        // Load TAP OUT file records for this partner in the billing cycle
        LocalDateTime from = cycleStart.atStartOfDay();
        LocalDateTime to = cycleEnd.atTime(23, 59, 59);
        LocalDateTime tapFileTo = cycleEnd.plusDays(1).atTime(23, 59, 59);
        List<TapFileRecord> tapFiles = tapFileRecordRepository
                .findTapOutByPartnerAndDateRange(partner.getPartnerId(), from, tapFileTo);

        if (tapFiles.isEmpty()) {
            log.info("TAP OUT invoice skipped: no TAP OUT files for agreement={} period={} to {}",
                    agreement.getAgreementCode(), cycleStart, cycleEnd);
            return null;
        }

        // Load CDRs from the three tables by homePlmn + billing cycle window
        List<VoiceRatedCdr> voiceCdrs = voiceRatedCdrRepository.findRatedByHomePlmnAndDateRange(homePlmn, from, to,true);
        List<SmsRatedCdr> smsCdrs = smsRatedCdrRepository.findRatedByHomePlmnAndDateRange(homePlmn, from, to,true);
        List<UsageRatedCdr> usageCdrs = usageRatedCdrRepository.findRatedByHomePlmnAndDateRange(homePlmn, from, to,true);

        // Grand total our charge across all CDR types
        BigDecimal totalOurCharge = sumOutgoingCost(voiceCdrs, smsCdrs, usageCdrs)
                .setScale(invoicePrecision, RoundingMode.HALF_UP);

        if (totalOurCharge.compareTo(BigDecimal.ZERO) == 0 && voiceCdrs.isEmpty() && smsCdrs.isEmpty() && usageCdrs.isEmpty()) {
            log.info("TAP OUT invoice skipped: no rated CDRs for partner={} period={} to {}",
                    partner.getPartnerCode(), cycleStart, cycleEnd);
            return null;
        }

        String currency = partner.getBillingCurrency();

        // Tax calculation
        MultiTaxCalculationResult taxResult = null;
        if (!Boolean.TRUE.equals(agreement.getIsTaxExempt())) {
            try {
                List<AgreementTaxConfig> taxConfigs = agreementTaxConfigRepository
                        .findByAgreement_AgreementIdOrderByApplyOrderAsc(agreementId);
                if (!taxConfigs.isEmpty()) {
                    taxResult = taxCalculationService.calculateMultiTax(totalOurCharge, taxConfigs, cycleEnd);
                }
            } catch (Exception e) {
                log.warn("Tax calculation failed for TAP OUT invoice agreement={}: {}, proceeding without tax",
                        agreement.getAgreementCode(), e.getMessage());
            }
        }

        String templatePath = agreement.getOutgoingSettlementTemplate() != null
                ? agreement.getOutgoingSettlementTemplate().getTemplatePath()
                : null;

        String invoiceNumber = generateInvoiceNumber(cycleStart);
        String xmlContent = buildXml(agreement, partner, tapFiles,
                voiceCdrs, smsCdrs, usageCdrs,
                totalOurCharge, cycleStart, cycleEnd, invoiceNumber, currency, taxResult);

        // Persist Invoice entity
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setAgreement(agreement);
        invoice.setBillingCycleStart(cycleStart);
        invoice.setBillingCycleEnd(cycleEnd);
        invoice.setSettlementType(SETTLEMENT_TYPE);
        invoice.setXmlContent(xmlContent);
        invoice.setStatus(AppConstants.STATUS_GENERATED);
        invoice.setCustomerTotal(totalOurCharge);
        invoice.setNetAmount(totalOurCharge);
        invoice.setNetPayableBy(NetPayableBy.CUSTOMER.label());
        invoice.setGeneratedDate(LocalDateTime.now());
        invoice.setCurrency(currency);

        if (taxResult != null) {
            invoice.setTaxType(AppConstants.TAX_TYPE_MULTI);
            invoice.setTaxableAmount(taxResult.getTaxableAmount());
            invoice.setTaxAmount(taxResult.getTotalTax());
            invoice.setTotalInvoiceAmount(taxResult.getTotalInvoiceAmount());
            invoice.setTaxCalculationDate(taxResult.getTaxCalculationDate());

            List<InvoiceTaxDetail> details = new ArrayList<>();
            for (InvoiceTaxLineItem line : taxResult.getLineItems()) {
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
        } else {
            invoice.setTaxableAmount(totalOurCharge);
            invoice.setTotalInvoiceAmount(totalOurCharge);
        }

        Invoice saved = invoiceRepository.save(invoice);

        PdfGenerationService.PdfGenerationResult pdfResult =
                pdfGenerationService.generatePdf(xmlContent, saved.getInvoiceId(), cycleStart, templatePath);
        if (pdfResult.success) {
            saved.setPdfFilePath(pdfResult.filePath);
            saved.setPdfChecksum(pdfResult.checksum);
            saved.setPdfGeneratedAt(LocalDateTime.now());
        } else {
            saved.setPdfErrorReason(pdfResult.errorReason);
            log.warn("PDF generation failed for TAP OUT invoice={}: {}", invoiceNumber, pdfResult.errorReason);
        }

        invoiceRepository.save(saved);
        log.info("TAP OUT invoice generated: number={} agreement={} period={} to {} voice={} sms={} usage={} totalOurCharge={}",
                invoiceNumber, agreement.getAgreementCode(), cycleStart, cycleEnd,
                voiceCdrs.size(), smsCdrs.size(), usageCdrs.size(), totalOurCharge);

        return saved;
    }

    // ── XML builder ───────────────────────────────────────────────────────────

    private String buildXml(Agreement agreement, Partner partner,
                            List<TapFileRecord> tapFiles,
                            List<VoiceRatedCdr> ignoredVoice,
                            List<SmsRatedCdr> ignoredSms,
                            List<UsageRatedCdr> ignoredUsage,
                            BigDecimal totalOurCharge,
                            LocalDate cycleStart, LocalDate cycleEnd,
                            String invoiceNumber, String currency,
                            MultiTaxCalculationResult taxResult) {

        RoamingTapOutInvoiceXmlDTO dto = new RoamingTapOutInvoiceXmlDTO();
        dto.setInvoiceId(invoiceNumber);
        dto.setGeneratedDate(LocalDate.now().toString());
        dto.setCurrency(currency);

        // Company info
        if (partner.getOrganization() != null) {
            Organization org = partner.getOrganization();
            CompanyInfo cinfo = new CompanyInfo();
            cinfo.setName(org.getName());
            cinfo.setNameSuffix(org.getSuffixName());
            cinfo.setLegalName(org.getLegalName());
            cinfo.setAddress(org.getAddress());
            dto.setCompanyInfo(cinfo);
        }

        // Agreement info
        com.xcess.ocs.dto.xml.Agreement agreementDto = new com.xcess.ocs.dto.xml.Agreement();
        agreementDto.setAgreementCode(agreement.getAgreementCode());
        agreementDto.setBillingCycleStart(cycleStart.toString());
        agreementDto.setBillingCycleEnd(cycleEnd.toString());
        agreementDto.setSettlementType(SETTLEMENT_TYPE);
        agreementDto.setDescription("Roaming TAP OUT Settlement");
        dto.setAgreement(agreementDto);

        // Bill to
        BillTo billTo = new BillTo();
        billTo.setCompanyName(partner.getPartnerName());
        billTo.setAccountCode(partner.getPartnerCode());
        dto.setBillTo(billTo);

        // Build per-TAP-file sections
        for (TapFileRecord tap : tapFiles) {
            RoamingTapOutInvoiceXmlDTO.TapFileSection section = new RoamingTapOutInvoiceXmlDTO.TapFileSection();
            section.setFileName(tap.getFileName());
            section.setSequenceNo(tap.getFileSequenceNo());
            section.setSenderTadig(tap.getSenderTadig());
            section.setRecipientTadig(tap.getRecipientTadig());
            section.setGeneratedAt(tap.getProcessedAt() != null ? tap.getProcessedAt().toString() : "");
            section.setTotalCdrs(tap.getTotalRecords() != null ? tap.getTotalRecords().longValue() : 0L);
            section.setTapCharge(tap.getTotalCharge() != null && tap.getTapDecimalPlaces() != null
                    ? new BigDecimal(tap.getTotalCharge())
                            .divide(BigDecimal.TEN.pow(tap.getTapDecimalPlaces().intValue()), invoicePrecision, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);

            List<RoamingTapOutInvoiceXmlDTO.CdrLine> lines = new ArrayList<>();

            List<VoiceRatedCdr> voiceCdrs = voiceRatedCdrRepository.findRatedByTapFileId(tap.getTapFileId());
            List<SmsRatedCdr> smsCdrs = smsRatedCdrRepository.findRatedByTapFileId(tap.getTapFileId());
            List<UsageRatedCdr> usageCdrs = usageRatedCdrRepository.findRatedByTapFileId(tap.getTapFileId());

            // Voice CDR lines
            for (VoiceRatedCdr cdr : voiceCdrs) {
                RoamingTapOutInvoiceXmlDTO.CdrLine line = new RoamingTapOutInvoiceXmlDTO.CdrLine();
                line.setServiceType(ServiceType.VOICE.name());
                line.setCallingNumber(cdr.getCallingNumber());
                line.setCalledNumber(cdr.getCalledNumber());
                line.setStartTime(cdr.getStartTime() != null ? cdr.getStartTime().toString() : null);
                line.setDurationSec(cdr.getDurationSeconds());
                line.setAppliedRate(cdr.getOutgoingAppliedRate());
                line.setOurCharge(cdr.getOutgoingTotalCost());
                line.setCurrency(currency);
                lines.add(line);
            }

            // SMS CDR lines
            for (SmsRatedCdr cdr : smsCdrs) {
                RoamingTapOutInvoiceXmlDTO.CdrLine line = new RoamingTapOutInvoiceXmlDTO.CdrLine();
                line.setServiceType(ServiceType.SMS.name());
                line.setCallingNumber(cdr.getCallingNumber());
                line.setCalledNumber(cdr.getCalledNumber());
                line.setStartTime(cdr.getStartTime() != null ? cdr.getStartTime().toString() : null);
                line.setSmsCount(cdr.getEventNos());
                line.setAppliedRate(cdr.getOutgoingAppliedRate());
                line.setOurCharge(cdr.getOutgoingTotalCost());
                line.setCurrency(currency);
                lines.add(line);
            }

            // Usage CDR lines
            for (UsageRatedCdr cdr : usageCdrs) {
                RoamingTapOutInvoiceXmlDTO.CdrLine line = new RoamingTapOutInvoiceXmlDTO.CdrLine();
                line.setServiceType(ServiceType.USAGE.name());
                line.setCallingNumber(cdr.getSubscriberIdentity());
                line.setCalledNumber(cdr.getAccessPointName());
                line.setStartTime(cdr.getStartTime() != null ? cdr.getStartTime().toString() : null);
                line.setTotalUsage(cdr.getTotalUsage());
                line.setMeasurementUnit(cdr.getMeasurementUnit());
                line.setAppliedRate(cdr.getOutgoingAppliedRate());
                line.setOurCharge(cdr.getOutgoingTotalCost());
                line.setCurrency(currency);
                lines.add(line);
            }

            section.setCdrLines(lines);

            // Per-file our charge total
            BigDecimal fileTotalOurCharge = lines.stream()
                    .map(l -> l.getOurCharge() != null ? l.getOurCharge() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(invoicePrecision, RoundingMode.HALF_UP);
            section.setTotalOurCharge(fileTotalOurCharge);

            dto.getTapFiles().add(section);
        }

        // Overall totals
        NetSettlement net = new NetSettlement();
        net.setCustomerTotal(totalOurCharge.doubleValue());
        net.setNetAmount(totalOurCharge.doubleValue());
        net.setNetPayableBy(NetPayableBy.CUSTOMER.label());
        dto.setNetSettlement(net);

        BigDecimal totalInvoice = taxResult != null ? taxResult.getTotalInvoiceAmount() : totalOurCharge;
        dto.setTotalInvoiceAmount(totalInvoice.doubleValue());

        if (taxResult != null) {
            dto.setTaxCalculationDate(taxResult.getTaxCalculationDate() != null
                    ? taxResult.getTaxCalculationDate().toString() : null);
            List<InvoiceXmlDTO.TaxLineItem> taxItems = new ArrayList<>();
            for (InvoiceTaxLineItem line : taxResult.getLineItems()) {
                InvoiceXmlDTO.TaxLineItem item = new InvoiceXmlDTO.TaxLineItem();
                item.setApplyOrder(line.getApplyOrder());
                item.setTaxType(line.getTaxType());
                item.setTaxRate(line.getTaxRate() != null ? line.getTaxRate().doubleValue() : null);
                item.setTaxableAmount(line.getTaxableAmount().doubleValue());
                item.setTaxAmount(line.getTaxAmount().doubleValue());
                item.setApplyOn(line.getApplyOn());
                item.setAccumulateFromOrders(line.getAccumulateFromOrders());
                taxItems.add(item);
            }
            dto.setTaxLineItems(taxItems);
        }

        return RoamingTapOutXmlConverter.convertToXml(dto);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal sumOutgoingCost(List<VoiceRatedCdr> voice, List<SmsRatedCdr> sms, List<UsageRatedCdr> usage) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(voice.stream().map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        total = total.add(sms.stream().map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        total = total.add(usage.stream().map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
        return total;
    }

    private Partner resolvePartner(Agreement agreement) {
        if (agreement.getAccountAgreements() == null || agreement.getAccountAgreements().isEmpty()) return null;
        return agreement.getAccountAgreements().iterator().next().getAccount().getPartner();
    }

    private String generateInvoiceNumber(LocalDate cycleStart) {
        String yearMonth = cycleStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
        String pattern = "RMG-OUT-" + yearMonth + "-%";
        long seq = invoiceRepository.countByInvoiceNumberLike(pattern) + 1;
        return String.format("RMG-OUT-%s-%03d", yearMonth, seq);
    }
}
