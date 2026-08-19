package com.xcess.ocs.roaming.scheduler;

import com.xcess.ocs.roaming.service.TapSftpPullService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "roaming.tap.pull-enabled", havingValue = "true", matchIfMissing = true)
public class TapSftpPullScheduler {

    private final TapSftpPullService tapSftpPullService;

    // Run every 10 minutes by default
    @Scheduled(fixedDelayString = "${roaming.tap.pull-interval-ms:600000}")
    public void pull() {
        log.info("Starting TAP SFTP Pull Scheduler");
        try {
            tapSftpPullService.pullAll();
        } catch (Exception e) {
            log.error("Error during TAP SFTP Pull Scheduler execution: {}", e.getMessage(), e);
        }
        log.info("Finished TAP SFTP Pull Scheduler");
    }
}
