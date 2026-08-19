package com.xcess.ocs.controller;

import com.xcess.ocs.kafka.KafkaProducer;
import com.xcess.ocs.kafka.MessageProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/test/kafka")
public class KafkaTestController {

    @Autowired
    public MessageProcessor messageProcessor;

    @PostMapping("/publish")
    public ResponseEntity<String> publish(
            @RequestParam(defaultValue = "cdr-topic-india") String topic,
            @RequestParam String callingNumber,
            @RequestParam String calledNumber,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String incomingAccountId,
            @RequestParam String outgoingAccountId) {

        String message = String.format("%s,%s,%s,%s,%s,%s",
                callingNumber, calledNumber, incomingAccountId, outgoingAccountId, startTime, endTime);
        messageProcessor.processMessage(topic, message);
        log.info("✓ INTERCONNECT CDR sent for processing : {}", message);
        return ResponseEntity.ok("✓ CDR sent to topic: " + topic + "\nMessage: " + message);
    }

    @PostMapping("/publish-roaming")
    public ResponseEntity<String> publishRoaming(
            @RequestParam(defaultValue = "cdr-topic-roaming") String topic,
            @RequestParam String callingNumber,
            @RequestParam String calledNumber,
            @RequestParam String eventNos,
            @RequestParam String homePlmn,
            @RequestParam String visitedPlmn,
            @RequestParam String incomingAccountId,
            @RequestParam String outgoingAccountId,
            @RequestParam String serviceType,
            @RequestParam String callType) {

        String message = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                callingNumber, calledNumber, eventNos, homePlmn, visitedPlmn, incomingAccountId, outgoingAccountId, serviceType, callType);
        messageProcessor.processMessage(topic, message);
        log.info("✓ ROAMING CDR sent for processing : {}", message);
        return ResponseEntity.ok("✓ ROAMING CDR sent to topic: " + topic + "\nMessage: " + message);
    }

    @PostMapping("/publish-voice")
    public ResponseEntity<String> publishVoice(
            @RequestParam(defaultValue = "cdr-topic-voice") String topic,
            @RequestParam String callingNumber,
            @RequestParam String calledNumber,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String homePlmn,
            @RequestParam String visitedPlmn,
            @RequestParam String incomingAccountId,
            @RequestParam String outgoingAccountId,
            @RequestParam String serviceType,
            @RequestParam String callType) {

        String message = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                callingNumber, calledNumber, startTime, endTime, homePlmn, visitedPlmn, incomingAccountId, outgoingAccountId, serviceType, callType);
        messageProcessor.processMessage(topic, message);
        log.info("✓ VOICE CDR sent for processing : {}", message);
        return ResponseEntity.ok("✓ VOICE CDR sent to topic: " + topic + "\nMessage: " + message);
    }

    @PostMapping("/publish-usage")
    public ResponseEntity<String> publishUsage(
            @RequestParam(defaultValue = "cdr-topic-data") String topic,
            @RequestParam String subscriberIdentity,
            @RequestParam String accessPointName,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String incomingAccountId,
            @RequestParam String outgoingAccountId,
            @RequestParam String homePlmn,
            @RequestParam String visitedPlmn,
            @RequestParam String serviceType,
            @RequestParam String usage,
            @RequestParam(defaultValue = "0") String uploadUsage,
            @RequestParam(defaultValue = "0") String downloadUsage,
            @RequestParam String measurementUnit) {

        String message = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                subscriberIdentity, accessPointName, startTime, endTime, usage, measurementUnit, homePlmn, visitedPlmn, incomingAccountId, outgoingAccountId, serviceType);
        messageProcessor.processMessage(topic, message);
        log.info("✓ USAGE CDR sent for processing : {}", message);
        return ResponseEntity.ok("✓ USAGE CDR sent to topic: " + topic + "\nMessage: " + message);
    }
}
