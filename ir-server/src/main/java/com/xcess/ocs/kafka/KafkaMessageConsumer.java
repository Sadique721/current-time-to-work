package com.xcess.ocs.kafka;

import com.xcess.ocs.cache.SourceConfigurationCache;
import com.xcess.ocs.dto.SourceConfigurationDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Kafka consumer component that listens to CDR messages from configured topics.
 * This class manages the lifecycle of a Kafka consumer, handles dynamic subscription
 * to topics based on enabled source configurations, and delegates message processing
 * to the CdrMessageProcessor.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageConsumer {

    // Dependency for retrieving source configurations with topic information
    private final SourceConfigurationCache sourceConfigCache;
    
    // Service responsible for processing CDR messages
    private final MessageProcessor messageProcessor;

    // Kafka bootstrap servers configuration from application properties
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    // Kafka consumer group ID from application properties
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // The Kafka consumer instance
    private KafkaConsumer<String, String> consumer;
    
    // Thread-safe flag to control the consumer thread's running state
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Thread for running the Kafka consumer
    private Thread consumerThread;
    
    // Lock object for thread synchronization
    private final Object consumerLock = new Object();

    /**
     * Initializes the Kafka consumer and starts the consumer thread.
     * Called automatically by Spring after bean creation.
     */
    @PostConstruct
    public void init() {
        // Configure Kafka consumer properties
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", groupId);
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");

        // Create the Kafka consumer with the configured properties
        this.consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
        this.running.set(true);

        // Start a dedicated thread for consuming messages
        this.consumerThread = new Thread(this::consumeMessages);
        this.consumerThread.start();

        log.info("Kafka consumer initialized and started");
    }

    /**
     * Stops the consumer thread and closes the Kafka consumer.
     * Called automatically by Spring during application shutdown.
     */
    @PreDestroy
    public void cleanup() {
        this.running.set(false);
        
        // Wait for consumer thread to finish
        if (consumerThread != null) {
            try {
                consumerThread.join(5000);
            } catch (InterruptedException e) {
                log.warn("Interrupted while waiting for consumer thread to stop", e);
                Thread.currentThread().interrupt();
            }
        }
        
        // Close the Kafka consumer
        synchronized (consumerLock) {
            if (consumer != null) {
                consumer.close();
                consumer = null;
            }
        }
        log.info("Kafka consumer closed");
    }

    /**
     * Periodically refreshes the topic subscriptions based on enabled source configurations.
     * This method is called automatically by Spring at the configured interval.
     */
    @Scheduled(fixedDelayString = "${kafka.subscription.refresh.interval:600000}")
    public void refreshSubscriptions() {
        // Synchronize access to the consumer to prevent concurrent modification
        synchronized (consumerLock) {
            if (consumer == null) return;

            // Get the set of currently enabled topics from source configurations
            Set<String> enabledTopics = getEnabledTopics();

            if (!enabledTopics.isEmpty()) {
                // Only resubscribe if the topics have changed from current subscription
                if (!consumer.subscription().equals(enabledTopics)) {
                    consumer.subscribe(enabledTopics);
                    log.info("Subscribed to topics: {}", enabledTopics);
                }
            } else {
                // If no enabled topics are found, unsubscribe from all topics
                consumer.unsubscribe();
                log.warn("No enabled topics found. Not subscribing to any topics.");
            }
        }
    }

    /**
     * Gets the set of currently enabled topic names from the source configuration cache.
     * Only topics from source configurations with status="enabled" are included.
     * 
     * @return Set of enabled topic names
     */
    private Set<String> getEnabledTopics() {
        List<SourceConfigurationDTO> allConfigs = sourceConfigCache.getAllConfigurations();
        return allConfigs.stream()
                .filter(config -> "enabled".equalsIgnoreCase(config.getStatus()))
                .map(SourceConfigurationDTO::getTopicName)
                .collect(Collectors.toSet());
    }

    /**
     * Main consumer loop that runs in a dedicated thread.
     * Continuously polls for messages from subscribed topics and
     * delegates processing to the CdrMessageProcessor.
     */
    private void consumeMessages() {
        try {
            // Initial subscription to enabled topics
            refreshSubscriptions();

            // Main consumption loop
            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records;

                    // Synchronize access to the consumer during polling
                    synchronized (consumerLock) {
                        if (!consumer.subscription().isEmpty()) {
                            // Poll for new messages with a short timeout
                            records = consumer.poll(Duration.ofMillis(100));

                            // Process each received record
                            for (ConsumerRecord<String, String> record : records) {
                                messageProcessor.processMessage(record.topic(), record.value());
                            }
                        } else {
                            // Sleep to avoid tight loop when no topics are subscribed
                            Thread.sleep(1000);
                        }
                    }
                } catch (InterruptedException e) {
                    log.warn("Consumer thread interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error consuming messages", e);
                    // Continue the loop to attempt recovery
                }
            }
        } catch (Exception e) {
            log.error("Fatal error in consumer thread", e);
        } finally {
            log.info("Exiting consumer thread");
        }
    }
}