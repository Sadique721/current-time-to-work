package com.xcess.ocs.roaming.scheduler;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileType;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.roaming.service.TapFileProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class TapFileWatcherScheduler {

    private final TapFileRecordRepository tapFileRecordRepository;
    private final PartnerRepository partnerRepository;
    private final TapFileProcessingService processingService;

    @Value("${roaming.tap.watch-dir:tap-files/inbox}")
    private String watchDir;

    @Scheduled(fixedDelayString = "${roaming.tap.poll-interval-ms:60000}")
    public void poll() {
        Path dir = Paths.get(watchDir);
        if (!Files.exists(dir)) return;

        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> !Files.isDirectory(p))
                 .forEach(this::processFile);
        } catch (IOException e) {
            log.error("TAP watcher error: {}", e.getMessage(), e);
        }
    }

    private void processFile(Path path) {
        String fileName = path.getFileName().toString();
        if (tapFileRecordRepository.findByFileNameAndFileType(fileName, TapFileType.TAP_IN).isPresent()) return;

        String senderTadig = fileName.length() >= 7 ? fileName.substring(2, 7) : null;
        Partner partner = senderTadig != null
                ? partnerRepository.findRoamingPartnersWithFullProfile().stream()
                    .filter(p -> senderTadig.equals(p.getTadigCode()))
                    .findFirst().orElse(null)
                : null;

        TapFileRecord record = new TapFileRecord();
        record.setFileName(fileName);
        record.setFileType(TapFileType.TAP_IN);
        record.setStatus(TapFileStatus.RECEIVED);
        record.setPartner(partner);
        record.setFilePath(path.toString());
        record.setSenderTadig(senderTadig);
        record.setRecipientTadig(fileName.length() >= 12 ? fileName.substring(7, 12) : null);
        tapFileRecordRepository.save(record);

        processingService.process(record);

        try {
            Path processedDir = path.resolveSibling("processed");
            if (!Files.exists(processedDir)) {
                Files.createDirectories(processedDir);
            }
            Path targetPath = processedDir.resolve(fileName);
            Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
            record.setFilePath(targetPath.toString());
            tapFileRecordRepository.save(record);
        } catch (IOException e) {
            log.warn("Could not move processed TAP file {}: {}", fileName, e.getMessage());
        }
    }

    private Integer parseSequenceNo(String fileName) {
        try {
            String digits = fileName.replaceAll("\\D", "");
            return digits.length() >= 5 ? Integer.parseInt(digits.substring(0, 5)) : null;
        } catch (Exception e) { return null; }
    }
}
