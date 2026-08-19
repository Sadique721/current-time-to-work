package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.entity.TapDirection;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.tapmodels.tap.DataInterChange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the full TAP file processing pipeline:
 *   RECEIVED → DECODING → DECODED → RATING → RATED
 *
 * Invoice generation is triggered separately via RoamingInvoiceService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TapFileProcessingService {

    private final TapFileRecordRepository tapFileRecordRepository;
    private final TapFileDecoderService decoderService;
    private final TapCdrExtractorService extractorService;
    private final RoamingRatingService ratingService;
    private final RoamingRatedSummaryService summaryService;
    private final PartnerRepository partnerRepository;

    @Transactional
    public void process(TapFileRecord record) {
        try {
            record.setStatus(TapFileStatus.DECODING);
            tapFileRecordRepository.save(record);

            DataInterChange dic = decoderService.decode(record);
            if (dic == null || record.getStatus() == TapFileStatus.ERROR) return;

            record.setStatus(TapFileStatus.DECODED);
            tapFileRecordRepository.save(record);

            Partner partner = record.getPartner();
            if (partner == null) throw new IllegalStateException("TapFileRecord has no partner assigned");

            // Re-fetch partner with full profile and field mappings eagerly loaded
            // within this @Transactional session to avoid LazyInitializationException
            final Long partnerId = partner.getPartnerId();
            Partner enrichedPartner = partnerRepository.findRoamingPartnersWithFullProfile().stream()
                    .filter(p -> p.getPartnerId().equals(partnerId))
                    .findFirst()
                    .orElse(partner);

            TapDirection direction = record.getFileType().name().startsWith("TAP_IN")
                    ? TapDirection.TAP_IN : TapDirection.TAP_OUT;

            int decimalPlaces = record.getTapDecimalPlaces() != null
                    ? Integer.parseInt(String.valueOf(record.getTapDecimalPlaces())) : 0;

            var tapCdrs = extractorService.extract(dic, direction, decimalPlaces, enrichedPartner);

            record.setStatus(TapFileStatus.RATING);
            tapFileRecordRepository.save(record);

            ratingService.rateCdrs(record, tapCdrs);

            record.setStatus(TapFileStatus.RATED);
            tapFileRecordRepository.save(record);

            summaryService.summarize(record);

        } catch (Exception e) {
            log.error("TAP processing failed for {}: {}", record.getFileName(), e.getMessage(), e);
            record.setStatus(TapFileStatus.ERROR);
            record.setErrorReason(e.getMessage());
            tapFileRecordRepository.save(record);
        }
    }
}
