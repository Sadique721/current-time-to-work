package com.xcess.ocs.roaming.scheduler;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.service.TapOutFileGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Daily scheduler that generates TAP OUT ASN files for all active ROAMING partners.
 *
 * Trigger:
 *   Runs daily at 1:00 AM by default (configurable via roaming.tap.out-cron).
 *   Processes the previous day's rated_cdr records (yesterday 00:00 to 23:59:59).
 *
 * Partner eligibility:
 *   - lineOfBusiness = ROAMING
 *   - tadigCode must be set
 *   - tapProfile must be assigned (required for dynamic field mapping)
 *
 * Note: @Where(clause = "is_deleted = false") on Partner entity means
 *       findAll() never returns deleted partners — no need to filter isDeleted here.
 *
 * Configuration in application.properties:
 *   roaming.tap.out-cron=0 0 1 * * ?   (daily at 1am, default)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TapOutScheduler {

    private final PartnerRepository partnerRepository;
    private final TapOutFileGenerationService tapOutFileGenerationService;

    /**
     * Daily TAP OUT generation job.
     *
     * Window: previous day (yesterday 00:00:00 to 23:59:59).
     * This ensures all CDRs rated during the previous day are included.
     *
     * Cron expression configurable via roaming.tap.out-cron property.
     * Default: 0 0 1 * * ? (every day at 01:00:00)
     */
    @Scheduled(cron = "${roaming.tap.out-cron:0 0 1 * * ?}")
    public void generateDailyTapOutFiles() {
        // Calculate yesterday's window
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startTime = yesterday.atStartOfDay();
        LocalDateTime endTime = yesterday.atTime(23, 59, 59);

        log.info("TAP OUT scheduler started — processing window: {} to {}", startTime, endTime);

        // Find all active ROAMING partners
        // lineOfBusiness = ROAMING identifies partners that use TAP file flow
        List<Partner> roamingPartners = partnerRepository.findRoamingPartnersWithFullProfile().stream()
                .filter(p -> !p.getTadigCode().isBlank()
                        && p.getTapProfileGroup() != null)
                .collect(Collectors.toList());

        if (roamingPartners.isEmpty()) {
            log.info("TAP OUT scheduler: no eligible ROAMING partners found (check tadigCode and tapProfileGroup assignment)");
            return;
        }

        log.info("TAP OUT scheduler: processing {} ROAMING partners", roamingPartners.size());

        int success = 0;
        int failed = 0;

        for (Partner partner : roamingPartners) {
            try {
                tapOutFileGenerationService.generateForPartner(partner, startTime, endTime);
                success++;
            } catch (Exception e) {
                log.error("TAP OUT generation failed for partner={}: {}",
                        partner.getPartnerCode(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("TAP OUT scheduler completed — success: {}, failed: {}, total: {}",
                success, failed, roamingPartners.size());
    }
}
