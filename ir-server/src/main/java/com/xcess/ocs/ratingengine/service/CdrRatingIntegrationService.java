package com.xcess.ocs.ratingengine.service;

import com.xcess.ocs.dto.SmsRatedCdrDTO;
import com.xcess.ocs.dto.VoiceRatedCdrDTO;
import com.xcess.ocs.dto.UsageRatedCdrDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.ProductPlanAssociation;
import com.xcess.ocs.entity.RateableCdr;
import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.repository.ProductPlanAssociationRepository;
import com.xcess.ocs.repository.RatePackageAssociationRepository;
import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.RoamingCdr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service responsible for integrating CDR processing with the rating engine.
 * This service acts as the bridge between Kafka CDR data and the RadixTrie-based
 * rating algorithm, handling the complete flow from CDR receipt to rating application.
 * Supports both INTERCONNECT and ROAMING line of business.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrRatingIntegrationService {
    
    private final RateLookupService rateLookupService;
    private final AccountRateService accountRateService;
    private final AccountRepository accountRepository;
    private final PartnerRepository partnerRepository;
    private final ProductPlanAssociationRepository productPlanAssociationRepository;
    private final RatePackageAssociationRepository ratePackageAssociationRepository;
    private final ZoneLookupService zoneLookupService;
    private final CdrValidationService cdrValidationService;

    @Value("${ocs.rounding.price-precision:4}")
    private int pricePrecision;

    @Value("${ocs.rounding.duration-precision:2}")
    private int durationPrecision;
    

    
    /**
     * Process and rate a CDR from Kafka before database storage.
     * This method implements the complete flow:
     * 1. Create entity from DTO
     * 2. Apply rating based on Line of Business
     * 3. Set rating information on the CDR
     * 
     * @param cdrDto The CDR data from Kafka
     * @param sourceId The source ID for this CDR
     * @param lob The Line of Business (INTERCONNECT or ROAMING)
     * @return Enhanced VoiceRatedCdr entity with rating information applied
     */
    @Transactional(readOnly = true)
    public VoiceRatedCdr processAndRateCdr(VoiceRatedCdrDTO cdrDto, Long sourceId, LineOfBusiness lob) {
        log.debug("Processing CDR for rating: calling={}, called={}, sourceId={}, LOB={}", 
                cdrDto.getCallingNumber(), cdrDto.getCalledNumber(), sourceId, lob);

        VoiceRatedCdr voiceRatedCdr = createRatedCdrFromDto(cdrDto, sourceId);

        applyRating(voiceRatedCdr, cdrDto.getServiceType(), cdrDto.getCallType());
        
        return voiceRatedCdr;
    }

    /**
     * Process and rate an SMS CDR from Kafka before database storage.
     * This method implements the complete flow:
     * 1. Create entity from DTO
     * 2. Apply rating based on Line of Business
     * 3. Set rating information on the CDR
     * 
     * @param cdrDto The SMS CDR data from Kafka
     * @param sourceId The source ID for this CDR
     * @param lob The Line of Business (INTERCONNECT or ROAMING)
     * @return Enhanced SmsRatedCdr entity with rating information applied
     */
    @Transactional(readOnly = true)
    public SmsRatedCdr processAndRateSmsCdr(SmsRatedCdrDTO cdrDto, Long sourceId, LineOfBusiness lob) {
        log.debug("Processing SMS CDR for rating: calling={}, called={}, sourceId={}, LOB={}", 
                cdrDto.getCallingNumber(), cdrDto.getCalledNumber(), sourceId, lob);

        SmsRatedCdr smsRatedCdr = createSmsRatedCdrFromDto(cdrDto, sourceId);

        applyRating(smsRatedCdr, cdrDto.getServiceType(), cdrDto.getCallType());
        
        return smsRatedCdr;
    }

    /**
     * Process and rate a USAGE CDR from Kafka before database storage.
     */
    @Transactional(readOnly = true)
    public UsageRatedCdr processAndRateUsageCdr(UsageRatedCdrDTO cdrDto, Long sourceId, LineOfBusiness lob) {
        log.debug("Processing USAGE CDR for rating: subscriber={}, APN={}, sourceId={}, LOB={}", 
                cdrDto.getSubscriberIdentity(), cdrDto.getAccessPointName(), sourceId, lob);

        UsageRatedCdr usageRatedCdr = createUsageRatedCdrFromDto(cdrDto, sourceId);

        applyRating(usageRatedCdr, cdrDto.getServiceType(), null);
        
        return usageRatedCdr;
    }
    
    /**
     * Create a VoiceRatedCdr entity from the Kafka DTO.
     * This method maps fields from the DTO to the entity for both INTERCONNECT and ROAMING.
     * 
     * @param cdrDto The CDR data from Kafka
     * @param sourceId The source ID for this CDR
     * @return A new VoiceRatedCdr entity
     */
    private VoiceRatedCdr createRatedCdrFromDto(VoiceRatedCdrDTO cdrDto, Long sourceId) {
        VoiceRatedCdr voiceRatedCdr = new VoiceRatedCdr();

        voiceRatedCdr.setCallingNumber(cdrDto.getCallingNumber());
        voiceRatedCdr.setCalledNumber(cdrDto.getCalledNumber());
        // start_time and end_time are now LocalDateTime — parse once here
        voiceRatedCdr.setStartTime(parseTimestamp(cdrDto.getStartTime()));
        voiceRatedCdr.setEndTime(parseTimestamp(cdrDto.getEndTime()));
        voiceRatedCdr.setSourceId(sourceId);
        voiceRatedCdr.setIncomingAccountId(cdrDto.getIncomingAccountId());
        voiceRatedCdr.setOutgoingAccountId(cdrDto.getOutgoingAccountId());
        voiceRatedCdr.setHomePlmn(cdrDto.getHomePlmn());
        voiceRatedCdr.setVisitedPlmn(cdrDto.getVisitedPlmn());
        voiceRatedCdr.setServiceType(cdrDto.getServiceType());
        voiceRatedCdr.setCallType(cdrDto.getCallType());
        voiceRatedCdr.setMessageCount(cdrDto.getEventNos() != null ? Integer.valueOf(cdrDto.getEventNos()) : 0);
        voiceRatedCdr.setLineOfBusiness(cdrDto.getLineOfBusiness());

        // Calculate duration for VOICE only — SMS uses event count, USAGE uses data volume
        if (cdrDto.getServiceType() != ServiceType.SMS && cdrDto.getServiceType() != ServiceType.USAGE) {
            calculateAndSetDuration(voiceRatedCdr);
        }

        voiceRatedCdr.markAsPending();
        return voiceRatedCdr;
    }

    /**
     * Create an SmsRatedCdr entity from the Kafka DTO.
     * This method maps fields from the DTO to the entity for both INTERCONNECT and ROAMING.
     * 
     * @param cdrDto The SMS CDR data from Kafka
     * @param sourceId The source ID for this CDR
     * @return A new SmsRatedCdr entity
     */
    private SmsRatedCdr createSmsRatedCdrFromDto(SmsRatedCdrDTO cdrDto, Long sourceId) {
        SmsRatedCdr smsRatedCdr = new SmsRatedCdr();

        smsRatedCdr.setCallingNumber(cdrDto.getCallingNumber());
        smsRatedCdr.setCalledNumber(cdrDto.getCalledNumber());
        // start_time is now LocalDateTime
        smsRatedCdr.setStartTime(parseTimestamp(cdrDto.getStartTime()));
        smsRatedCdr.setEndTime(parseTimestamp(cdrDto.getEndTime()));
        smsRatedCdr.setSourceId(sourceId);
        
        // Map INTERCONNECT-specific fields
        smsRatedCdr.setIncomingAccountId(cdrDto.getIncomingAccountId());
        smsRatedCdr.setOutgoingAccountId(cdrDto.getOutgoingAccountId());
        
        // Map ROAMING-specific fields
        smsRatedCdr.setHomePlmn(cdrDto.getHomePlmn());
        smsRatedCdr.setVisitedPlmn(cdrDto.getVisitedPlmn());
        smsRatedCdr.setServiceType(cdrDto.getServiceType());
        smsRatedCdr.setCallType(cdrDto.getCallType());

        if (cdrDto.getEventNos() != null) {
            try {
                smsRatedCdr.setEventNos(Integer.valueOf(cdrDto.getEventNos()));
            } catch (NumberFormatException e) {
                log.warn("Invalid EventNos format: {}", cdrDto.getEventNos());
            }
        }

        // Map Line of Business
        smsRatedCdr.setLineOfBusiness(cdrDto.getLineOfBusiness());

        // Set initial rating status
        smsRatedCdr.markAsPending();
        
        return smsRatedCdr;
    }

    private UsageRatedCdr createUsageRatedCdrFromDto(UsageRatedCdrDTO cdrDto, Long sourceId) {
        UsageRatedCdr usageRatedCdr = new UsageRatedCdr();
        
        usageRatedCdr.setSubscriberIdentity(cdrDto.getSubscriberIdentity());
        usageRatedCdr.setAccessPointName(cdrDto.getAccessPointName());
        // start_time is now LocalDateTime
        usageRatedCdr.setStartTime(parseTimestamp(cdrDto.getStartTime()));
        usageRatedCdr.setEndTime(parseTimestamp(cdrDto.getEndTime()));
        usageRatedCdr.setSourceId(sourceId);
        
        usageRatedCdr.setIncomingAccountId(cdrDto.getIncomingAccountId());
        usageRatedCdr.setOutgoingAccountId(cdrDto.getOutgoingAccountId());
        
        usageRatedCdr.setHomePlmn(cdrDto.getHomePlmn());
        usageRatedCdr.setVisitedPlmn(cdrDto.getVisitedPlmn());
        usageRatedCdr.setServiceType(cdrDto.getServiceType());

        if (cdrDto.getTotalUsage() != null) {
            BigDecimal totalUsage = BigDecimal.valueOf(cdrDto.getTotalUsage());
            usageRatedCdr.setTotalUsage(totalUsage);
            if (cdrDto.getMeasurementUnit() != null) {
                usageRatedCdr.setTotalUsageBytes(normalizeDataVolume(totalUsage, cdrDto.getMeasurementUnit(), "BYTE").longValue());
            } else {
                usageRatedCdr.setTotalUsageBytes(totalUsage.longValue());
            }
        }
        if (cdrDto.getUploadUsage() != null) {
            usageRatedCdr.setUploadUsage(BigDecimal.valueOf(cdrDto.getUploadUsage()));
        }
        if (cdrDto.getDownloadUsage() != null) {
            usageRatedCdr.setDownloadUsage(BigDecimal.valueOf(cdrDto.getDownloadUsage()));
        }
        usageRatedCdr.setMeasurementUnit(cdrDto.getMeasurementUnit());
        
        usageRatedCdr.setLineOfBusiness(cdrDto.getLineOfBusiness());
        usageRatedCdr.markAsPending();
        
        return usageRatedCdr;
    }
    
    /**
     * Apply unified rating to the CDR regardless of Line of Business.
     * This handles INTERCONNECT and ROAMING by resolving the rate package 
     * through either the account ID or home PLMN fallback, then applying
     * zone-based, source-based, or source_destination-based rating.
     * 
     * @param cdr The CDR to apply rating to
     * @param serviceType The service type of the CDR
     * @param callType The call type of the CDR (can be null for USAGE)
     */
    public void applyRating(RateableCdr cdr, ServiceType serviceType, CallType callType) {
        try {
            String validationError = cdrValidationService.validateCdr(cdr, serviceType);
            if (validationError != null) {
                markIncomingAsFailed(cdr, validationError);
                markOutgoingAsFailed(cdr, validationError);
                return;
            }

            LocalDateTime callTimestamp = cdr.getStartTime();

            rateWithAccount(cdr, cdr.getIncomingAccountId(), callTimestamp, serviceType, callType, true);
            rateWithAccount(cdr, cdr.getOutgoingAccountId(), callTimestamp, serviceType, callType, false);

            cdr.setRatedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Failed to rate CDR: calling={}, called={}, error={}",
                    cdr.getCallingNumber(), cdr.getCalledNumber(), e.getMessage(), e);
            markIncomingAsFailed(cdr, "RATING_ERROR");
            markOutgoingAsFailed(cdr, "RATING_ERROR");
        }
    }

    private RatePackage getRatePackage(RateableCdr cdr, String accountId, LocalDateTime callTimestamp, ServiceType serviceType, CallType callType) {
        // --- Step 1: Fetch Account with ProductPlan ---
        Account account = null;
        if (accountId != null && !accountId.isEmpty() && !accountId.equalsIgnoreCase("NULL")) {
            account = accountRepository.findByAccountCodeWithProductPlan(accountId).orElse(null);
        } else if (cdr.getPartnerPlmnForRating() != null && !cdr.getPartnerPlmnForRating().isEmpty()) {
            String partnerPlmn = cdr.getPartnerPlmnForRating();
            Partner partner = partnerRepository.findByHplmnAndIsDeletedFalse(partnerPlmn).orElse(null);
            if (partner != null) {
                List<Account> accounts = accountRepository.findByPartnerPartnerIdWithProductPlan(partner.getPartnerId());
                account = accounts.stream()
                        .filter(a -> a.getAccountCode().equalsIgnoreCase(partnerPlmn))
                        .findFirst().orElse(null);
            }
        }
        
        if (account == null || account.getProductPlan() == null) {
            return null;
        }
        
        // --- Step 2: Fetch ProductPlanAssociations with RatePackageGroup ---
        List<ProductPlanAssociation> ppAssociations = productPlanAssociationRepository
                .findByProductPlanIdWithGroup(account.getProductPlan().getProductPlanId());
        
        if (ppAssociations == null || ppAssociations.isEmpty()) return null;
        
        // --- Step 3: Find matching RatePackageGroup ---
        RatePackageGroup rpg = ppAssociations.stream()
                .filter(a -> a.getRatePackageGroup() != null)
                .filter(a -> serviceType == a.getRatePackageGroup().getServiceType())
                .findFirst()
                .map(ProductPlanAssociation::getRatePackageGroup)
                .orElse(null);
                
        if (rpg == null) return null;

        // --- Query 2: Load RatePackageAssociations → RatePackage for the matched group ---
        List<RatePackageAssociation> associations = ratePackageAssociationRepository
                .findByGroupIdWithRatePackage(rpg.getRatePackageGroupId());
        
        if (associations == null || associations.isEmpty()) return null;
        
        return associations.stream()
                .filter(a -> {
                    if (callTimestamp != null && a.getStartTime() != null && callTimestamp.isBefore(a.getStartTime())) return false;
                    if (callTimestamp != null && a.getEndTime() != null && callTimestamp.isAfter(a.getEndTime())) return false;
                    
                    if (a.getExpression() != null && !a.getExpression().trim().isEmpty()) {
                        return isExpressionMatch(a, cdr);
                    }
                    if (a.getCallType() != null) {
                        return callType != null && callType.equals(a.getCallType());
                    }
                    return true;
                })
                .findFirst()
                .map(RatePackageAssociation::getRatePackage)
                .orElse(null);
    }

    /**
     * Rate using resolved rate package and apply zone based, source based, or source_destination based rating.
     */
    private void rateWithAccount(RateableCdr voiceRatedCdr, String accountId, LocalDateTime callTimestamp,
                                 ServiceType serviceType, CallType callType, boolean isIncoming) {
        if ((accountId == null || accountId.isEmpty()) && (voiceRatedCdr.getPartnerPlmnForRating() == null || voiceRatedCdr.getPartnerPlmnForRating().isEmpty())) {
            if (isIncoming) markIncomingAsFailed(voiceRatedCdr, "NULL_ACCOUNT_ID_AND_PLMN");
            else markOutgoingAsFailed(voiceRatedCdr, "NULL_ACCOUNT_ID_AND_PLMN");
            return;
        }

        RatePackage ratePackage = getRatePackage(voiceRatedCdr, accountId, callTimestamp, serviceType, callType);
        if (ratePackage == null) {
            if (isIncoming) markIncomingAsUnrated(voiceRatedCdr, "NO_RATE_PACKAGE");
            else markOutgoingAsUnrated(voiceRatedCdr, "NO_RATE_PACKAGE");
            return;
        }

        RateDetails bestRate = null;
        boolean isSourceDestMatch = false;
        // Holds the zone name when zone-based (ZonePrefixTrie) rating path is taken.
        // Stays null when source/destination prefix rating or package-level rate is used.
        String resolvedZoneName = null;

        if (ServiceType.USAGE.equals(voiceRatedCdr.getServiceType())) {
            if (ratePackage.getRate() != null) {
                boolean timeValid = true;
                if (ratePackage.getStartTime() != null && callTimestamp.isBefore(ratePackage.getStartTime())) timeValid = false;
                if (ratePackage.getEndTime() != null && callTimestamp.isAfter(ratePackage.getEndTime())) timeValid = false;

                if (timeValid) {
                    bestRate = new RateDetails();
                    bestRate.setRate(ratePackage.getRate());
                }
            }
        } else {
            // MERGED RATING: 1. Try source/destination prefix-based (only for non-ZONE packages)
            if (ratePackage.getRatePackageType() != RatePackageType.ZONE_DESTINATION_BASED) {
                bestRate = rateLookupService.findBestRate(
                        ratePackage.getRatePackageId(),
                        voiceRatedCdr.getCallingNumber(),
                        voiceRatedCdr.getCalledNumber(),
                        callTimestamp
                );

                if (bestRate != null) {
                    isSourceDestMatch = bestRate.getSourcePrefix() != null && !bestRate.getSourcePrefix().trim().isEmpty();
                }
            }

            if (bestRate == null) {
                // 2. Fallback (or primary for ZONE packages): zone-based rating via ZonePrefixTrie.
                // Zone always resolves on calledNumber — the zone represents the call destination.
                String lookupPrefix = voiceRatedCdr.getCalledNumber();

                if (lookupPrefix != null && !lookupPrefix.isBlank()) {
                    String zoneName = zoneLookupService.getZoneForNumber(ratePackage.getRatePackageId(), lookupPrefix);
                    if (zoneName != null) {
                        bestRate = ratePackage.getRateDetails().stream()
                                .filter(rd -> zoneName.equalsIgnoreCase(rd.getZoneName()))
                                .filter(rd -> callTimestamp == null || rd.getStartTime() == null || !callTimestamp.isBefore(rd.getStartTime()))
                                .filter(rd -> callTimestamp == null || rd.getEndTime() == null || !callTimestamp.isAfter(rd.getEndTime()))
                                .findFirst()
                                .orElse(null);
                        if (bestRate != null) {
                            resolvedZoneName = zoneName;
                        }
                    }
                }
            }
        }

        if (bestRate != null) {
            BigDecimal totalCost;
            if (serviceType == ServiceType.SMS) {
                totalCost = calculateSmsCost(voiceRatedCdr.getEventCountForRating(), bestRate, ratePackage);
            } else if (serviceType == ServiceType.USAGE) {
                totalCost = calculateUsageCost(voiceRatedCdr.getUsageAmountForRating(), voiceRatedCdr.getMeasurementUnitForRating(), bestRate, ratePackage);
            } else {
                totalCost = calculateTotalCost(voiceRatedCdr.getDurationForRating(), bestRate, ratePackage);
            }

            if (isIncoming) {
                voiceRatedCdr.markIncomingAsRated(
                        BigDecimal.valueOf(bestRate.getRate()),
                        ratePackage.getRatePackageId(),
                        ratePackage.getPackageName(),
                        bestRate.getRateDetailsId(),
                        bestRate.getSourcePrefix(),
                        bestRate.getDestinationPrefix(),
                        isSourceDestMatch,
                        totalCost
                );
            } else {
                voiceRatedCdr.markOutgoingAsRated(
                        BigDecimal.valueOf(bestRate.getRate()),
                        ratePackage.getRatePackageId(),
                        ratePackage.getPackageName(),
                        bestRate.getRateDetailsId(),
                        bestRate.getSourcePrefix(),
                        bestRate.getDestinationPrefix(),
                        isSourceDestMatch,
                        totalCost
                );
            }

            // Set zone name on the CDR only when zone-based rating was the actual path taken.
            // For prefix-based or package-level rating, zoneName remains null.
            if (resolvedZoneName != null) {
                voiceRatedCdr.setZoneName(resolvedZoneName);
            }

            log.info("Successfully rated {} CDR: calling={}, called={}, zone={}, rate={}, package={}",
                    isIncoming ? "INCOMING" : "OUTGOING",
                    voiceRatedCdr.getCallingNumber(),
                    voiceRatedCdr.getCalledNumber(),
                    resolvedZoneName != null ? resolvedZoneName : "prefix-based",
                    bestRate.getRate(),
                    ratePackage.getPackageName());
        } else {
            if (isIncoming) {
                markIncomingAsUnrated(voiceRatedCdr, "NO_MATCHING_RATE");
            } else {
                markOutgoingAsUnrated(voiceRatedCdr, "NO_MATCHING_RATE");
            }
        }
    }

    private BigDecimal calculateSmsCost(Integer messageCount, RateDetails rateDetails, RatePackage ratePackage) {
        if (messageCount == null || messageCount <= 0 || rateDetails == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal cost = BigDecimal.valueOf(rateDetails.getRate()).multiply(BigDecimal.valueOf(messageCount));
        return applyPriceRounding(cost, ratePackage.getPriceRounding());
    }

    private BigDecimal calculateUsageCost(BigDecimal usageAmount, String cdrUnit, RateDetails rateDetails, RatePackage ratePackage) {
        if (usageAmount == null || usageAmount.compareTo(BigDecimal.ZERO) <= 0 || rateDetails == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal normalizedUsage = usageAmount;
        if (ratePackage.getPulse() != null && ratePackage.getPulse().getUnit() != null) {
            normalizedUsage = normalizeDataVolume(usageAmount, cdrUnit, ratePackage.getPulse().getUnit().name());
        }

        BigDecimal cost = BigDecimal.valueOf(rateDetails.getRate()).multiply(normalizedUsage);
        return applyPriceRounding(cost, ratePackage.getPriceRounding());
    }

    private BigDecimal normalizeDataVolume(BigDecimal volume, String fromUnit, String toUnit) {
        if (fromUnit == null || toUnit == null || fromUnit.equalsIgnoreCase(toUnit)) {
            return volume;
        }

        // Convert to BYTES first
        BigDecimal inBytes = volume;
        switch (fromUnit.toUpperCase()) {
            case "KB" -> inBytes = volume.multiply(BigDecimal.valueOf(1024));
            case "MB" -> inBytes = volume.multiply(BigDecimal.valueOf(1024 * 1024));
            case "GB" -> inBytes = volume.multiply(BigDecimal.valueOf(1024 * 1024 * 1024));
        }

        // Convert from BYTES to target unit
        switch (toUnit.toUpperCase()) {
            case "KB" -> { return inBytes.divide(BigDecimal.valueOf(1024), 4, RoundingMode.CEILING); }
            case "MB" -> { return inBytes.divide(BigDecimal.valueOf(1024 * 1024), 4, RoundingMode.CEILING); }
            case "GB" -> { return inBytes.divide(BigDecimal.valueOf(1024 * 1024 * 1024), 4, RoundingMode.CEILING); }
            default -> { return inBytes; } // BYTE or unknown
        }
    }

    private BigDecimal calculateTotalCost(BigDecimal durationSeconds, RateDetails rateDetails, RatePackage ratePackage) {
        if (durationSeconds == null || rateDetails == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = BigDecimal.valueOf(rateDetails.getRate());
        Pulse pulse = ratePackage.getPulse();
        BigDecimal cost;

        if (pulse != null && pulse.getNoOfUnits() > 0) {
            // Convert duration to seconds based on pulse unit type
            BigDecimal durationInPulseUnits = convertDurationToPulseUnits(durationSeconds, pulse.getUnit());
            // Calculate billable units (using rounding from rate package)
            BigDecimal pulseUnits = BigDecimal.valueOf(pulse.getNoOfUnits());
            BigDecimal billableUnits = durationInPulseUnits.divide(pulseUnits, 0, getRoundingMode(ratePackage.getRounding()));
            cost = rate.multiply(billableUnits);
        } else {
            BigDecimal durationMinutes = convertDurationToPulseUnits(durationSeconds, UnitType.MINUTE);
            cost = rate.multiply(durationMinutes);
        }

        // Apply price rounding
        return applyPriceRounding(cost, ratePackage.getPriceRounding());
    }

    private BigDecimal convertDurationToPulseUnits(BigDecimal durationSeconds, UnitType unitType) {
        if (unitType == null) {
            return durationSeconds;
        }
        
        switch (unitType) {
            case MINUTE:
                return durationSeconds.divide(BigDecimal.valueOf(60), durationPrecision, BigDecimal.ROUND_HALF_UP);
            case SECOND:
                return durationSeconds;
            default:
                return durationSeconds;
        }
    }

    private BigDecimal applyPriceRounding(BigDecimal cost, Rounding rounding) {
        if (rounding == null || rounding == Rounding.DEFAULT) {
            return cost.setScale(pricePrecision, BigDecimal.ROUND_HALF_UP);
        }
        
        switch (rounding) {
            case UPPER:
                return cost.setScale(pricePrecision, BigDecimal.ROUND_UP);
            case LOWER:
                return cost.setScale(pricePrecision, BigDecimal.ROUND_DOWN);
            default:
                return cost.setScale(pricePrecision, BigDecimal.ROUND_HALF_UP);
        }
    }

    private int getRoundingMode(Rounding rounding) {
        if (rounding == null || rounding == Rounding.DEFAULT) {
            return BigDecimal.ROUND_HALF_UP;
        }
        
        switch (rounding) {
            case UPPER:
                return BigDecimal.ROUND_UP;
            case LOWER:
                return BigDecimal.ROUND_DOWN;
            default:
                return BigDecimal.ROUND_HALF_UP;
        }
    }
    

    /**
     * Calculate call duration in seconds from the CDR's LocalDateTime start/end and store it.
     * start_time and end_time are now proper LocalDateTime fields — no string parsing needed.
     */
    private void calculateAndSetDuration(VoiceRatedCdr voiceRatedCdr) {
        if (voiceRatedCdr.getStartTime() != null && voiceRatedCdr.getEndTime() != null) {
            long durationSeconds = ChronoUnit.SECONDS.between(
                    voiceRatedCdr.getStartTime(), voiceRatedCdr.getEndTime());
            voiceRatedCdr.setDurationSeconds(durationSeconds);
            log.debug("Duration: {}s — calling={}, called={}",
                    durationSeconds, voiceRatedCdr.getCallingNumber(), voiceRatedCdr.getCalledNumber());
        } else {
            log.warn("Cannot calculate duration — null start/end time: calling={}, called={}",
                    voiceRatedCdr.getCallingNumber(), voiceRatedCdr.getCalledNumber());
        }
    }

    /**
     * Parse a timestamp string into LocalDateTime using common formats.
     * Used only during DTO mapping before the value is stored as LocalDateTime.
     */
    private static final java.time.format.DateTimeFormatter[] TIMESTAMP_FORMATTERS = {
        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) return null;
        String clean = timestampStr.trim();
        for (java.time.format.DateTimeFormatter fmt : TIMESTAMP_FORMATTERS) {
            try { return LocalDateTime.parse(clean, fmt); }
            catch (java.time.format.DateTimeParseException ignored) {}
        }
        log.warn("Unable to parse timestamp: {}", timestampStr);
        return null;
    }

    private void markIncomingAsUnrated(RateableCdr voiceRatedCdr, String errorCode) {
        log.info("Incoming CDR marked as UNRATED: {}", errorCode);
        voiceRatedCdr.markIncomingAsUnrated(errorCode);
    }

    private void markIncomingAsFailed(RateableCdr voiceRatedCdr, String errorCode) {
        log.error("Incoming CDR marked as FAILED: {}", errorCode);
        voiceRatedCdr.markIncomingAsFailed(errorCode);
    }

    private void markOutgoingAsUnrated(RateableCdr voiceRatedCdr, String errorCode) {
        log.info("Outgoing CDR marked as UNRATED: {}", errorCode);
        voiceRatedCdr.markOutgoingAsUnrated(errorCode);
    }

    private void markOutgoingAsFailed(RateableCdr voiceRatedCdr, String errorCode) {
        log.error("Outgoing CDR marked as FAILED: {}", errorCode);
        voiceRatedCdr.markOutgoingAsFailed(errorCode);
    }

    private boolean isExpressionMatch(RatePackageAssociation association, RateableCdr cdr) {
        String expression = association.getExpression();
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }
        
        String[] parts = expression.split("=");
        if (parts.length != 2) {
            log.warn("Invalid expression format: {}", expression);
            return false;
        }
        
        String fieldName = parts[0].trim();
        String expectedValue = parts[1].trim();
        
        try {
            java.lang.reflect.Field field = null;
            Class<?> clazz = cdr.getClass();
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName);
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (field == null) {
                log.warn("Field {} not found in {}", fieldName, cdr.getClass().getName());
                return false;
            }
            
            field.setAccessible(true);
            Object valueObj = field.get(cdr);
            String actualValue = valueObj != null ? valueObj.toString() : "";
            
            return expectedValue.equalsIgnoreCase(actualValue);
        } catch (Exception e) {
            log.error("Error evaluating expression {} on CDR", expression, e);
            return false;
        }
    }
} 