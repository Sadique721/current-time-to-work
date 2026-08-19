package com.xcess.ocs.controller;

import com.xcess.ocs.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/kafka")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);


    @Autowired
    private KafkaProducer kafkaProducer;

//    @PostMapping("/publish")
//    public ResponseEntity<String> publish(@RequestParam String topic, @RequestParam String message) {
//        kafkaProducer.sendMessage(message, topic);
//        return ResponseEntity.ok("Message sent to Kafka topic");
//    }


    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody Map<String, String> body) {
        String topic = body.get("topic");
        String message = body.get("message");

        logger.info("Received request to publish message to Kafka");
        logger.debug("Topic: {}", topic);
        logger.debug("Message: {}", message);

        try {
            kafkaProducer.sendMessage(message, topic);
            logger.info("Message successfully sent to topic '{}'", topic);
            return ResponseEntity.ok("Message sent to Kafka topic");
        } catch (Exception e) {
            logger.error("Failed to send message to Kafka topic '{}'", topic, e);
            return ResponseEntity.status(500).body("Failed to send message");
        }
    }
}

