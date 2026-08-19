package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.*;
import com.xcess.ocs.roaming.repository.RoamingCdrRepository;
import com.xcess.ocs.roaming.repository.RoamingRatedSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoamingRatedSummaryService {

    private final RoamingCdrRepository roamingCdrRepository;
    private final RoamingRatedSummaryRepository summaryRepository;

    @Transactional
    public void summarize(TapFileRecord tapFileRecord) {
        List<RoamingCdr> cdrs = roamingCdrRepository
                .findByTapFileRecord_TapFileIdAndIsSummarizedFalse(tapFileRecord.getTapFileId());

        Map<String, List<RoamingCdr>> grouped = cdrs.stream()
                .collect(Collectors.groupingBy(c ->
                        c.getTapDirection() + "|" +
                        (c.getServiceType() != null ? c.getServiceType().name() : "") + "|" +
                        // Group by zoneName instead of iotRate.destinationPrefix
                        // Zone is the new grouping dimension for ROAMING rating
                        (c.getZoneName() != null ? c.getZoneName() : "")));

        for (Map.Entry<String, List<RoamingCdr>> entry : grouped.entrySet()) {
            String[] keys = entry.getKey().split("\\|", -1);
            List<RoamingCdr> group = entry.getValue();

            RoamingRatedSummary summary = new RoamingRatedSummary();
            summary.setSummaryDate(LocalDate.now());
            summary.setTapFileRecord(tapFileRecord);
            summary.setPartner(tapFileRecord.getPartner());
            summary.setTapDirection(TapDirection.valueOf(keys[0]));
            summary.setServiceType(keys[1].isEmpty() ? null : ServiceType.valueOf(keys[1]));
            // keys[2] is now zoneName (e.g. "ZONE_EU") instead of destinationPrefix
            // stored in destination_prefix column for backward compatibility with existing queries
            summary.setDestinationPrefix(keys[2].isEmpty() ? null : keys[2]);

            // ── Counts ───────────────────────────────────────────────────────
            // Grouped by serviceType so every CDR in the group has the same type.
            ServiceType groupServiceType = summary.getServiceType();
            if (ServiceType.VOICE.equals(groupServiceType)) {
                // Each CDR = 1 call
                summary.setTotalCalls(group.size());
                summary.setTotalSms(0);
            } else if (ServiceType.SMS.equals(groupServiceType)) {
                // eventNos = number of SMS messages in the CDR (may be > 1)
                summary.setTotalCalls(0);
                summary.setTotalSms(group.stream()
                        .mapToInt(c -> c.getEventNos() != null ? c.getEventNos() : 1)
                        .sum());
            } else {
                // USAGE / unknown
                summary.setTotalCalls(0);
                summary.setTotalSms(0);
            }

            // ── Duration ─────────────────────────────────────────────────────
            summary.setTotalDurationSec(group.stream()
                    .mapToLong(c -> c.getCallDurationSec() != null ? c.getCallDurationSec() : 0L)
                    .sum());

            // ── Data volume — normalise to bytes ─────────────────────────────
            // TAP 3.12 encodes data in octets (bytes), so no conversion is required
            // for standard TAP files. The explicit conversion block below is kept
            // for resilience in case a custom TAP profile decodes in KB/MB/GB.
            summary.setTotalUsageBytes(group.stream()
                    .map(c -> {
                        if (c.getTotalUsage() == null) return BigDecimal.ZERO;
                        // RoamingCdr.getMeasurementUnitForRating() always returns "BYTES"
                        // (TAP standard), so the switch below is a safety guard only.
                        BigDecimal val = c.getTotalUsage();
                        String unit = c.getMeasurementUnitForRating() != null
                                ? c.getMeasurementUnitForRating().toUpperCase() : "BYTES";
                        switch (unit) {
                            case "KB": case "KILOBYTE": case "KILOBYTES":
                                return val.multiply(BigDecimal.valueOf(1024));
                            case "MB": case "MEGABYTE": case "MEGABYTES":
                                return val.multiply(BigDecimal.valueOf(1_048_576));
                            case "GB": case "GIGABYTE": case "GIGABYTES":
                                return val.multiply(BigDecimal.valueOf(1_073_741_824));
                            default:
                                return val; // already in bytes
                        }
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            // ── Charges ──────────────────────────────────────────────────────
            summary.setTotalTapCharge(group.stream().map(c -> c.getTapCharge() != null ? c.getTapCharge() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalOurCharge(group.stream().map(c -> c.getOurCharge() != null ? c.getOurCharge() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setCurrency(group.get(0).getCurrency());
            summaryRepository.save(summary);

            group.forEach(c -> c.setSummarized(true));
            roamingCdrRepository.saveAll(group);
        }
    }
}
