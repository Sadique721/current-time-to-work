package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.constants.enums.SettlementType;
import com.xcess.ocs.dto.RatedSummaryDTO;
import com.xcess.ocs.entity.RatedSummary;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.repository.RatedSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RatedSummaryService {

    @Autowired
    private DynamicQueryService dynamicQueryService;

    @Autowired
    private RatedSummaryRepository ratedSummaryRepository;

    @Autowired
    private VoiceRatedCdrRepository voiceRatedCdrRepository;

    @Autowired
    private SmsRatedCdrRepository smsRatedCdrRepository;

    @Autowired
    private com.xcess.ocs.repository.UsageRatedCdrRepository usageRatedCdrRepository;

    @Autowired
    private CdrMarkSummarizedService cdrMarkSummarizedService;

    @Transactional(timeout = 600, propagation = Propagation.REQUIRES_NEW)
    public long generateDailySummaries(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("Generating summaries for CDR range: {} to {}", startTime, endTime);

        try {
            long unsummarizedVoiceCount = voiceRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);
            long unsummarizedSmsCount = smsRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);
            long unsummarizedUsageCount = usageRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);

            if (unsummarizedVoiceCount == 0 && unsummarizedSmsCount == 0 && unsummarizedUsageCount == 0) {
                log.warn("All CDRs already summarized for {} to {}", startTime, endTime);
                return 0L;
            }
            log.info("Found {} unsummarized Voice, {} SMS, {} Usage CDRs",
                    unsummarizedVoiceCount, unsummarizedSmsCount, unsummarizedUsageCount);

            Map<String, Object> params = new HashMap<>();
            params.put("startTime", startTime);
            params.put("endTime", endTime);
            params.put("ratingStatus", "RATED");
            params.put("isDeleted", false);
            params.put("isSummarized", false);
            params.put("lineOfBusiness", "INTERCONNECT");

            // Generate OUTGOING summaries then mark outgoing CDRs as summarized
            generateOutgoingSummaries(params);
            long outgoingMarked = cdrMarkSummarizedService.markOutgoingAsSummarized(startTime, endTime);
            log.info("Marked {} CDRs as summarized (OUTGOING side)", outgoingMarked);

            // Generate INCOMING summaries then mark incoming CDRs as summarized
            generateIncomingSummaries(params);
            long incomingMarked = cdrMarkSummarizedService.markIncomingAsSummarized(startTime, endTime);
            log.info("Marked {} CDRs as summarized (INCOMING side)", incomingMarked);

            long totalMarked = outgoingMarked + incomingMarked;
            log.info("Total CDRs marked as summarized: {}", totalMarked);
            return totalMarked;

        } catch (Exception e) {
            log.error("Failed to generate summaries: {}", e.getMessage(), e);
            throw e;
        }
    }

    public LocalDateTime getLastModifiedDateOfProcessedCdrs(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            LocalDateTime voiceLastModified = voiceRatedCdrRepository.findLastModifiedDateBetween(startTime, endTime);
            LocalDateTime smsLastModified = smsRatedCdrRepository.findLastModifiedDateBetween(startTime, endTime);
            LocalDateTime usageLastModified = usageRatedCdrRepository.findLastModifiedDateBetween(startTime, endTime);

            LocalDateTime lastModified = null;
            if (voiceLastModified != null) lastModified = voiceLastModified;
            if (smsLastModified != null && (lastModified == null || smsLastModified.isAfter(lastModified)))
                lastModified = smsLastModified;
            if (usageLastModified != null && (lastModified == null || usageLastModified.isAfter(lastModified)))
                lastModified = usageLastModified;

            log.info("Last modified date of processed CDRs: {}", lastModified);
            return lastModified;
        } catch (Exception e) {
            log.error("Failed to get last modified date: {}", e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public void generateOutgoingSummaries(Map<String, Object> params) {
        log.info("Generating OUTGOING summaries");

        List<Map<String, Object>> voiceResults = dynamicQueryService.executeQuery("rated-summary-outgoing-daily", params);
        saveSummaries(voiceResults, SettlementType.OUTGOING.label());

        List<Map<String, Object>> smsResults = dynamicQueryService.executeQuery("rated-summary-sms-outgoing-daily", params);
        saveSummaries(smsResults, SettlementType.OUTGOING.label());

        List<Map<String, Object>> usageResults = dynamicQueryService.executeQuery("rated-summary-usage-outgoing-daily", params);
        saveSummaries(usageResults, SettlementType.OUTGOING.label());

        log.info("Saved OUTGOING summaries — voice: {}, sms: {}, usage: {}",
                voiceResults.size(), smsResults.size(), usageResults.size());
    }

    @Transactional
    public void generateIncomingSummaries(Map<String, Object> params) {
        log.info("Generating INCOMING summaries");

        List<Map<String, Object>> voiceResults = dynamicQueryService.executeQuery("rated-summary-incoming-daily", params);
        saveSummaries(voiceResults, SettlementType.INCOMING.label());

        List<Map<String, Object>> smsResults = dynamicQueryService.executeQuery("rated-summary-sms-incoming-daily", params);
        saveSummaries(smsResults, SettlementType.INCOMING.label());

        List<Map<String, Object>> usageResults = dynamicQueryService.executeQuery("rated-summary-usage-incoming-daily", params);
        saveSummaries(usageResults, SettlementType.INCOMING.label());

        log.info("Saved INCOMING summaries — voice: {}, sms: {}, usage: {}",
                voiceResults.size(), smsResults.size(), usageResults.size());
    }

    @Transactional
    public void saveSummaries(List<Map<String, Object>> results, String direction) {
        if (results == null || results.isEmpty()) {
            log.warn("No {} results to store", direction);
            return;
        }

        int updated = 0, created = 0;

        for (Map<String, Object> row : results) {
            try {
                LocalDate summaryDate    = getLocalDate(row, "summary_date");
                String accountCode       = getString(row, "account_code");
                String serviceType       = getString(row, "service_type");
                String ratingType        = getString(row, "rating_type");
                String sourcePrefix      = getString(row, "source_prefix");
                String destinationPrefix = getString(row, "destination_prefix");
                String zoneName          = getString(row, "zone_name");
                BigDecimal appliedRate   = getBigDecimal(row, "applied_rate");

                Optional<RatedSummary> existing = ratedSummaryRepository.findExistingSummary(
                        summaryDate, accountCode, direction, serviceType, ratingType,
                        sourcePrefix, destinationPrefix, zoneName, appliedRate);

                RatedSummary summary;
                if (existing.isPresent()) {
                    summary = existing.get();
                    // Accumulate only the metric column relevant to this service type
                    if (ServiceType.VOICE.name().equals(serviceType)) {
                        summary.setTotalCalls(nullSafeAdd(summary.getTotalCalls(), getLong(row, "total_calls")));
                        summary.setTotalDuration(nullSafeAdd(summary.getTotalDuration(), getBigDecimal(row, "total_duration")));
                    } else if (ServiceType.SMS.name().equals(serviceType)) {
                        summary.setTotalSms(nullSafeAdd(summary.getTotalSms(), getLong(row, "total_sms")));
                    } else if (ServiceType.USAGE.name().equals(serviceType)) {
                        summary.setTotalSessions(nullSafeAdd(summary.getTotalSessions(), getLong(row, "total_sessions")));
                        summary.setTotalDataVolume(nullSafeAdd(summary.getTotalDataVolume(), getBigDecimal(row, "total_data_volume")));
                    }
                    summary.setTotalCharge(summary.getTotalCharge().add(getBigDecimal(row, "total_charge")));
                    updated++;
                } else {
                    summary = new RatedSummary();
                    summary.setSummaryDate(summaryDate);
                    summary.setAccountCode(accountCode);
                    summary.setPartnerId(getLong(row, "partner_id"));
                    summary.setPartnerName(getString(row, "partner_name"));
                    summary.setSourcePrefix(sourcePrefix);
                    summary.setSourcePrefixName(getString(row, "source_prefix_name"));
                    summary.setSourceCountryCode(getString(row, "source_country_code"));
                    summary.setSourceCountryName(getString(row, "source_country_name"));
                    summary.setDestinationPrefix(destinationPrefix);
                    summary.setDestinationPrefixName(getString(row, "destination_prefix_name"));
                    summary.setDestinationCountryCode(getString(row, "destination_country_code"));
                    summary.setDestinationCountryName(getString(row, "destination_country_name"));
                    summary.setZoneName(zoneName);
                    summary.setRatingType(ratingType);
                    summary.setServiceType(serviceType);
                    summary.setDirection(direction);
                    summary.setAppliedRate(appliedRate);
                    summary.setTotalCharge(getBigDecimal(row, "total_charge"));

                    if (ServiceType.VOICE.name().equals(serviceType)) {
                        summary.setTotalCalls(getLong(row, "total_calls"));
                        summary.setTotalDuration(getBigDecimal(row, "total_duration"));
                    } else if (ServiceType.SMS.name().equals(serviceType)) {
                        summary.setTotalSms(getLong(row, "total_sms"));
                    } else if (ServiceType.USAGE.name().equals(serviceType)) {
                        summary.setTotalSessions(getLong(row, "total_sessions"));
                        summary.setTotalDataVolume(getBigDecimal(row, "total_data_volume"));
                    }
                    created++;
                }

                ratedSummaryRepository.save(summary);

            } catch (Exception e) {
                log.error("Failed to save {} summary row: {}", direction, e.getMessage(), e);
                throw new RuntimeException("Failed to save " + direction + " summary row", e);
            }
        }

        log.info("{} summaries: {} created, {} updated", direction, created, updated);
    }

    // ── Helper: null-safe accumulation ──────────────────────────────────────────

    private Long nullSafeAdd(Long existing, Long delta) {
        if (delta == null) return existing;
        return (existing == null ? 0L : existing) + delta;
    }

    private BigDecimal nullSafeAdd(BigDecimal existing, BigDecimal delta) {
        if (delta == null) return existing;
        return (existing == null ? BigDecimal.ZERO : existing).add(delta);
    }

    // ── Type-safe row extractors ─────────────────────────────────────────────────

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal getBigDecimal(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try { return new BigDecimal(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private LocalDate getLocalDate(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof java.sql.Date) return ((java.sql.Date) value).toLocalDate();
        try { return LocalDate.parse(value.toString()); } catch (Exception e) {
            log.warn("Failed to parse date: {}", value);
            return null;
        }
    }

    // ── Query methods ────────────────────────────────────────────────────────────

    public List<RatedSummaryDTO> getSummariesByAccount(String accountCode, LocalDate startDate, LocalDate endDate) {
        return ratedSummaryRepository.findByAccountCodeAndSummaryDateBetween(accountCode, startDate, endDate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<RatedSummaryDTO> getSummariesByDate(LocalDate date, String direction) {
        List<RatedSummary> summaries = direction != null
                ? ratedSummaryRepository.findBySummaryDateAndDirection(date, direction)
                : ratedSummaryRepository.findBySummaryDate(date);
        return summaries.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<RatedSummaryDTO> getSummariesByPartner(Long partnerId, LocalDate startDate, LocalDate endDate) {
        return ratedSummaryRepository.findByPartnerIdAndSummaryDateBetween(partnerId, startDate, endDate)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private RatedSummaryDTO toDTO(RatedSummary entity) {
        return RatedSummaryDTO.builder()
                .summaryId(entity.getSummaryId())
                .summaryDate(entity.getSummaryDate())
                .accountCode(entity.getAccountCode())
                .partnerId(entity.getPartnerId())
                .partnerName(entity.getPartnerName())
                .sourcePrefix(entity.getSourcePrefix())
                .sourcePrefixName(entity.getSourcePrefixName())
                .sourceCountryCode(entity.getSourceCountryCode())
                .sourceCountryName(entity.getSourceCountryName())
                .destinationPrefix(entity.getDestinationPrefix())
                .destinationPrefixName(entity.getDestinationPrefixName())
                .destinationCountryCode(entity.getDestinationCountryCode())
                .destinationCountryName(entity.getDestinationCountryName())
                .zoneName(entity.getZoneName())
                .ratingType(entity.getRatingType())
                .serviceType(entity.getServiceType())
                .direction(entity.getDirection())
                .appliedRate(entity.getAppliedRate())
                .totalCalls(entity.getTotalCalls())
                .totalSms(entity.getTotalSms())
                .totalSessions(entity.getTotalSessions())
                .totalDuration(entity.getTotalDuration())
                .totalCharge(entity.getTotalCharge())
                .totalDataVolume(entity.getTotalDataVolume())
                .build();
    }
}
