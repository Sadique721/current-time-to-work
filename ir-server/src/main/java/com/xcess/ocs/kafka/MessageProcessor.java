package com.xcess.ocs.kafka;

import com.xcess.ocs.cache.SourceCdrConfigurationCache;
import com.xcess.ocs.cache.SourceConfigurationCache;
import com.xcess.ocs.dto.VoiceRatedCdrDTO;
import com.xcess.ocs.dto.SmsRatedCdrDTO;
import com.xcess.ocs.dto.UsageRatedCdrDTO;
import com.xcess.ocs.dto.SourceCdrConfigurationDTO;
import com.xcess.ocs.dto.SourceConfigurationDTO;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.ratingengine.service.CdrRatingIntegrationService;

import com.xcess.ocs.service.VoiceRatedCdrService;
import com.xcess.ocs.service.SmsRatedCdrService;
import com.xcess.ocs.service.UsageRatedCdrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for processing CDR (Call Detail Record) messages from Kafka topics.
 * This class handles the business logic of:
 * 1. Parsing raw messages from Kafka
 * 2. Mapping fields based on configured sequences
 * 3. Converting raw data to structured DTO objects
 * 4. RATING ENGINE INTEGRATION: Applying rates using RadixTrie algorithm
 * 5. Persisting enhanced rated records to the database
 *
 * FLOW: Kafka → Parse → Rate Lookup → Rate Application → Enhanced Database Storage
 * Supports both INTERCONNECT and ROAMING line of business.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageProcessor {

    // Service for persisting rated CDRs to the database
    private final VoiceRatedCdrService voiceRatedCdrService;
    private final SmsRatedCdrService smsRatedCdrService;
    private final UsageRatedCdrService usageRatedCdrService;

    // NEW: Rating integration service that bridges CDR processing and rating engine
    private final CdrRatingIntegrationService cdrRatingIntegrationService;
    
    // Cache for accessing source configurations without database queries
    private final SourceConfigurationCache sourceConfigCache;

    // Cache for accessing CDR field configurations without database queries
    private final SourceCdrConfigurationCache sourceCdrConfigCache;

    /**
     * Processes a single message from a Kafka topic.
     * This is the main entry point called by the Kafka consumer for each received message.
     * 
     * @param topicName The name of the topic the message came from
     * @param message The message content (typically a comma-separated string)
     */
    public void processMessage(String topicName, String message) {
        try {
            log.info("Received message for topic: {}", topicName);
            log.debug("Raw message: {}", message);

            // Step1: Get source configuration by topic name
            // This should always be valid since we only subscribe to enabled topics
            SourceConfigurationDTO sourceConfig = sourceConfigCache.getConfigurationByTopicName(topicName);
            if (sourceConfig == null) {
                log.error("Source configuration not found for topic: {}. This should not happen as we only subscribe to enabled topics.", topicName);
                return;
            }

            Long sourceId = sourceConfig.getSourceId();
            LineOfBusiness lob = detectLineOfBusiness(topicName);
            log.debug("Processing message for topic: {} with sourceId: {} and LOB: {}", topicName, sourceId, lob);

            // Step2: Fetch field mappings from cache
            List<SourceCdrConfigurationDTO> fieldConfigs = sourceCdrConfigCache.getConfigurationsBySourceId(sourceId);
            if (fieldConfigs.isEmpty()) {
                log.error("No field configuration found for source ID: {}                         ", sourceId);
                return;
            }

            // Step3: Validate field configurations for duplicate sequences
            // This is important to ensure correct parsing
            Map<Integer, Long> sequenceCounts = fieldConfigs.stream()
                    .collect(Collectors.groupingBy(SourceCdrConfigurationDTO::getSequence, Collectors.counting()));

            Set<Integer> duplicateSequences = sequenceCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            if (!duplicateSequences.isEmpty()) {
                log.warn("Duplicate sequences found for sourceId {}: {}", sourceId, duplicateSequences);
            }

            // Step4: Sort fields by sequence number to determine the correct order
            List<String> fieldOrder = fieldConfigs.stream()
                    .sorted(Comparator.comparing(SourceCdrConfigurationDTO::getSequence))
                    .map(SourceCdrConfigurationDTO::getFieldName)
                    .toList();
            log.debug("Field order for topic {}: {}", topicName, fieldOrder);

            // Step5 & 6 & 7: Parse and Apply rating based on Service Type and Line of Business, then Save
            ServiceType serviceType = sourceConfig.getServiceType();
            if (serviceType == ServiceType.VOICE) {
                VoiceRatedCdrDTO voiceCdrRecord = parseMessage(message, fieldOrder);
                voiceCdrRecord.setLineOfBusiness(lob);
                log.info("Parsed VOICE message for topic [{}] and sourceId [{}] with LOB [{}]", topicName, sourceId, lob);

                VoiceRatedCdr voiceRatedCdrEntity = cdrRatingIntegrationService.processAndRateCdr(voiceCdrRecord, sourceId, lob);
                log.info("Applied rating for VOICE CDR: calling={}, called={}, LOB={}, status={}",
                        voiceRatedCdrEntity.getCallingNumber(),
                        voiceRatedCdrEntity.getCalledNumber(),
                        lob,
                        voiceRatedCdrEntity.getIncomingRatingStatus());
                voiceRatedCdrService.saveRatedCdr(voiceRatedCdrEntity);
                log.info("Saved enhanced VoiceRatedCdr for topic [{}] with LOB [{}] - incoming: {} outgoing: {}",
                        topicName, lob, voiceRatedCdrEntity.getIncomingRatingStatus(), voiceRatedCdrEntity.getOutgoingRatingStatus());
            } else if (serviceType == ServiceType.SMS) {
                SmsRatedCdrDTO smsCdrRecord = parseSmsMessage(message, fieldOrder);
                smsCdrRecord.setLineOfBusiness(lob);
                log.info("Parsed SMS message for topic [{}] and sourceId [{}] with LOB [{}]", topicName, sourceId, lob);

                SmsRatedCdr smsRatedCdrEntity = cdrRatingIntegrationService.processAndRateSmsCdr(smsCdrRecord, sourceId, lob);
                log.info("Applied rating for SMS CDR: calling={}, called={}, LOB={}, status={}",
                        smsRatedCdrEntity.getCallingNumber(),
                        smsRatedCdrEntity.getCalledNumber(),
                        lob,
                        smsRatedCdrEntity.getIncomingRatingStatus());
                smsRatedCdrService.saveRatedCdr(smsRatedCdrEntity);
                log.info("Saved enhanced SmsRatedCdr for topic [{}] with LOB [{}] - incoming: {} outgoing: {}",
                        topicName, lob, smsRatedCdrEntity.getIncomingRatingStatus(), smsRatedCdrEntity.getOutgoingRatingStatus());
            } else if (serviceType == ServiceType.USAGE) {
                UsageRatedCdrDTO usageCdrRecord = parseUsageMessage(message, fieldOrder);
                usageCdrRecord.setLineOfBusiness(lob);
                log.info("Parsed USAGE message for topic [{}] and sourceId [{}] with LOB [{}]", topicName, sourceId, lob);

                UsageRatedCdr usageRatedCdrEntity = cdrRatingIntegrationService.processAndRateUsageCdr(usageCdrRecord, sourceId, lob);
                log.info("Applied rating for USAGE CDR: subscriber={}, APN={}, LOB={}, status={}",
                        usageRatedCdrEntity.getSubscriberIdentity(),
                        usageRatedCdrEntity.getAccessPointName(),
                        lob,
                        usageRatedCdrEntity.getIncomingRatingStatus());
                usageRatedCdrService.saveRatedCdr(usageRatedCdrEntity);
                log.info("Saved enhanced UsageRatedCdr for topic [{}] with LOB [{}] - incoming: {} outgoing: {}",
                        topicName, lob, usageRatedCdrEntity.getIncomingRatingStatus(), usageRatedCdrEntity.getOutgoingRatingStatus());
            } else {
                log.warn("Unknown service type: {} for topic: {}", serviceType, topicName);
            }

        } catch (Exception e) {
            // Comprehensive error handling with detailed logging
            log.error("Failed to process message for topic [{}]: {}", topicName, e.getMessage(), e);
        }
    }

    /**
     * Detect Line of Business based on topic naming convention only.
     * No longer uses configured LOB from source configuration.
     * 
     * @param topicName The Kafka topic name
     * @return The detected LineOfBusiness
     */
    private LineOfBusiness detectLineOfBusiness(String topicName) {
        // Topic naming convention only
        String lowerTopicName = topicName.toLowerCase();
        if (lowerTopicName.contains("roaming")) {
            return LineOfBusiness.ROAMING;
        } else if (lowerTopicName.contains("interconnect")) {
            return LineOfBusiness.INTERCONNECT;
        }
        
        // Default fallback
        log.warn("Could not determine LOB for topic: {}, defaulting to INTERCONNECT", topicName);
        return LineOfBusiness.INTERCONNECT;
    }

    /**
     * Parses a comma-separated message into a VoiceRatedCdrDTO object based on the field order.
     * This method maps each field in the raw message to the appropriate property in the DTO
     * according to the configured field name. Supports VOICE, SMS, and USAGE service types.
     *
     * @param message    The comma-separated message to parse
     * @param fieldOrder The ordered list of field names based on their sequence numbers
     * @return A populated VoiceRatedCdrDTO object with values extracted from the message
     * @throws IllegalArgumentException if the message format doesn't match the expected configuration
     */
    private VoiceRatedCdrDTO parseMessage(String message, List<String> fieldOrder) {
        // Split the message into individual fields
        String[] fields = message.split(",");
        
        // Validate that the number of fields matches the configuration
//        if (fields.length != fieldOrder.size()) {
//            throw new IllegalArgumentException("Message field count does not match configuration: " +
//                    "expected " + fieldOrder.size() + " fields but got " + fields.length);
//        }

        // Create a new DTO to populate
        VoiceRatedCdrDTO cdrRecord = new VoiceRatedCdrDTO();
        
        // Map each field value to the appropriate property based on field name
        for (int i = 0; i < fieldOrder.size(); i++) {
            String value = fields[i].trim();
            // Use switch expression (Java 14+) for cleaner mapping
            switch (fieldOrder.get(i).toUpperCase()) {
                case "CALLING_NUMBER" -> cdrRecord.setCallingNumber(value);
                case "CALLED_NUMBER" -> cdrRecord.setCalledNumber(value);
                case "START_TIME" -> cdrRecord.setStartTime(value);
                case "END_TIME" -> cdrRecord.setEndTime(value);
                // INTERCONNECT/ROAMING common fields
                case "INCOMING_ACCOUNT_ID" -> cdrRecord.setIncomingAccountId(value);
                case "OUTGOING_ACCOUNT_ID" -> cdrRecord.setOutgoingAccountId(value);
                // ROAMING fields
                case "HOME_PLMN" -> cdrRecord.setHomePlmn(value);
                case "VISITED_PLMN" -> cdrRecord.setVisitedPlmn(value);
                case "CALL_TYPE" -> {
                    try {
                        cdrRecord.setCallType(com.xcess.ocs.roaming.entity.CallType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown CALL_TYPE value '{}', skipping", value);
                    }
                }
                case "SERVICE_TYPE" -> {
                    try {
                        cdrRecord.setServiceType(ServiceType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown SERVICE_TYPE value '{}', skipping", value);
                    }
                }
                // SMS field
                case "EVENT_NOS" -> {
                    try {
                        cdrRecord.setEventNos(value);
                    } catch (NumberFormatException e) {
                        log.warn("SMS parse: invalid MESSAGE_COUNT '{}', skipping", value);
                    }
                }
                case "USAGE" -> {
                    try {
                        cdrRecord.setTotalUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid USAGE value '{}', skipping", value);
                    }
                }
                case "UPLOAD_USAGE" -> {
                    try {
                        cdrRecord.setUploadUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid UPLOAD_USAGE value '{}', skipping", value);
                    }
                }
                case "DOWNLOAD_USAGE" -> {
                    try {
                        cdrRecord.setDownloadUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid DOWNLOAD_USAGE value '{}', skipping", value);
                    }
                }
                case "MEASURMENT_UNIT" -> cdrRecord.setMeasurementUnit(value);
            }
        }
        return cdrRecord;
    }

    /**
     * Parses a comma-separated message into an SmsRatedCdrDTO object based on the field order.
     * This method maps each field in the raw message to the appropriate property in the DTO
     * according to the configured field name.
     *
     * @param message    The comma-separated message to parse
     * @param fieldOrder The ordered list of field names based on their sequence numbers
     * @return A populated SmsRatedCdrDTO object with values extracted from the message
     */
    private SmsRatedCdrDTO parseSmsMessage(String message, List<String> fieldOrder) {
        // Split the message into individual fields
        String[] fields = message.split(",");
        
        // Create a new DTO to populate
        SmsRatedCdrDTO cdrRecord = new SmsRatedCdrDTO();
        
        // Map each field value to the appropriate property based on field name
        for (int i = 0; i < fieldOrder.size(); i++) {
            if (i >= fields.length) {
                break;
            }
            String value = fields[i].trim();
            switch (fieldOrder.get(i).toUpperCase()) {
                case "CALLING_NUMBER" -> cdrRecord.setCallingNumber(value);
                case "CALLED_NUMBER" -> cdrRecord.setCalledNumber(value);
                case "START_TIME" -> cdrRecord.setStartTime(value);
                case "END_TIME" -> cdrRecord.setEndTime(value);
                // INTERCONNECT/ROAMING common fields
                case "INCOMING_ACCOUNT_ID" -> cdrRecord.setIncomingAccountId(value);
                case "OUTGOING_ACCOUNT_ID" -> cdrRecord.setOutgoingAccountId(value);
                // ROAMING fields
                case "HOME_PLMN" -> cdrRecord.setHomePlmn(value);
                case "VISITED_PLMN" -> cdrRecord.setVisitedPlmn(value);
                case "CALL_TYPE" -> {
                    try {
                        cdrRecord.setCallType(com.xcess.ocs.roaming.entity.CallType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("SMS parse: Unknown CALL_TYPE value '{}', skipping", value);
                    }
                }
                case "SERVICE_TYPE" -> {
                    try {
                        cdrRecord.setServiceType(ServiceType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("SMS parse: Unknown SERVICE_TYPE value '{}', skipping", value);
                    }
                }
                // SMS fields
                case "EVENT_NOS", "MESSAGE_COUNT" -> {
                    cdrRecord.setEventNos(value);
                }
            }
        }
        return cdrRecord;
    }

    /**
     * Parses a comma-separated message into an UsageRatedCdrDTO object based on the field order.
     */
    private UsageRatedCdrDTO parseUsageMessage(String message, List<String> fieldOrder) {
        String[] fields = message.split(",");
        UsageRatedCdrDTO cdrRecord = new UsageRatedCdrDTO();
        
        for (int i = 0; i < fieldOrder.size(); i++) {
            if (i >= fields.length) {
                break;
            }
            String value = fields[i].trim();
            switch (fieldOrder.get(i).toUpperCase()) {
                case "SUBSCRIBER_IDENTITY" -> cdrRecord.setSubscriberIdentity(value);
                case "ACCESS_POINT_NAME" -> cdrRecord.setAccessPointName(value);
                case "START_TIME" -> cdrRecord.setStartTime(value);
                case "END_TIME" -> cdrRecord.setEndTime(value);
                case "INCOMING_ACCOUNT_ID" -> cdrRecord.setIncomingAccountId(value);
                case "OUTGOING_ACCOUNT_ID" -> cdrRecord.setOutgoingAccountId(value);
                case "HOME_PLMN" -> cdrRecord.setHomePlmn(value);
                case "VISITED_PLMN" -> cdrRecord.setVisitedPlmn(value);
                case "SERVICE_TYPE" -> {
                    try {
                        cdrRecord.setServiceType(ServiceType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("USAGE parse: Unknown SERVICE_TYPE value '{}', skipping", value);
                    }
                }
                case "USAGE" -> {
                    try {
                        cdrRecord.setTotalUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid USAGE value '{}', skipping", value);
                    }
                }
                case "UPLOAD_USAGE" -> {
                    try {
                        cdrRecord.setUploadUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid UPLOAD_USAGE value '{}', skipping", value);
                    }
                }
                case "DOWNLOAD_USAGE" -> {
                    try {
                        cdrRecord.setDownloadUsage(Double.valueOf(value));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid DOWNLOAD_USAGE value '{}', skipping", value);
                    }
                }
                case "MEASURMENT_UNIT" -> cdrRecord.setMeasurementUnit(value);
            }
        }
        return cdrRecord;
    }

    /**
     * SMS fields:
     * CALLING_NUMBER, CALLED_NUMBER, START_TIME, MESSAGE_COUNT,
     * INCOMING_ACCOUNT_ID, OUTGOING_ACCOUNT_ID (INTERCONNECT)
     * HOME_PLMN, VISITED_PLMN, CALL_TYPE (ROAMING)
     * END_TIME is not a valid SMS field — warned and skipped.
     */
    private void parseSmsFields(String[] fields, List<String> fieldOrder, VoiceRatedCdrDTO cdrRecord) {
        int limit = Math.min(fields.length, fieldOrder.size());
        for (int i = 0; i < limit; i++) {
            String value = fields[i].trim();
            switch (fieldOrder.get(i).toUpperCase()) {
                case "CALLING_NUMBER"      -> cdrRecord.setCallingNumber(value);
                case "CALLED_NUMBER"       -> cdrRecord.setCalledNumber(value);
                case "START_TIME"          -> cdrRecord.setStartTime(value);
                case "INCOMING_ACCOUNT_ID" -> cdrRecord.setIncomingAccountId(value);
                case "OUTGOING_ACCOUNT_ID" -> cdrRecord.setOutgoingAccountId(value);
                case "HOME_PLMN"           -> cdrRecord.setHomePlmn(value);
                case "VISITED_PLMN"        -> cdrRecord.setVisitedPlmn(value);
                case "MESSAGE_COUNT" -> {
                    try {
                        cdrRecord.setEventNos(value);
                    } catch (NumberFormatException e) {
                        log.warn("SMS parse: invalid MESSAGE_COUNT '{}', skipping", value);
                    }
                }
                case "CALL_TYPE" -> {
                    try {
                        cdrRecord.setCallType(com.xcess.ocs.roaming.entity.CallType.valueOf(value.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("SMS parse: unknown CALL_TYPE '{}', skipping", value);
                    }
                }
                case "END_TIME" -> log.warn("SMS parse: END_TIME is not valid for SMS CDRs, skipping");
                default -> log.warn("SMS parse: unexpected field '{}' at position {}, skipping", fieldOrder.get(i), i);
            }
        }
    }
}
