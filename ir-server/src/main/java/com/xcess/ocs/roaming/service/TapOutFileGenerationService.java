package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.RateableCdr;
import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.repository.AccountRepository;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.repository.SmsRatedCdrRepository;
import com.xcess.ocs.repository.UsageRatedCdrRepository;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import com.xcess.ocs.roaming.config.TapFieldPathResolver;
import com.xcess.ocs.roaming.entity.*;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.roaming.repository.TapOutSequenceRepository;
import com.xcess.ocs.roaming.repository.TapProfileRepository;
import com.xcess.ocs.tapmodels.tap.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TapOutFileGenerationService {

    private final VoiceRatedCdrRepository voiceRatedCdrRepository;
    private final SmsRatedCdrRepository smsRatedCdrRepository;
    private final UsageRatedCdrRepository usageRatedCdrRepository;
    private final TapOutSequenceRepository tapOutSequenceRepository;
    private final TapFileRecordRepository tapFileRecordRepository;
    private final AccountRepository accountRepository;
    private final PartnerRepository partnerRepository;
    private final TapProfileRepository tapProfileRepository;
    private final TapSftpPushService tapSftpPushService;
    private final TapFieldPathResolver resolver;

    @Value("${roaming.tap.our-tadig:EUR01}")
    private String ourTadig;

    @Value("${roaming.tap.outbox-dir:C:/INTER-CONNECT/data/tap-files/outbox}")
    private String outboxDir;

    private static final int TAP_DECIMAL_PLACES = 4;

    private static final DateTimeFormatter TAP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void generateForPartner(Partner partner, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("TAP OUT generation: partner={}, window={} to {}",
                partner.getPartnerCode(), startTime, endTime);

        final Long partnerId = partner.getPartnerId();
        Partner enrichedPartner = partnerRepository.findRoamingPartnersWithFullProfile().stream()
                .filter(p -> p.getPartnerId().equals(partnerId))
                .findFirst()
                .orElse(partner);

        if (enrichedPartner.getTapProfileGroup() == null ||
                enrichedPartner.getTapProfileGroup().getTapProfiles().isEmpty()) {
            log.warn("TAP OUT skipped: no TAP profile group assigned to partner={}", enrichedPartner.getPartnerCode());
            return;
        }

        String homePlmn = enrichedPartner.getHplmn();
        if (homePlmn == null) {
            log.warn("TAP OUT skipped: no hplmn found for partner: {}", enrichedPartner.getPartnerCode());
            return;
        }

        List<VoiceRatedCdr> voiceRatedCdrs = voiceRatedCdrRepository.findRatedByHomePlmnAndDateRange(
                homePlmn, startTime, endTime,false);
        List<SmsRatedCdr> smsRatedCdrs = smsRatedCdrRepository.findRatedByHomePlmnAndDateRange(
                homePlmn, startTime, endTime,false);
        List<UsageRatedCdr> usageRatedCdrs = usageRatedCdrRepository.findRatedByHomePlmnAndDateRange(
                homePlmn, startTime, endTime,false);

        List<RateableCdr> ratedCdrs = new ArrayList<>();
        ratedCdrs.addAll(voiceRatedCdrs);
        ratedCdrs.addAll(smsRatedCdrs);
        ratedCdrs.addAll(usageRatedCdrs);

        if (ratedCdrs.isEmpty()) {
            log.info("TAP OUT skipped: no rated CDRs for partner={} in window={} to {}",
                    enrichedPartner.getPartnerCode(), startTime, endTime);
            return;
        }

        log.info("TAP OUT: found {} rated CDRs ({} voice, {} sms, {} usage) for partner={}",
                ratedCdrs.size(), voiceRatedCdrs.size(), smsRatedCdrs.size(),
                usageRatedCdrs.size(), enrichedPartner.getPartnerCode());

        List<RateableCdr> successfulCdrs = new ArrayList<>();
        List<CallEventDetail> generatedEvents = new ArrayList<>();
        for (RateableCdr cdr : ratedCdrs) {
            CallEventDetail event = buildCallEventDetail(cdr, enrichedPartner);
            if (event != null) {
                generatedEvents.add(event);
                successfulCdrs.add(cdr);
            }
        }

        if (successfulCdrs.isEmpty()) {
            log.warn("TAP OUT skipped: all {} CDRs failed to map to TAP profile for partner={}",
                    ratedCdrs.size(), enrichedPartner.getPartnerCode());
            return;
        }

        int nextSequence = getNextSequence(enrichedPartner);

        String partnerTadig = enrichedPartner.getTadigCode() != null ? enrichedPartner.getTadigCode() : "XXXXX";
        String fileName = buildTapOutFileName(ourTadig, partnerTadig, nextSequence);

        DataInterChange dic = buildDataInterChange(enrichedPartner, successfulCdrs, generatedEvents, nextSequence);

        Path outPath = writeToOutbox(dic, fileName);
        if (outPath == null) {
            log.error("TAP OUT file write failed for partner={}, sequence={}",
                    enrichedPartner.getPartnerCode(), nextSequence);
            return;
        }

        TapFileRecord tapFileRecord = saveTapFileRecord(enrichedPartner, fileName, outPath, nextSequence,
                successfulCdrs.size(), calculateTotalRawCharge(successfulCdrs), startTime != null ? startTime.plusHours(1) : LocalDateTime.now());

       tapSftpPushService.push(enrichedPartner, outPath, tapFileRecord);

        for (RateableCdr cdr : successfulCdrs) {
            if (cdr instanceof VoiceRatedCdr) {
                voiceRatedCdrRepository.markAsTapOutGenerated(((VoiceRatedCdr) cdr).getRatedCdrId(), tapFileRecord);
            } else if (cdr instanceof SmsRatedCdr) {
                smsRatedCdrRepository.markAsTapOutGenerated(((SmsRatedCdr) cdr).getSmsRatedCdrId(), tapFileRecord);
            } else if (cdr instanceof UsageRatedCdr) {
                usageRatedCdrRepository.markAsTapOutGenerated(((UsageRatedCdr) cdr).getRatedCdrId(), tapFileRecord);
            }
        }
        log.info("Marked {} CDRs as TAP OUT generated for partner={}", successfulCdrs.size(), enrichedPartner.getPartnerCode());
        log.info("TAP OUT file generated: {} ({} CDRs) for partner={}",
                fileName, successfulCdrs.size(), enrichedPartner.getPartnerCode());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GSMA DataInterChange construction
    // ─────────────────────────────────────────────────────────────────────────

    private DataInterChange buildDataInterChange(Partner partner, List<RateableCdr> successfulCdrs, List<CallEventDetail> generatedEvents, int sequenceNo) {
        TransferBatch batch = new TransferBatch();
        batch.setBatchControlInfo(buildBatchControlInfo(partner, sequenceNo));
        batch.setAccountingInfo(buildAccountingInfo(partner));
        batch.setNetworkInfo(buildNetworkInfo());

        CallEventDetailList eventList = new CallEventDetailList();
        eventList.getCallEventDetail().addAll(generatedEvents);
        batch.setCallEventDetails(eventList);
        batch.setAuditControlInfo(buildAuditControlInfo(successfulCdrs));

        DataInterChange dic = new DataInterChange();
        dic.setTransferBatch(batch);
        return dic;
    }

    private NetworkInfo buildNetworkInfo() {
        NetworkInfo ni = new NetworkInfo();

        UtcTimeOffsetInfoList utcList = new UtcTimeOffsetInfoList();
        UtcTimeOffsetInfo utc = new UtcTimeOffsetInfo();
        UtcTimeOffsetCode utcCode = new UtcTimeOffsetCode();
        utcCode.value = BigInteger.valueOf(1);
        UtcTimeOffset utcOffset = new UtcTimeOffset();
        utcOffset.value = "+0000".getBytes();
        utc.setUtcTimeOffsetCode(utcCode);
        utc.setUtcTimeOffset(utcOffset);
        utcList.getUtcTimeOffsetInfo().add(utc);
        ni.setUtcTimeOffsetInfo(utcList);

        RecEntityInfoList recList = new RecEntityInfoList();
        RecEntityInformation recInfo = new RecEntityInformation();
        RecEntityCode recCode = new RecEntityCode();
        recCode.value = BigInteger.valueOf(1);
        RecEntityType recType = new RecEntityType();
        recType.value = BigInteger.valueOf(1);
        RecEntityId recId = new RecEntityId();
        recId.value = ourTadig.getBytes();
        recInfo.setRecEntityCode(recCode);
        recInfo.setRecEntityType(recType);
        recInfo.setRecEntityId(recId);
        recList.getRecEntityInformation().add(recInfo);
        ni.setRecEntityInfo(recList);

        return ni;
    }

    private BatchControlInfo buildBatchControlInfo(Partner partner, int sequenceNo) {
        BatchControlInfo bci = new BatchControlInfo();

        Sender sender = new Sender();
        sender.value = String.format("%-5s", ourTadig).substring(0, 5).getBytes();
        bci.setSender(sender);

        Recipient recipient = new Recipient();
        String tadig = partner.getTadigCode() != null ? partner.getTadigCode() : "XXXXX";
        recipient.value = String.format("%-5s", tadig).substring(0, 5).getBytes();
        bci.setRecipient(recipient);

        FileSequenceNumber seqNum = new FileSequenceNumber();
        seqNum.value = String.format("%05d", sequenceNo).getBytes();
        bci.setFileSequenceNumber(seqNum);

        SpecificationVersionNumber specVersion = new SpecificationVersionNumber();
        specVersion.value = BigInteger.valueOf(3);
        bci.setSpecificationVersionNumber(specVersion);

        ReleaseVersionNumber releaseVersion = new ReleaseVersionNumber();
        releaseVersion.value = BigInteger.valueOf(12);
        bci.setReleaseVersionNumber(releaseVersion);

        return bci;
    }

    private AccountingInfo buildAccountingInfo(Partner partner) {
        AccountingInfo ai = new AccountingInfo();

        TapDecimalPlaces decimalPlaces = new TapDecimalPlaces();
        decimalPlaces.value = BigInteger.valueOf(TAP_DECIMAL_PLACES);
        ai.setTapDecimalPlaces(decimalPlaces);

        if (partner.getBillingCurrency() != null) {
            LocalCurrency currency = new LocalCurrency();
            currency.value = partner.getBillingCurrency().getBytes();
            ai.setLocalCurrency(currency);
        }

        return ai;
    }

    private AuditControlInfo buildAuditControlInfo(List<RateableCdr> cdrs) {
        AuditControlInfo aci = new AuditControlInfo();

        CallEventDetailsCount count = new CallEventDetailsCount();
        count.value = BigInteger.valueOf(cdrs.size());
        aci.setCallEventDetailsCount(count);

        TotalCharge totalCharge = new TotalCharge();
        totalCharge.value = calculateTotalRawCharge(cdrs);
        aci.setTotalCharge(totalCharge);

        return aci;
    }

    private CallEventDetail buildCallEventDetail(RateableCdr cdr, Partner partner) {
        try {
            String cdrServiceType = cdr.getServiceType() != null ? cdr.getServiceType().name() : null;
            TapProfile profile = (partner.getTapProfileGroup() != null && cdrServiceType != null)
                    ? partner.getTapProfileGroup().getTapProfiles().stream()
                            .filter(p -> cdrServiceType.equalsIgnoreCase(p.getServiceType()))
                            .findFirst()
                            .orElse(null)
                    : null;
            
            if (profile != null) {
                profile = tapProfileRepository.findByIdWithFieldMappings(profile.getId()).orElse(profile);
            }

            if (profile == null) {
                Long cdrId = resolveCdrId(cdr);
                log.warn("No TAP profile found for serviceType={} in group={}, skipping CDR id={}",
                        cdrServiceType,
                        partner.getTapProfileGroup() != null ? partner.getTapProfileGroup().getName() : "null",
                        cdrId);
                return null;
            }

            CallType callType = cdr.getCallType() != null ? cdr.getCallType() : CallType.MO_VOICE;

            Object eventObj;
            String eventSetterName;
            switch (callType) {
                case MT_VOICE -> { eventObj = new MobileTerminatedCall();  eventSetterName = "setMobileTerminatedCall"; }
                case GPRS     -> { eventObj = new GprsCall();              eventSetterName = "setGprsCall"; }
                case MO_SMS, MT_SMS -> { eventObj = new MobileSession();   eventSetterName = "setMobileSession"; }
                default       -> { eventObj = new MobileOriginatedCall();  eventSetterName = "setMobileOriginatedCall"; }
            }

            for (TapProfileFieldMapping profileMapping : profile.getFieldMappings()) {
                TapFieldMapping master = profileMapping.getTapFieldMapping();

                // MO_SMS and MT_SMS both map to MobileSession — treat their field mappings as interchangeable
                CallType masterCallType = master.getCallType();
                if (masterCallType != null && masterCallType != callType) {
                    boolean isSmsEquivalent = (callType == CallType.MO_SMS || callType == CallType.MT_SMS)
                            && (masterCallType == CallType.MO_SMS || masterCallType == CallType.MT_SMS);
                    if (!isSmsEquivalent) continue;
                }

                // Resolve value: CDR property first, then profile override, then master default.
                // outSourceColumn may be null for default-only fields (e.g. basicServiceCode, chargedItem,
                // exchangeRateCode, causeForTerm, taxCode, taxValue) — these must still be written.
                Object sourceVal = null;
                if (master.getOutSourceColumn() != null) {
                    sourceVal = resolver.getPropertyValue(cdr, master.getOutSourceColumn());
                }
                if (sourceVal == null) {
                    sourceVal = profileMapping.getCustomDefaultValue() != null
                            ? profileMapping.getCustomDefaultValue()
                            : master.getDefaultValue();
                }

                // Mandatory check
                boolean isMandatory = profileMapping.getIsMandatoryOverride() != null
                        ? profileMapping.getIsMandatoryOverride()
                        : Boolean.TRUE.equals(master.getIsMandatory());
                if (sourceVal == null && isMandatory) {
                    Long cdrId = resolveCdrId(cdr);
                    throw new IllegalArgumentException(String.format(
                            "Mandatory field [%s] (path: %s) missing on %s id=%s, partner=%s",
                            master.getFieldName(), master.getAsnPath(),
                            cdr.getClass().getSimpleName(), cdrId, partner.getPartnerCode()));
                }

                if (sourceVal != null) {
                    Object formatted = resolver.formatValue(sourceVal, master.getDataType(), TAP_DECIMAL_PLACES);
                    resolver.setValue(eventObj, master.getAsnPath(), formatted);
                }
            }

            CallEventDetail event = new CallEventDetail();
            Method setter = CallEventDetail.class.getMethod(eventSetterName, eventObj.getClass());
            setter.invoke(event, eventObj);
            return event;

        } catch (Exception e) {
            Long cdrId = resolveCdrId(cdr);
            log.error("Failed to build CallEventDetail for {} id={}, partner={}: {}",
                    cdr.getClass().getSimpleName(), cdrId, partner.getPartnerCode(), e.getMessage(), e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sequence management
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public int getNextSequence(Partner partner) {
        TapOutSequence seq = tapOutSequenceRepository
                .findByPartnerIdForUpdate(partner.getPartnerId())
                .orElseGet(() -> {
                    TapOutSequence newSeq = new TapOutSequence();
                    newSeq.setPartner(partner);
                    newSeq.setLastSequence(0);
                    return tapOutSequenceRepository.save(newSeq);
                });

        int nextSeq = seq.getLastSequence() + 1;
        seq.setLastSequence(nextSeq);
        seq.setUpdatedAt(LocalDateTime.now());
        tapOutSequenceRepository.save(seq);

        log.debug("TAP OUT sequence for partner={}: {}", partner.getPartnerCode(), nextSeq);
        return nextSeq;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File I/O
    // ─────────────────────────────────────────────────────────────────────────

    private Path writeToOutbox(DataInterChange dic, String fileName) {
        try {
            Path outboxPath = Paths.get(outboxDir);
            if (!Files.exists(outboxPath)) {
                Files.createDirectories(outboxPath);
                log.info("Created TAP OUT outbox directory: {}", outboxDir);
            }

            Path filePath = outboxPath.resolve(fileName);

            com.beanit.asn1bean.ber.ReverseByteArrayOutputStream reverseOS =
                    new com.beanit.asn1bean.ber.ReverseByteArrayOutputStream(8192);
            dic.encode(reverseOS);
            byte[] encodedBytes = reverseOS.getArray();

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath.toFile())) {
                fos.write(encodedBytes);
            }

            log.info("TAP OUT file written: {} ({} bytes)", filePath, encodedBytes.length);
            return filePath;

        } catch (IOException e) {
            log.error("Failed to write TAP OUT file {}: {}", fileName, e.getMessage(), e);
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TapFileRecord persistence
    // ─────────────────────────────────────────────────────────────────────────

    private TapFileRecord saveTapFileRecord(Partner partner, String fileName, Path filePath,
                                            int sequenceNo, int recordCount, BigInteger totalCharge,
                                            LocalDateTime processedAt) {
        TapFileRecord record = new TapFileRecord();
        record.setFileName(fileName);
        record.setFileType(TapFileType.TAP_OUT);
        record.setStatus(TapFileStatus.INVOICED);
        record.setPartner(partner);
        record.setFilePath(filePath.toString());
        record.setFileSequenceNo(sequenceNo);
        record.setSenderTadig(ourTadig);
        record.setRecipientTadig(partner.getTadigCode());
        record.setTotalRecords(BigInteger.valueOf(recordCount));
        record.setTotalCharge(totalCharge);
        record.setTapDecimalPlaces(BigInteger.valueOf(TAP_DECIMAL_PLACES));
        record.setLocalCurrency(partner.getBillingCurrency());
        record.setTapVersion(partner.getTapVersion());
        if (processedAt != null) {
            record.setProcessedAt(processedAt);
        }
        return tapFileRecordRepository.save(record);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ─────────────────────────────────────────────────────────────────────────

    private Long resolveCdrId(RateableCdr cdr) {
        if (cdr instanceof VoiceRatedCdr) return ((VoiceRatedCdr) cdr).getRatedCdrId();
        if (cdr instanceof SmsRatedCdr)   return ((SmsRatedCdr) cdr).getSmsRatedCdrId();
        if (cdr instanceof UsageRatedCdr) return ((UsageRatedCdr) cdr).getRatedCdrId();
        return null;
    }

    private String buildTapOutFileName(String senderTadig, String recipientTadig, int sequenceNo) {
        String sender = String.format("%-5s", senderTadig).substring(0, 5);
        String recipient = String.format("%-5s", recipientTadig).substring(0, 5);
        return String.format("TD%s%s%05d", sender, recipient, sequenceNo);
    }

    private String resolveOutgoingAccountId(Partner partner) {
        return accountRepository.findByPartnerPartnerId(partner.getPartnerId())
                .stream()
                .findFirst()
                .map(a -> a.getAccountCode())
                .orElse(null);
    }

    private BigInteger calculateTotalRawCharge(List<RateableCdr> cdrs) {
        BigDecimal total = cdrs.stream()
                .filter(c -> c.getOutgoingTotalCost() != null)
                .map(RateableCdr::getOutgoingTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.multiply(BigDecimal.TEN.pow(TAP_DECIMAL_PLACES)).toBigInteger();
    }

    private byte[] encodeBcd(String number) {
        if (number == null || number.isEmpty()) return new byte[0];
        String padded = number.length() % 2 == 0 ? number : number + "F";
        byte[] result = new byte[padded.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(padded.charAt(i * 2), 16);
            int low = Character.digit(padded.charAt(i * 2 + 1), 16);
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private LocalDateTime parseStartTime(String startTimeStr) {
        if (startTimeStr == null || startTimeStr.isBlank()) return null;
        String[] patterns = {
                "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };
        for (String pattern : patterns) {
            try {
                return LocalDateTime.parse(startTimeStr.trim(), DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {}
        }
        log.warn("TapOutFileGenerationService: could not parse startTime: {}", startTimeStr);
        return null;
    }
}
