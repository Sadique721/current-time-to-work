package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.*;
import com.xcess.ocs.ratingengine.service.CdrRatingIntegrationService;
import com.xcess.ocs.repository.ExchangeRateRepository;
import com.xcess.ocs.roaming.dto.TapCdrDTO;
import com.xcess.ocs.roaming.entity.*;
import com.xcess.ocs.roaming.repository.RoamingCdrRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Rates RoamingCdr records decoded from TAP files using zone-based RatePackage lookup.
 *
 * This service replaces the previous RoamingIotRate-based rating with the full
 * RatePackage infrastructure (same as INTERCONNECT), using ZONE-type packages.
 *
 * Rating flow per CDR:
 *   1. Map TapCdrDTO → RoamingCdr (PENDING status)
 *   2. Resolve partner's active Account (partner → accounts)
 *   3. AccountRateService.findRatePackageForAccount() traverses:
 *        Account → ProductPlan → RatePackageGroup → RatePackage[ZONE, ROAMING]
 *   4. ZoneLookupService.getZoneForNumber(groupId, visitedPlmn) → zoneName
 *   5. ZoneRateMappingRepository.findActiveByGroupIdAndZoneId() → ZoneRateMapping → RatePackage
 *   6. RateLookupService.findBestRate() → RateDetails (time-based rate lookup)
 *   7. calculateTotalCost() → pulse-based billing + price rounding (same as INTERCONNECT)
 *   8. Save RoamingCdr with rating results
 *
 * Failure reasons stored in ratingFailureReason:
 *   NO_ACCOUNT          — partner has no active account
 *   NO_RATE_PACKAGE     — no RatePackage found via account chain
 *   NO_ZONE_FOUND       — visitedPlmn did not match any zone in ZonePrefixTrie
 *   NO_ZONE_RATE_MAPPING — zone found but no active ZoneRateMapping for this group
 *   NO_MATCHING_RATE    — RatePackage found but no RateDetails matched call time
 *   RATING_ERROR        — unexpected exception during rating
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoamingRatingService {

    private final RoamingCdrRepository roamingCdrRepository;
    private final CdrRatingIntegrationService cdrRatingIntegrationService;
    private final ExchangeRateRepository exchangeRateRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rate all CDRs decoded from a TAP file.
     *
     * Each CDR is rated independently — a failure on one CDR does not affect others.
     * Results are persisted to roaming_cdrs table.
     *
     * @param tapFileRecord the TAP file record (contains partner reference)
     * @param cdrs          list of decoded CDR DTOs from TapCdrExtractorService
     */
    @Transactional
    public void rateCdrs(TapFileRecord tapFileRecord, List<TapCdrDTO> cdrs) {
        Partner partner = tapFileRecord.getPartner();

        if (partner == null) {
            log.error("TAP file {} has no partner — cannot rate CDRs", tapFileRecord.getFileName());
            return;
        }

        log.info("Rating {} CDRs for TAP file: {} partner: {}",
                cdrs.size(), tapFileRecord.getFileName(), partner.getPartnerCode());

        for (TapCdrDTO dto : cdrs) {
            RoamingCdr cdr = mapToCdr(dto, tapFileRecord);
            rateSingleCdr(cdr, tapFileRecord);
            roamingCdrRepository.save(cdr);
        }

        log.info("Completed rating for TAP file: {}", tapFileRecord.getFileName());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core rating logic
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rate a single RoamingCdr using the unified CdrRatingIntegrationService.
     *
     * @param cdr            the RoamingCdr to rate (status = PENDING)
     * @param tapFileRecord  original TAP file record reference
     */
    private void rateSingleCdr(RoamingCdr cdr, TapFileRecord tapFileRecord) {
        try {
            cdrRatingIntegrationService.applyRating(cdr, cdr.getServiceType(), cdr.getCallType());

            if (cdr.getRatingStatus() == RoamingRatingStatus.RATED && cdr.getOurCharge() != null) {
                BigDecimal convertedCharge = convertCurrency(cdr.getOurCharge(), cdr.getCurrency(),
                        tapFileRecord.getPartner().getBillingCurrency(), cdr.getCallStartTime());
                cdr.setOurCharge(convertedCharge);

                log.info("Rated RoamingCdr: visitedPlmn={}, callingNumber={}, zone={}, package={}, rate={}, convertedCharge={}",
                        cdr.getVisitedPlmn(), cdr.getCallingNumber(), cdr.getZoneName(), cdr.getRatePackageName(),
                        cdr.getAppliedRate(), convertedCharge);
            }
        } catch (Exception e) {
            log.error("Unexpected error rating RoamingCdr for TAP file {}: {}",
                    cdr.getTapFileRecord().getFileName(), e.getMessage(), e);
            cdr.markIncomingAsFailed("RATING_ERROR");
        }
    }

    private RoamingCdr mapToCdr(TapCdrDTO dto, TapFileRecord tapFileRecord) {
        RoamingCdr cdr = new RoamingCdr();
        cdr.setTapFileRecord(tapFileRecord);
        cdr.setTapDirection(dto.getTapDirection());
        cdr.setCallType(dto.getCallType());
        cdr.setImsi(dto.getImsi());
        cdr.setMsisdn(dto.getMsisdn()!=null?dto.getMsisdn():dto.getCallingNumber());
        cdr.setCallingNumber(dto.getCallingNumber());
        cdr.setCalledNumber(dto.getCalledNumber());
        cdr.setCallStartTime(dto.getCallStartTime());
        cdr.setCallDurationSec(dto.getCallDurationSec());
        cdr.setTotalUsage(dto.getTotalUsage());
        cdr.setEventNos(dto.getEventNos());
        cdr.setVisitedPlmn(dto.getVisitedPlmn());
        cdr.setHomePlmn(dto.getHomePlmn());
        cdr.setServiceType(dto.getServiceType());
        cdr.setTapCharge(dto.getTapCharge());
        cdr.setCurrency(dto.getCurrency() != null ? dto.getCurrency() : tapFileRecord.getLocalCurrency());
        cdr.setRatingStatus(RoamingRatingStatus.PENDING);
        return cdr;
    }

    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency,
                                        String toCurrency, java.time.LocalDateTime callTime) {
        if (amount == null) return BigDecimal.ZERO;
        if (fromCurrency == null || toCurrency == null || fromCurrency.equalsIgnoreCase(toCurrency))
            return amount;
        try {
            LocalDate rateDate = callTime != null ? callTime.toLocalDate() : LocalDate.now();
            // Try exact date first, then fall back to latest available rate
            List<ExchangeRate> rates = exchangeRateRepository
                    .findByBaseCurrencyAndValidFrom(fromCurrency.toUpperCase(), rateDate);
            if (rates.isEmpty()) {
                LocalDate maxDate = exchangeRateRepository
                        .findMaxValidFromByBaseCurrency(fromCurrency.toUpperCase());
                if (maxDate != null)
                    rates = exchangeRateRepository
                            .findByBaseCurrencyAndValidFrom(fromCurrency.toUpperCase(), maxDate);
            }
            return rates.stream()
                    .filter(r -> toCurrency.equalsIgnoreCase(r.getTargetCurrency()))
                    .findFirst()
                    .map(r -> amount.multiply(r.getRate()).setScale(6, RoundingMode.HALF_UP))
                    .orElseGet(() -> {
                        log.warn("No exchange rate found for {}->{}, using unconverted amount",
                                fromCurrency, toCurrency);
                        return amount;
                    });
        } catch (Exception e) {
            log.warn("Currency conversion failed {}->{}: {}, using unconverted amount",
                    fromCurrency, toCurrency, e.getMessage());
            return amount;
        }
    }

}
