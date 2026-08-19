package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.entity.AgreementTaxConfig;
import com.xcess.ocs.repository.AgreementRepository;
import com.xcess.ocs.repository.AgreementTaxConfigRepository;
import com.xcess.ocs.roaming.entity.RoamingRatedSummary;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.repository.RoamingCdrRepository;
import com.xcess.ocs.roaming.repository.RoamingRatedSummaryRepository;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.service.TaxCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates roaming invoice/file generation after TAP file processing.
 *
 * TAP IN  → aggregate totalOurCharge from RoamingRatedSummary, log invoice total.
 *           (Phase 6 TODO: persist RoamingInvoice entity + PDF/email delivery)
 *
 * TAP OUT → delegate to TapOutFileGenerationService which:
 *           1. Queries rated_cdr by partner's outgoing_account_id + date range
 *           2. Encodes CDRs into GSMA TAP ASN file via TapFiles.write()
 *           3. Saves TapFileRecord (TAP_OUT, INVOICED) for audit trail
 *
 * Called by:
 *   - TapFileController POST /roaming/tap/files/{id}/invoice (manual trigger)
 *   - TapOutScheduler (daily cron for TAP OUT)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoamingInvoiceService {

    private final RoamingRatedSummaryRepository summaryRepository;
    private final RoamingCdrRepository roamingCdrRepository;
    private final TapFileRecordRepository tapFileRecordRepository;

    /**
     * TapOutFileGenerationService handles TAP OUT file creation from rated_cdr.
     * Injected here to delegate the TAP OUT path.
     */
    private final TapOutFileGenerationService tapOutFileGenerationService;
    private final TaxCalculationService taxCalculationService;
    private final AgreementTaxConfigRepository agreementTaxConfigRepository;
    private final AgreementRepository agreementRepository;

    @Transactional
    public void generateInvoice(TapFileRecord tapFileRecord) {
        List<RoamingRatedSummary> summaries =
                summaryRepository.findByTapFileRecord_TapFileIdAndIsInvoicedFalse(
                        tapFileRecord.getTapFileId());

        if (summaries.isEmpty()) {
            log.warn("No uninvoiced summaries for TAP file {}", tapFileRecord.getFileName());
            return;
        }

        if (tapFileRecord.getFileType().name().startsWith("TAP_IN")) {
            generateTapInInvoice(tapFileRecord, summaries);
        } else {
            boolean generated = generateTapOutFile(tapFileRecord);
            if (!generated) {
                log.error("TAP OUT file generation failed for {}, skipping invoice status update",
                        tapFileRecord.getFileName());
                return;
            }
        }

        summaries.forEach(s -> s.setInvoiced(true));
        summaryRepository.saveAll(summaries);

        tapFileRecord.setStatus(TapFileStatus.INVOICED);
        tapFileRecordRepository.save(tapFileRecord);

        log.info("Roaming invoice completed for TAP file {} ({})",
                tapFileRecord.getFileName(), tapFileRecord.getFileType());
    }

    // ── TAP IN: bill the Home Network partner ───────────────────────────────────

    /**
     * Generate TAP IN invoice — aggregate our charges and log the total.
     *
     * We are the Visited Network. We charge the Home Network (partner) for
     * their subscriber's usage on our network.
     *
     * totalOurCharge = sum of RoamingRatedSummary.totalOurCharge
     *                = calculated via zone-based RatePackage billing
     *
     * Phase 6 TODO: persist RoamingInvoice entity + trigger PDF/email delivery to partner.
     */
    private void generateTapInInvoice(TapFileRecord tapFileRecord,
                                       List<RoamingRatedSummary> summaries) {
        BigDecimal totalOurCharge = summaries.stream()
                .map(s -> s.getTotalOurCharge() != null ? s.getTotalOurCharge() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String currency = summaries.get(0).getCurrency();

        // Apply tax if a ROAMING agreement exists for this partner
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal totalWithTax = totalOurCharge;
        try {
            Agreement agreement = agreementRepository
                    .findActiveRoamingAgreementByPartnerId(tapFileRecord.getPartner().getPartnerId())
                    .orElse(null);
            if (agreement != null && !Boolean.TRUE.equals(agreement.getIsTaxExempt())) {
                List<AgreementTaxConfig> taxConfigs = agreementTaxConfigRepository
                        .findByAgreement_AgreementIdOrderByApplyOrderAsc(agreement.getAgreementId());
                if (!taxConfigs.isEmpty()) {
                    TaxCalculationService.MultiTaxCalculationResult taxResult =
                            taxCalculationService.calculateMultiTax(totalOurCharge, taxConfigs, LocalDate.now());
                    taxAmount = taxResult.getTotalTax();
                    totalWithTax = taxResult.getTotalInvoiceAmount();
                }
            }
        } catch (Exception e) {
            log.warn("Tax calculation failed for TAP IN invoice {}: {}, proceeding without tax",
                    tapFileRecord.getFileName(), e.getMessage());
        }

        log.info("TAP IN invoice: partner={} file={} charge={} tax={} total={} {}",
                tapFileRecord.getPartner() != null
                        ? tapFileRecord.getPartner().getPartnerCode() : "unknown",
                tapFileRecord.getFileName(), totalOurCharge, taxAmount, totalWithTax, currency);

        // TODO Phase 6: persist a RoamingInvoice entity and trigger PDF/email delivery to partner
    }

    // ── TAP OUT: generate ASN file from rated_cdr ───────────────────────────────

    /**
     * Generate TAP OUT ASN file from our rated_cdr records.
     *
     * We are the Home Network. The Visited Network (partner) charged us.
     * We generate a TAP OUT file confirming the charges from OUR rated_cdr perspective.
     *
     * The date window is derived from the TAP file's processedAt timestamp:
     *   startTime = processedAt date at 00:00:00
     *   endTime   = processedAt date at 23:59:59
     *
     * Delegates to TapOutFileGenerationService which:
     *   1. Queries rated_cdr by partner's outgoing_account_id + date range
     *   2. Encodes each VoiceRatedCdr → MobileOriginatedCall TAP record
     *   3. Writes GSMA ASN file to roaming.tap.outbox-dir
     *   4. Saves TapFileRecord (TAP_OUT, INVOICED) for audit trail
     *
     * @param tapFileRecord the incoming TAP file record (TAP_OUT direction)
     */
    private boolean generateTapOutFile(TapFileRecord tapFileRecord) {
        if (tapFileRecord.getPartner() == null) {
            log.error("TAP OUT generation skipped: no partner on TAP file {}",
                    tapFileRecord.getFileName());
            return false;
        }

        LocalDateTime processedAt = tapFileRecord.getProcessedAt() != null
                ? tapFileRecord.getProcessedAt()
                : LocalDateTime.now();

        LocalDateTime startTime = processedAt.toLocalDate().atStartOfDay();
        LocalDateTime endTime = processedAt.toLocalDate().atTime(23, 59, 59);

        log.info("TAP OUT file generation: partner={} file={} window={} to {}",
                tapFileRecord.getPartner().getPartnerCode(),
                tapFileRecord.getFileName(), startTime, endTime);

        try {
            tapOutFileGenerationService.generateForPartner(
                    tapFileRecord.getPartner(), startTime, endTime);
            return true;
        } catch (Exception e) {
            log.error("TAP OUT generation threw exception for file {}: {}",
                    tapFileRecord.getFileName(), e.getMessage(), e);
            return false;
        }
    }
}
