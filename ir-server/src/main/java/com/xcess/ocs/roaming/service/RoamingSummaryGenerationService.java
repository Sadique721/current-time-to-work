package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapOutRatedSummary;
import com.xcess.ocs.roaming.repository.TapOutRatedSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business logic for the Roaming TAP OUT Summary generation step.
 *
 * Flow:
 *   1. Fetch ROAMING CDRs where isTapOutGenerated=true AND isSummarized=false
 *      for the given time window.
 *   2. Group by tapFileRecord.tapFileId.
 *   3. For each group compute per-file aggregates.
 *   4. Persist TapOutRatedSummary (one row per TAP file).
 *   5. Bulk-mark all processed CDR IDs as isSummarized=true.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoamingSummaryGenerationService {

    private final VoiceRatedCdrRepository voiceRepo;
    private final SmsRatedCdrRepository   smsRepo;
    private final UsageRatedCdrRepository usageRepo;
    private final TapOutRatedSummaryRepository summaryRepo;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generate summaries for all unsummarized TAP OUT CDRs in the given window.
     *
     * @param startTime window start (inclusive)
     * @param endTime   window end   (inclusive)
     * @return number of TapOutRatedSummary rows created
     */
    @Transactional
    public int generateSummaries(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("RoamingSummaryGenerationService: generating summaries for window {} – {}", startTime, endTime);

        List<VoiceRatedCdr>  voiceCdrs = voiceRepo.findUnsummarizedTapOutCdrs(startTime, endTime);
        List<SmsRatedCdr>    smsCdrs   = smsRepo.findUnsummarizedTapOutCdrs(startTime, endTime);
        List<UsageRatedCdr>  usageCdrs = usageRepo.findUnsummarizedTapOutCdrs(startTime, endTime);

        log.info("Unsummarized CDRs found — voice: {}, sms: {}, usage: {}",
                voiceCdrs.size(), smsCdrs.size(), usageCdrs.size());

        if (voiceCdrs.isEmpty() && smsCdrs.isEmpty() && usageCdrs.isEmpty()) {
            log.info("No unsummarized TAP OUT CDRs found for window {} – {}", startTime, endTime);
            return 0;
        }

        // Collect all distinct TAP file/partner combinations present in the CDR lists
        Map<String, TapFileRecord> tapFileMap = collectTapFileMap(voiceCdrs, smsCdrs, usageCdrs);
        int summariesCreated = 0;

        for (Map.Entry<String, TapFileRecord> entry : tapFileMap.entrySet()) {
            String       key        = entry.getKey();
            Long         tapFileId  = entry.getValue().getTapFileId();
            Long         partnerId  = entry.getValue().getPartner().getPartnerId();
            TapFileRecord tapFile   = entry.getValue();

            // Skip if summaries already exist for this file
            if (summaryRepo.findByTapFileRecord_TapFileId(tapFileId).isPresent()) {
                log.warn("Summary already exists for tapFileId={}, skipping", tapFileId);
                continue;
            }

            List<VoiceRatedCdr> fVoice = voiceCdrs.stream()
                    .filter(c -> c.getTapFileRecord() != null && tapFileId.equals(c.getTapFileRecord().getTapFileId()))
                    .collect(Collectors.toList());
            List<SmsRatedCdr>   fSms   = smsCdrs.stream()
                    .filter(c -> c.getTapFileRecord() != null && tapFileId.equals(c.getTapFileRecord().getTapFileId()))
                    .collect(Collectors.toList());
            List<UsageRatedCdr> fUsage = usageCdrs.stream()
                    .filter(c -> c.getTapFileRecord() != null && tapFileId.equals(c.getTapFileRecord().getTapFileId()))
                    .collect(Collectors.toList());

            // Group the filtered CDRs by serviceType and zoneName
            // Note: Since each list only has one service type inherently, we group them individually
            Map<String, List<VoiceRatedCdr>> voiceByZone = fVoice.stream()
                    .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));
            Map<String, List<SmsRatedCdr>> smsByZone = fSms.stream()
                    .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));
            Map<String, List<UsageRatedCdr>> usageByZone = fUsage.stream()
                    .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));

            // Create summaries for Voice groups
            for (Map.Entry<String, List<VoiceRatedCdr>> voiceEntry : voiceByZone.entrySet()) {
                TapOutRatedSummary summary = buildSummary(tapFile, voiceEntry.getValue(), null, null);
                summary.setServiceType(com.xcess.ocs.entity.ServiceType.VOICE);
                summary.setZoneName(voiceEntry.getKey().isEmpty() ? null : voiceEntry.getKey());
                summaryRepo.save(summary);
                summariesCreated++;
            }

            // Create summaries for SMS groups
            for (Map.Entry<String, List<SmsRatedCdr>> smsEntry : smsByZone.entrySet()) {
                TapOutRatedSummary summary = buildSummary(tapFile, null, smsEntry.getValue(), null);
                summary.setServiceType(com.xcess.ocs.entity.ServiceType.SMS);
                summary.setZoneName(smsEntry.getKey().isEmpty() ? null : smsEntry.getKey());
                summaryRepo.save(summary);
                summariesCreated++;
            }

            // Create summaries for USAGE groups
            for (Map.Entry<String, List<UsageRatedCdr>> usageEntry : usageByZone.entrySet()) {
                TapOutRatedSummary summary = buildSummary(tapFile, null, null, usageEntry.getValue());
                summary.setServiceType(com.xcess.ocs.entity.ServiceType.USAGE);
                summary.setZoneName(usageEntry.getKey().isEmpty() ? null : usageEntry.getKey());
                summaryRepo.save(summary);
                summariesCreated++;
            }

            // Mark CDRs as summarized
            markSummarized(fVoice, fSms, fUsage);

            log.info("Created {} summaries for tapFileId={} fileName={}",
                    (voiceByZone.size() + smsByZone.size() + usageByZone.size()), tapFileId, tapFile.getFileName());
        }

        log.info("RoamingSummaryGenerationService: created {} summaries", summariesCreated);
        return summariesCreated;
    }

    /**
     * Single-file variant for targeted/manual runs (e.g. via API or retry logic).
     *
     * @param tapFileRecord the specific TAP OUT file to summarise
     * @return the saved TapOutRatedSummary, or null if no unsummarized CDRs exist
     */
    @Transactional
    public TapOutRatedSummary generateSummariesForTapFile(TapFileRecord tapFileRecord) {
        Long tapFileId = tapFileRecord.getTapFileId();

        if (summaryRepo.findByTapFileRecord_TapFileId(tapFileId).isPresent()) {
            log.warn("Summary already exists for tapFileId={}", tapFileId);
            return summaryRepo.findByTapFileRecord_TapFileId(tapFileId).orElse(null);
        }

        List<VoiceRatedCdr>  voice = voiceRepo.findUnsummarizedByTapFileId(tapFileId);
        List<SmsRatedCdr>    sms   = smsRepo.findUnsummarizedByTapFileId(tapFileId);
        List<UsageRatedCdr>  usage = usageRepo.findUnsummarizedByTapFileId(tapFileId);

        if (voice.isEmpty() && sms.isEmpty() && usage.isEmpty()) {
            log.info("No unsummarized CDRs for tapFileId={}", tapFileId);
            return null;
        }

        // We return the first one created just for API compat, though it creates multiple now.
        TapOutRatedSummary firstSaved = null;

        Map<String, List<VoiceRatedCdr>> voiceByZone = voice.stream()
                .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));
        Map<String, List<SmsRatedCdr>> smsByZone = sms.stream()
                .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));
        Map<String, List<UsageRatedCdr>> usageByZone = usage.stream()
                .collect(Collectors.groupingBy(c -> (c.getZoneName() != null ? c.getZoneName() : "")));

        for (Map.Entry<String, List<VoiceRatedCdr>> voiceEntry : voiceByZone.entrySet()) {
            TapOutRatedSummary summary = buildSummary(tapFileRecord, voiceEntry.getValue(), null, null);
            summary.setServiceType(com.xcess.ocs.entity.ServiceType.VOICE);
            summary.setZoneName(voiceEntry.getKey().isEmpty() ? null : voiceEntry.getKey());
            TapOutRatedSummary saved = summaryRepo.save(summary);
            if (firstSaved == null) firstSaved = saved;
        }

        for (Map.Entry<String, List<SmsRatedCdr>> smsEntry : smsByZone.entrySet()) {
            TapOutRatedSummary summary = buildSummary(tapFileRecord, null, smsEntry.getValue(), null);
            summary.setServiceType(com.xcess.ocs.entity.ServiceType.SMS);
            summary.setZoneName(smsEntry.getKey().isEmpty() ? null : smsEntry.getKey());
            TapOutRatedSummary saved = summaryRepo.save(summary);
            if (firstSaved == null) firstSaved = saved;
        }

        for (Map.Entry<String, List<UsageRatedCdr>> usageEntry : usageByZone.entrySet()) {
            TapOutRatedSummary summary = buildSummary(tapFileRecord, null, null, usageEntry.getValue());
            summary.setServiceType(com.xcess.ocs.entity.ServiceType.USAGE);
            summary.setZoneName(usageEntry.getKey().isEmpty() ? null : usageEntry.getKey());
            TapOutRatedSummary saved = summaryRepo.save(summary);
            if (firstSaved == null) firstSaved = saved;
        }

        markSummarized(voice, sms, usage);

        log.info("Manual summaries created for tapFileId={}", tapFileId);
        return firstSaved;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TapOutRatedSummary buildSummary(TapFileRecord tapFile,
                                            List<VoiceRatedCdr>  voice,
                                            List<SmsRatedCdr>    sms,
                                            List<UsageRatedCdr>  usage) {
        TapOutRatedSummary s = new TapOutRatedSummary();
        LocalDate summaryDate = null;
        if (voice != null && !voice.isEmpty() && voice.get(0).getStartTime() != null) {
            summaryDate = voice.get(0).getStartTime().toLocalDate();
        } else if (sms != null && !sms.isEmpty() && sms.get(0).getStartTime() != null) {
            summaryDate = sms.get(0).getStartTime().toLocalDate();
        } else if (usage != null && !usage.isEmpty() && usage.get(0).getStartTime() != null) {
            summaryDate = usage.get(0).getStartTime().toLocalDate();
        }
        if (summaryDate == null) {
            summaryDate = (tapFile != null && tapFile.getProcessedAt() != null)
                    ? tapFile.getProcessedAt().toLocalDate()
                    : LocalDate.now();
        }
        s.setSummaryDate(summaryDate);
        s.setTapFileRecord(tapFile);
        s.setPartner(tapFile.getPartner());

        // Aggregates
        s.setTotalCalls(voice != null ? voice.size() : 0);

        s.setTotalSms(sms != null ? sms.stream()
                .mapToInt(c -> c.getEventNos() != null ? c.getEventNos() : 0)
                .sum() : 0);

        s.setTotalDurationSec(voice != null ? voice.stream()
                .mapToLong(c -> c.getDurationSeconds() != null ? c.getDurationSeconds() : 0L)
                .sum() : 0L);

        s.setTotalUsageBytes(usage != null ? usage.stream()
                .map(c -> com.xcess.ocs.constants.enums.DataUnit.toBytes(c.getTotalUsage(), c.getMeasurementUnit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO);

        BigDecimal voiceCharge = voice != null ? voice.stream()
                .map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        BigDecimal smsCharge   = sms != null ? sms.stream()
                .map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        BigDecimal usageCharge = usage != null ? usage.stream()
                .map(c -> c.getOutgoingTotalCost() != null ? c.getOutgoingTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        s.setTotalCharge(voiceCharge.add(smsCharge).add(usageCharge));

        s.setCurrency(tapFile.getPartner() != null
                ? tapFile.getPartner().getBillingCurrency() : null);

        return s;
    }

    private void markSummarized(List<VoiceRatedCdr>  voice,
                                List<SmsRatedCdr>    sms,
                                List<UsageRatedCdr>  usage) {
        if (voice != null && !voice.isEmpty()) {
            List<Long> ids = voice.stream().map(VoiceRatedCdr::getRatedCdrId).collect(Collectors.toList());
            voiceRepo.markAsSummarizedByIds(ids);
        }
        if (sms != null && !sms.isEmpty()) {
            List<Long> ids = sms.stream().map(SmsRatedCdr::getSmsRatedCdrId).collect(Collectors.toList());
            smsRepo.markAsSummarizedByIds(ids);
        }
        if (usage != null && !usage.isEmpty()) {
            List<Long> ids = usage.stream().map(UsageRatedCdr::getRatedCdrId).collect(Collectors.toList());
            usageRepo.markAsSummarizedByIds(ids);
        }
    }

    private Map<String, TapFileRecord> collectTapFileMap(List<VoiceRatedCdr>  voice,
                                                         List<SmsRatedCdr>    sms,
                                                         List<UsageRatedCdr>  usage) {
        Map<String, TapFileRecord> result = new java.util.LinkedHashMap<>();
        
        voice.stream().filter(c -> c.getTapFileRecord() != null && c.getTapFileRecord().getPartner() != null)
                .forEach(c -> result.putIfAbsent(c.getTapFileRecord().getTapFileId() + "_" + c.getTapFileRecord().getPartner().getPartnerId(), c.getTapFileRecord()));
                
        sms.stream().filter(c -> c.getTapFileRecord() != null && c.getTapFileRecord().getPartner() != null)
                .forEach(c -> result.putIfAbsent(c.getTapFileRecord().getTapFileId() + "_" + c.getTapFileRecord().getPartner().getPartnerId(), c.getTapFileRecord()));
                
        usage.stream().filter(c -> c.getTapFileRecord() != null && c.getTapFileRecord().getPartner() != null)
                .forEach(c -> result.putIfAbsent(c.getTapFileRecord().getTapFileId() + "_" + c.getTapFileRecord().getPartner().getPartnerId(), c.getTapFileRecord()));
                
        return result;
    }
}
