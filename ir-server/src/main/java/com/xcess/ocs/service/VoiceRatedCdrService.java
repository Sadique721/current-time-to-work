package com.xcess.ocs.service;

import com.xcess.ocs.dto.VoiceRatedCdrDTO;
import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.repository.VoiceRatedCdrRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VoiceRatedCdrService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceRatedCdrService.class);

    private final VoiceRatedCdrRepository voiceRatedCdrRepository;

    public VoiceRatedCdrService(VoiceRatedCdrRepository voiceRatedCdrRepository) {
        this.voiceRatedCdrRepository = voiceRatedCdrRepository;
    }

    /**
     * NEW METHOD: Save an enhanced VoiceRatedCdr entity with rating information
     * This method handles the new flow where CDRs come pre-rated from the rating engine
     */
    public VoiceRatedCdr saveRatedCdr(VoiceRatedCdr voiceRatedCdr) {
        try {
            VoiceRatedCdr savedEntity = voiceRatedCdrRepository.save(voiceRatedCdr);

            // Enhanced logging with rating information
            logger.info("Saved VoiceRatedCdr: id={}, calling={}, called={}, inRate={}, outRate={}, duration={}",
                    savedEntity.getRatedCdrId(),
                    savedEntity.getCallingNumber(),
                    savedEntity.getCalledNumber(),
                    savedEntity.getIncomingAppliedRate(),
                    savedEntity.getOutgoingAppliedRate(),
                    savedEntity.getDurationSeconds());

            return savedEntity;

        } catch (Exception e) {
            logger.error("Failed to save enhanced VoiceRatedCdr: calling={}, called={}, " +
                        "incoming ratingStatus={}, outGoing ratingStatus={}, error={}",
                    voiceRatedCdr.getCallingNumber(),
                    voiceRatedCdr.getCalledNumber(),
                    voiceRatedCdr.getIncomingRatingStatus(),
                    voiceRatedCdr.getOutgoingRatingStatus(),
                    e.getMessage(), e);
            throw e;
        }
    }

    /**
     * LEGACY METHOD: Save CDR without rating (kept for backward compatibility)
     * NOTE: This method is now deprecated in favor of the rating-integrated flow.
     * start_time / end_time are stored as LocalDateTime — parse the raw DTO strings here.
     */
    @Deprecated
    public void saveCdr(VoiceRatedCdrDTO record, Long sourceId) {
        try {
            VoiceRatedCdr entity = new VoiceRatedCdr();
            // start_time / end_time are now LocalDateTime on the entity — parse the DTO Strings
            entity.setStartTime(parseTimestamp(record.getStartTime()));
            entity.setEndTime(parseTimestamp(record.getEndTime()));
            entity.setCallingNumber(record.getCallingNumber());
            entity.setCalledNumber(record.getCalledNumber());
            entity.setIncomingAccountId(record.getIncomingAccountId());
            entity.setOutgoingAccountId(record.getOutgoingAccountId());
            entity.setSourceId(sourceId);

            // Mark as pending since no rating was applied
            entity.markAsPending();

            voiceRatedCdrRepository.save(entity);

            // Console output
            System.out.println("Saved VoiceRatedCdr (LEGACY - NO RATING): " + entity);

            // Production-grade logging
            logger.warn("Saved VoiceRatedCdr entity using LEGACY method (no rating applied) for sourceId {}: {}",
                       sourceId, entity);

        } catch (Exception e) {
            //  Console error
            System.err.println(" Error saving VoiceRatedCdr: " + e.getMessage());

            // Production-grade error logging
            logger.error(" Failed to save VoiceRatedCdr for sourceId {}. Error: {}", sourceId, e.getMessage(), e);

            throw e;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static final java.time.format.DateTimeFormatter[] TIMESTAMP_FORMATTERS = {
        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private java.time.LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) return null;
        String clean = timestampStr.trim();
        for (java.time.format.DateTimeFormatter fmt : TIMESTAMP_FORMATTERS) {
            try { return java.time.LocalDateTime.parse(clean, fmt); }
            catch (java.time.format.DateTimeParseException ignored) {}
        }
        logger.warn("Legacy saveCdr: unable to parse timestamp '{}'", timestampStr);
        return null;
    }
}
