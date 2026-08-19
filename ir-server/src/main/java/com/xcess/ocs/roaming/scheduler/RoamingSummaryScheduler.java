package com.xcess.ocs.roaming.scheduler;

import com.xcess.ocs.roaming.service.RoamingSummaryGenerationService;
import com.xcess.ocs.roaming.service.RoamingSummarySchedulerStatusService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Daily cron scheduler that generates TAP OUT rated summaries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoamingSummaryScheduler {

    private static final long WAIT_TIMEOUT_MINUTES  = 60;
    private static final long WAIT_INTERVAL_SECONDS = 30;

    private final RoamingSummaryGenerationService generationService;
    private final RoamingSummarySchedulerStatusService statusService;

    private volatile boolean interrupted = false;

    @Scheduled(cron = "${roaming.summary.cron:0 0 3 * * ?}")
    public void runDailySummary() {
        log.info("RoamingSummaryScheduler: starting");
        long startMs = System.currentTimeMillis();

        try {
            if (!waitForSchedulerLock()) {
                log.warn("RoamingSummaryScheduler: skipped — timeout waiting for previous run to finish");
                return;
            }

            statusService.updateToRunning();

            if (interrupted) {
                log.warn("RoamingSummaryScheduler: interrupted before processing");
                statusService.updateToInterrupted();
                return;
            }

            // Process yesterday's window
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime start = yesterday.atStartOfDay();
            LocalDateTime end   = yesterday.atTime(23, 59, 59);

            log.info("RoamingSummaryScheduler: processing window {} to {}", start, end);
            int summariesCreated = generationService.generateSummaries(start, end);
            long elapsed = System.currentTimeMillis() - startMs;

            if (interrupted) {
                statusService.updateToInterrupted();
                log.warn("RoamingSummaryScheduler: interrupted after creating {} summaries, time={}ms",
                        summariesCreated, elapsed);
            } else {
                statusService.updateToSuccess(summariesCreated);
                log.info("RoamingSummaryScheduler: completed — summaries={}, time={}ms",
                        summariesCreated, elapsed);
            }

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.error("RoamingSummaryScheduler: failed after {}ms — {}", elapsed, e.getMessage(), e);
            statusService.updateToFailed(e.getMessage());
        }
    }

    private boolean waitForSchedulerLock() {
        int maxAttempts = (int) (WAIT_TIMEOUT_MINUTES * 60 / WAIT_INTERVAL_SECONDS);
        int attempt     = 0;

        while (attempt < maxAttempts) {
            if (!statusService.isRunning()) {
                return true;
            }
            attempt++;
            log.debug("RoamingSummaryScheduler: previous run still active — waiting ({}/{})", attempt, maxAttempts);
            try {
                TimeUnit.SECONDS.sleep(WAIT_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                log.warn("RoamingSummaryScheduler: wait interrupted");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("RoamingSummaryScheduler: timed out waiting for lock — skipping");
        return false;
    }

    @PreDestroy
    public void onShutdown() {
        log.warn("RoamingSummaryScheduler: application shutdown detected — marking interrupted");
        interrupted = true;
        if (statusService.isRunning()) {
            statusService.updateToInterrupted();
        }
    }
}
