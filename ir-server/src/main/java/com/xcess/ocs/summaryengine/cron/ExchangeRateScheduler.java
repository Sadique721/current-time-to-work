package com.xcess.ocs.summaryengine.cron;

import com.xcess.ocs.service.ExchangeRateSchedulerStatusService;
import com.xcess.ocs.service.ExchangeRateService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private static final long WAIT_TIMEOUT_MINUTES = 60;
    private static final long WAIT_INTERVAL_SECONDS = 30;

    private final ExchangeRateService exchangeRateService;
    private final ExchangeRateSchedulerStatusService statusService;

    private volatile boolean interrupted = false;

    @Scheduled(cron = "${exchangeRateScheduler.cron}")
    public void fetchExchangeRates() {
        log.info("Starting exchange rate fetch job");
        long startTime = System.currentTimeMillis();

        try {
            if (!waitForSchedulerLock()) {
                log.warn("Scheduler skipped due to timeout waiting for previous run");
                return;
            }

            Long executionId = statusService.acquireRunningStatus();
            if (executionId == null) {
                log.warn("Scheduler skipped due to concurrent execution");
                return;
            }

            if (interrupted) {
                log.warn("Scheduler interrupted before API call");
                statusService.updateToInterrupted(executionId);
                return;
            }

            int recordsSaved = exchangeRateService.fetchAndSaveRates();
            long executionTime = System.currentTimeMillis() - startTime;

            if (interrupted) {
                statusService.updateToInterrupted(executionId);
                log.warn("Exchange rate scheduler interrupted after saving {} records, time: {}ms",
                        recordsSaved, executionTime);
            } else {
                statusService.updateToSuccess(executionId, recordsSaved, "FRANKFURTER");
                log.info("Exchange rate scheduler completed. Saved {} rates, time: {}ms",
                        recordsSaved, executionTime);
            }

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Exchange rate fetch failed: {}", e.getMessage(), e);
            statusService.updateToFailed(null, "API request failed: " + e.getMessage());
        }
    }

    private boolean waitForSchedulerLock() {
        log.debug("Checking if previous scheduler is still running");

        int maxAttempts = (int) (WAIT_TIMEOUT_MINUTES * 60 / WAIT_INTERVAL_SECONDS);
        int attempt = 0;

        while (attempt < maxAttempts) {
            if (!statusService.isRunning()) {
                log.debug("Scheduler is not running, proceeding with execution");
                return true;
            }
            attempt++;
            log.debug("Previous scheduler still running. Waiting... (attempt {}/{})",
                    attempt, maxAttempts);
            try {
                TimeUnit.SECONDS.sleep(WAIT_INTERVAL_SECONDS);
            } catch (InterruptedException e) {
                log.warn("Wait interrupted");
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Timeout waiting for previous scheduler to complete. Skipping this execution.");
        return false;
    }

    @PreDestroy
    public void onShutdown() {
        log.warn("Application shutdown detected. Marking exchange rate scheduler as interrupted.");
        interrupted = true;
        statusService.markInterruptedIfRunning();
    }
}
