package com.xcess.ocs.roaming.service;

import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.tapmodels.tap.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Decodes a TAP file using the real GSMA SDK (com.xcess.ocs.tapmodels).
 *
 * Populates TapFileRecord metadata from:
 *   - BatchControlInfo  → tapVersion, senderTadig/recipientTadig validation, fileSequenceNo validation
 *   - AccountingInfo    → tapDecimalPlaces, localCurrency
 *   - AuditControlInfo  → totalRecords, totalCharge
 *
 * Returns the decoded DataInterChange for downstream extraction,
 * or null on error (status set to ERROR on the record).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TapFileDecoderService {

    private final TapFileRecordRepository tapFileRecordRepository;

    public DataInterChange decode(TapFileRecord record) {
        Path path = Paths.get(record.getFilePath());

        DataInterChange dic;
        try {
            Optional<DataInterChange> opt = TapFiles.read(path, StandardOpenOption.READ);
            if (opt.isEmpty()) {
                markError(record, "TapFiles.read() returned empty — file may be a Notification or corrupt");
                return null;
            }
            dic = opt.get();
        } catch (IOException e) {
            log.error("Failed to read TAP file {}: {}", record.getFilePath(), e.getMessage(), e);
            markError(record, "IO error: " + e.getMessage());
            return null;
        }

        TransferBatch batch = dic.getTransferBatch();
        if (batch == null) {
            markError(record, "TAP file contains no TransferBatch");
            return null;
        }

        populateFromBatchControlInfo(record, batch.getBatchControlInfo());
        populateFromAccountingInfo(record, batch.getAccountingInfo());
        populateFromAuditControlInfo(record, batch.getAuditControlInfo());

        if (record.getStatus() == TapFileStatus.ERROR) {
            tapFileRecordRepository.save(record);
            return null;
        }

        tapFileRecordRepository.save(record);
        return dic;
    }

    // ── BatchControlInfo ────────────────────────────────────────────────────────

    private void populateFromBatchControlInfo(TapFileRecord record, BatchControlInfo bci) {
        if (bci == null) return;

        // TAP version from specificationVersionNumber + releaseVersionNumber
        if (bci.getSpecificationVersionNumber() != null && bci.getReleaseVersionNumber() != null) {
            record.setTapVersion("TAP" + bci.getSpecificationVersionNumber().value
                    + "." + bci.getReleaseVersionNumber().value);
        }

        // Validate sender TADIG against filename
        if (bci.getSender() != null) {
            String bciSender = new String(bci.getSender().value).trim();
            if (record.getSenderTadig() != null && !bciSender.equals(record.getSenderTadig())) {
                log.warn("Filename senderTadig '{}' != BatchControlInfo sender '{}'",
                        record.getSenderTadig(), bciSender);
            }
            record.setSenderTadig(bciSender); // trust the file over the filename
        }

        if (bci.getRecipient() != null) {
            String bciRecipient = new String(bci.getRecipient().value).trim();
            if (record.getRecipientTadig() != null && !bciRecipient.equals(record.getRecipientTadig())) {
                log.warn("Filename recipientTadig '{}' != BatchControlInfo recipient '{}'",
                        record.getRecipientTadig(), bciRecipient);
            }
            record.setRecipientTadig(bciRecipient);
        }

        record.setFileSequenceNo( Integer.parseInt(new String(bci.getFileSequenceNumber().value).trim()));
    }

    // ── AccountingInfo ──────────────────────────────────────────────────────────

    private void populateFromAccountingInfo(TapFileRecord record, AccountingInfo ai) {
        if (ai == null) return;

        if (ai.getTapDecimalPlaces() != null) {
            record.setTapDecimalPlaces(ai.getTapDecimalPlaces().value);
        }
        if (ai.getLocalCurrency() != null) {
            record.setLocalCurrency(new String(ai.getLocalCurrency().value).trim());
        }
    }

    // ── AuditControlInfo ────────────────────────────────────────────────────────

    private void populateFromAuditControlInfo(TapFileRecord record, AuditControlInfo aci) {
        if (aci == null) return;

        if (aci.getCallEventDetailsCount() != null) {
            record.setTotalRecords(aci.getCallEventDetailsCount().value);
        }
        if (aci.getTotalCharge() != null) {
            record.setTotalCharge(aci.getTotalCharge().value);
        }
    }

    private void markError(TapFileRecord record, String reason) {
        record.setStatus(TapFileStatus.ERROR);
        record.setErrorReason(reason);
        log.error("TAP decode error for {}: {}", record.getFileName(), reason);
    }
}
