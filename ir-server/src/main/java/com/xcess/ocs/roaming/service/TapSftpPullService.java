package com.xcess.ocs.roaming.service;

import com.jcraft.jsch.*;
import com.xcess.ocs.entity.ClearingHouse;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.ClearingHouseRepository;
import com.xcess.ocs.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;

@Slf4j
@Service
@RequiredArgsConstructor
public class TapSftpPullService {

    private final ClearingHouseRepository clearingHouseRepository;
    private final PartnerRepository partnerRepository;

    @Value("${roaming.tap.watch-dir:tap-files/inbox}")
    private String watchDir;

    public void pullAll() {
        log.info("Starting TAP files pull from SFTP sources");
        File localDir = new File(watchDir);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }

        pullFromDch();
        pullFromDirect();
    }

    private void pullFromDch() {
        List<ClearingHouse> clearingHouses = clearingHouseRepository.findByIsDeletedFalse();
        for (ClearingHouse ch : clearingHouses) {
            if (ch.getSftpHost() == null || ch.getSftpHost().isBlank()) {
                continue;
            }
            if (ch.getSftpInboxPath() == null || ch.getSftpInboxPath().isBlank()) {
                log.warn("DCH {} has no SFTP inbox path defined", ch.getName());
                continue;
            }

            int port = ch.getSftpPort() != null ? ch.getSftpPort() : 22;
            log.info("Pulling TAP IN files from DCH: {}", ch.getName());
            try {
                sftpDownloadAndCleanup(
                        ch.getSftpHost(), port, ch.getSftpUsername(), ch.getSftpPassword(),
                        ch.getSftpInboxPath(), watchDir
                );
            } catch (Exception e) {
                log.error("Failed to pull from DCH {}: {}", ch.getName(), e.getMessage(), e);
            }
        }
    }

    private void pullFromDirect() {
        List<Partner> partners = partnerRepository.findAll();
        for (Partner partner : partners) {
            if (partner.getLineOfBusiness() != LineOfBusiness.ROAMING || partner.getClearingHouse() != null) {
                continue;
            }

            String host = partner.getSftpHost();
            if (host == null || host.isBlank()) {
                continue;
            }

            try {
                int port = partner.getSftpPort() != null ? partner.getSftpPort() : 22;
                String username = partner.getSftpUsername() != null ? partner.getSftpUsername() : "";
                String password = partner.getSftpPassword() != null ? partner.getSftpPassword() : "";
                // Use inboxPath if defined, otherwise fallback to remotePath or root
                String remotePath = partner.getSftpInboxPath();
                if (remotePath == null || remotePath.isBlank()) {
                    remotePath = partner.getSftpRemotePath();
                }

                log.info("Pulling TAP IN files directly from Partner: {}", partner.getPartnerCode());
                sftpDownloadAndCleanup(
                        host, port, username, password, remotePath != null ? remotePath : "", watchDir
                );
            } catch (Exception e) {
                log.error("Failed to pull from Partner {}: {}", partner.getPartnerCode(), e.getMessage(), e);
            }
        }
    }

    private void sftpDownloadAndCleanup(String host, int port, String username, String password,
                                        String remoteDir, String localDir) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(30_000);

        try {
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(10_000);
            try {
                channel.cd(remoteDir);
                Vector<ChannelSftp.LsEntry> entries = channel.ls(".");
                for (ChannelSftp.LsEntry entry : entries) {
                    if (entry.getAttrs().isDir()) continue;

                    String filename = entry.getFilename();
                    // Optional: filter only TAP files if there's a naming convention, but usually everything in inbox is TAP
                    Path localFile = Paths.get(localDir, filename);
                    
                    log.info("Downloading TAP IN file: {} from {}", filename, host);
                    channel.get(filename, localFile.toString());
                    
                    // Cleanup remote file
//                    try {
//                        channel.rm(filename);
//                        log.debug("Deleted remote file: {} from {}", filename, host);
//                    } catch (SftpException e) {
//                        log.warn("Failed to delete remote file {} after download from {}", filename, host);
//                    }
                }
            } finally {
                channel.disconnect();
            }
        } finally {
            session.disconnect();
        }
    }
}
