package com.xcess.ocs.roaming.service;

import com.jcraft.jsch.*;
import com.xcess.ocs.entity.ClearingHouse;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.ClearingHouseRepository;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Pushes a generated TAP OUT file to the correct SFTP destination.
 *
 * Routing logic:
 *   clearingHouse != null → DCH: use ClearingHouse SFTP config
 *   clearingHouse == null → DIRECT: use partner's own SFTP config fields
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TapSftpPushService {

    private final TapFileRecordRepository tapFileRecordRepository;
    private final ClearingHouseRepository houseRepository;

    /**
     * Push the TAP OUT file and update the TapFileRecord's sftpPushStatus.
     *
     * Remote path structure:
     *   {remotePath}/{yyyy-MM-dd}/{HH}/{fileName}
     *   e.g. /outbox/tap/2025-07-15/03/TDEUR01AUTPT00006
     *
     * The date/hour is taken from record.processedAt (the TAP OUT file creation time).
     *
     * @param partner       the roaming partner
     * @param localFilePath path of the generated TAP OUT file on disk
     * @param record        the TapFileRecord to update with push result
     */
    public void push(Partner partner, Path localFilePath, TapFileRecord record) {
        if (partner.getClearingHouse() == null && (partner.getSftpHost() == null || partner.getSftpHost().isBlank())) {
            log.warn("TAP SFTP push skipped: no SFTP config for partner={}", partner.getPartnerCode());
            return;
        }

        try {
            if (partner.getClearingHouse() != null) {
                pushViaDch(partner, localFilePath, record.getProcessedAt());
            } else {
                pushViaDirect(partner, localFilePath, record.getProcessedAt());
            }
            record.setStatus(TapFileStatus.SFTP_PUSHED);
            log.info("TAP SFTP push succeeded: file={}, partner={}",
                    localFilePath.getFileName(), partner.getPartnerCode());
        } catch (Exception e) {
            record.setStatus(TapFileStatus.SFTP_FAILED);
            record.setErrorReason("SFTP push failed: " + e.getMessage());
            log.error("TAP SFTP push failed: file={}, partner={}: {}",
                    localFilePath.getFileName(), partner.getPartnerCode(), e.getMessage(), e);
        } finally {
            tapFileRecordRepository.save(record);
        }
    }

    // ─── DCH routing ─────────────────────────────────────────────────────────

    private void pushViaDch(Partner partner, Path localFilePath, LocalDateTime createdAt) throws Exception {
        ClearingHouse ch = houseRepository.findById(partner.getClearingHouse().getId()).orElseThrow();
        if (ch == null || ch.getSftpHost() == null) {
            throw new IllegalStateException("DCH routing selected but ClearingHouse SFTP config is missing");
        }
        sftpUpload(
                ch.getSftpHost(),
                ch.getSftpPort() != null ? ch.getSftpPort() : 22,
                ch.getSftpUsername(),
                ch.getSftpPassword(),
                ch.getSftpRemotePath(),
                localFilePath,
                createdAt
        );
    }

    // ─── Direct routing ───────────────────────────────────────────────────────

    private void pushViaDirect(Partner partner, Path localFilePath, LocalDateTime createdAt) throws Exception {
        sftpUpload(
                partner.getSftpHost(),
                partner.getSftpPort() != null ? partner.getSftpPort() : 22,
                partner.getSftpUsername(),
                partner.getSftpPassword(),
                partner.getSftpRemotePath(),
                localFilePath,
                createdAt
        );
    }

    // ─── Core SFTP upload ─────────────────────────────────────────────────────

    /**
     * Upload a file to the SFTP server under a structured date/hour path.
     *
     * Final remote path:
     *   {remotePath}/{yyyy-MM-dd}/{HH}/{fileName}
     *
     * Each directory segment is created with mkdir() if it does not exist.
     * mkdir() on an existing directory throws SftpException with id=SSH_FX_FAILURE —
     * we catch and ignore that specific case so the method is idempotent.
     */
    private void sftpUpload(String host, int port, String username, String password,
                            String remotePath, Path localFilePath, LocalDateTime createdAt) throws Exception {
        String date = createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String hour = createdAt.format(DateTimeFormatter.ofPattern("HH"));

        String base = remotePath.endsWith("/") ? remotePath.substring(0, remotePath.length() - 1) : remotePath;
        String targetDir  = base + "/" + date + "/" + hour;
        String remoteFile = targetDir + "/" + localFilePath.getFileName();

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30_000);

        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10_000);
            try {
                mkdirs(channel, targetDir);
                channel.put(localFilePath.toString(), remoteFile);
                log.info("SFTP uploaded {} → {}:{}{}", localFilePath.getFileName(), host, port, remoteFile);
            } finally {
                channel.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }

    /**
     * Recursively create each segment of the remote path.
     * Ignores SSH_FX_FAILURE (code 4) which most SFTP servers return for mkdir on existing dir.
     */
    private void mkdirs(ChannelSftp channel, String remotePath) throws SftpException {
        StringBuilder current = new StringBuilder();
        for (String segment : remotePath.split("/")) {
            if (segment.isEmpty()) {
                current.append("/");
                continue;
            }
            current.append(segment).append("/");
            try {
                channel.mkdir(current.toString());
                log.debug("SFTP mkdir: {}", current);
            } catch (SftpException e) {
                // SSH_FX_FAILURE (4) = directory already exists on most servers
                if (e.id != ChannelSftp.SSH_FX_FAILURE) {
                    throw e;
                }
            }
        }
    }
}
