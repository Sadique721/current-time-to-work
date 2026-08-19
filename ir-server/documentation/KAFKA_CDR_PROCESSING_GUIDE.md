# Kafka CDR Processing — Complete Developer Guide

> **Purpose:** This guide is written for developers working on the OCS (Online Charging System) codebase to understand the end-to-end flow of raw CDRs (Call Detail Records) arriving via Kafka, being parsed, dynamically mapped, rated, and saved to the database.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map](#2-component-map)
3. [Kafka Consumer & Scheduler](#3-kafka-consumer--scheduler)
4. [Message Processing & Field Mapping](#4-message-processing--field-mapping)
5. [Rating Engine Integration](#5-rating-engine-integration)
6. [Scenario Playbook — Positive & Negative Cases](#6-scenario-playbook--positive--negative-cases)
7. [Checklist & Troubleshooting for New Developers](#7-checklist--troubleshooting-for-new-developers)

---

## 1. System Overview

The Kafka CDR Processing pipeline is responsible for ingesting raw, unrated CDRs from external switches or network elements, converting them into internal DTOs based on dynamic database-driven field mappings, passing them to the Rating Engine, and persisting the rated result.

### Core Design Goals

| Goal | How It Is Achieved |
|---|---|
| **Dynamic Ingestion** | Kafka topics are dynamically subscribed to based on `SourceConfiguration` tables without restarting the app. |
| **Flexible Formats** | CDR formats (CSV field order) are mapped dynamically using `SourceCdrConfiguration` sequence numbers. |
| **Resiliency** | Parsing errors on individual fields or records log warnings/errors but do not crash the consumer loop. |
| **LOB Detection** | Line of Business (INTERCONNECT vs ROAMING) is automatically inferred from the topic name convention. |

---

## 2. Component Map

```text
Kafka Broker(s)
    │
    ▼
KafkaMessageConsumer (@Component)
    │   ├── poll(100ms) thread
    │   └── @Scheduled refreshSubscriptions() (every 10 mins)
    │
    ▼
MessageProcessor (@Service)
    │   ├── sourceConfigCache.getConfigurationByTopicName()
    │   ├── sourceCdrConfigCache.getConfigurationsBySourceId()
    │   ├── detectLineOfBusiness(topicName)
    │   └── parseMessage() / parseSmsMessage() / parseUsageMessage()
    │
    ▼
CdrRatingIntegrationService (@Service)
    │   └── processAndRateCdr() [Uses RadixTrie for prefix matching & rating]
    │
    ▼
VoiceRatedCdrService / SmsRatedCdrService / UsageRatedCdrService
    └── saveRatedCdr(entity) ──► Database (voice_rated_cdr, sms_rated_cdr, usage_rated_cdr)
```

---

## 3. Kafka Consumer & Scheduler

The `KafkaMessageConsumer` manages the connection to Kafka. It runs a dedicated, non-blocking background thread for polling messages and uses a Spring `@Scheduled` task to manage topic subscriptions.

### Dynamic Topic Subscription (The "Scheduler")
```java
@Scheduled(fixedDelayString = "${kafka.subscription.refresh.interval:600000}")
public void refreshSubscriptions()
```
- **How it works:** Every 10 minutes (configurable), this method fetches all `SourceConfigurationDTO`s from the `SourceConfigurationCache`. 
- **Filtering:** It filters configs where `status == "enabled"`.
- **Subscription:** It compares the currently subscribed topics against the enabled ones. If there is a change, it calls `consumer.subscribe(enabledTopics)`. If no topics are enabled, it completely unsubscribes to save resources.

### The Polling Loop
```java
private void consumeMessages()
```
- Started on a dedicated thread in `@PostConstruct init()`.
- Runs in a `while (running.get())` loop.
- **Locking:** Uses `synchronized (consumerLock)` to ensure `consumer.poll()` doesn't conflict with `refreshSubscriptions()`.
- **Polling:** `consumer.poll(Duration.ofMillis(100))` pulls records in batches. Each record is passed to `MessageProcessor.processMessage(topic, value)`.
- **Graceful Shutdown:** Handled by `@PreDestroy cleanup()`, which sets `running.set(false)`, waits for the thread to join, and safely closes the Kafka connection.

---

## 4. Message Processing & Field Mapping

The `MessageProcessor` receives raw string messages (comma-separated).

### Step 1: Configuration Lookup
- Looks up `SourceConfigurationDTO` by topic name. This provides the `ServiceType` (VOICE, SMS, USAGE) and `SourceId`.
- Fetches `SourceCdrConfigurationDTO`s which map sequence numbers to field names (e.g., 0 -> CALLING_NUMBER, 1 -> CALLED_NUMBER).

### Step 2: LOB Detection
Line of Business is determined entirely by the topic name:
- If topic contains `"roaming"`, LOB = `ROAMING`
- If topic contains `"interconnect"`, LOB = `INTERCONNECT`
- Fallback = `INTERCONNECT`

### Step 3: Parsing Logic
The raw message is split by commas: `String[] fields = message.split(",");`
- The system correlates `fields[i]` with the mapped field name from the configuration based on its sequence order.
- **Java 14 Switch Expressions** are used to map string fields to the strongly-typed properties on `VoiceRatedCdrDTO`, `SmsRatedCdrDTO`, or `UsageRatedCdrDTO`.
- Invalid data types (e.g., parsing "ABC" as a Double for USAGE) are caught, a warning is logged, and the specific field is skipped, allowing the rest of the CDR to process.

---

## 5. Rating Engine Integration

Once the DTO is parsed:
1. `MessageProcessor` calls `cdrRatingIntegrationService.processAndRateCdr(cdrRecord, sourceId, lob)`.
2. The Integration Service bridges the raw CDR with the Rating Engine.
3. The Rating Engine applies tariffs, prefix matching (via RadixTrie), and zoning logic to determine costs for both Incoming and Outgoing traffic.
4. An enhanced Entity (e.g., `VoiceRatedCdr`) is returned.
5. `MessageProcessor` finally passes the Entity to `voiceRatedCdrService.saveRatedCdr()` for database persistence.

---

## 6. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard VOICE CDR Flow (Positive)
**Context:** An interconnect switch drops a valid CSV CDR into the `interconnect_voice_topic`.
**Flow:**
1. `KafkaMessageConsumer` polls the topic and extracts the message.
2. `MessageProcessor` finds the config: `ServiceType = VOICE`.
3. LOB is detected as `INTERCONNECT` (due to topic name).
4. `parseMessage()` maps fields based on DB config sequence (e.g., Calling, Called, Duration).
5. CDR is sent to `CdrRatingIntegrationService`, rated successfully, and persisted to `voice_rated_cdr`.
**Result:** Data safely stored and ready for summary generation.

### ✅ Scenario 2: Dynamic Topic Addition (Positive)
**Context:** An admin enables a new partner's source configuration in the UI, marking it "enabled" with topic `roaming_partner_x_sms`.
**Flow:**
1. Within 10 minutes, the `@Scheduled` `refreshSubscriptions()` runs.
2. The cache returns the new list of enabled topics.
3. `consumer.subscribe(newTopics)` is invoked safely.
4. The polling loop immediately begins ingesting SMS CDRs from the new partner.
**Result:** Zero-downtime onboarding of new Kafka topics.

### ❌ Scenario 3: Topic Configuration Missing (Negative)
**Context:** A message arrives on a topic, but the cache returns `null` for `getConfigurationByTopicName(topicName)`.
**Flow:**
1. `MessageProcessor` logs a severe error: `"Source configuration not found for topic: [topicName]"`.
2. The specific message processing is aborted.
3. The method returns gracefully, and the consumer loop proceeds to the next message.
**Handling:** We prevent null pointers and system crashes. (Note: This is rare because the consumer only subscribes to enabled topics in the cache).

### ❌ Scenario 4: Malformed Numeric Fields (Negative)
**Context:** An SMS CDR is received where `MESSAGE_COUNT` is `"XYZ"` instead of a number.
**Flow:**
1. `parseSmsMessage()` maps `"XYZ"` to the `EVENT_NOS` switch case.
2. `NumberFormatException` is thrown during parsing.
3. The `catch` block catches the exception, logs: `"SMS parse: invalid MESSAGE_COUNT 'XYZ', skipping"`.
4. The rest of the CDR continues to be parsed and rated.
**Handling:** The application doesn't crash on bad data, but the resulting CDR may have `null` or default values for that specific field.

### ❌ Scenario 5: Duplicate Sequence Configuration (Negative)
**Context:** An admin misconfigures the database, assigning sequence `3` to both `START_TIME` and `END_TIME` for a specific source.
**Flow:**
1. `MessageProcessor` detects duplicates during field setup.
2. Logs a warning: `"Duplicate sequences found for sourceId X: [3]"`.
3. The stream sorts fields, but the order of duplicates is non-deterministic.
**Handling:** The CDR attempts to parse, but fields will likely be mapped to the wrong columns. The error log serves as the alert for admins to fix the database configuration.

### ❌ Scenario 6: Database/Rating Engine Crash (Negative)
**Context:** The `CdrRatingIntegrationService` or `voiceRatedCdrService.saveRatedCdr()` throws an unexpected `SqlException` or generic `Exception`.
**Flow:**
1. The top-level `try-catch` block in `processMessage()` intercepts the exception.
2. Logs: `"Failed to process message for topic [topicName]: [error]"`.
3. The message is discarded from memory (but committed in Kafka since `enable.auto.commit` is true).
**Handling:** The thread does not die. However, because auto-commit is enabled, **the message is lost**. *Developer Note for Future:* If strict exactly-once processing is required, manual offset commits and Dead Letter Queues (DLQ) should be implemented.

---

## 7. Checklist & Troubleshooting for New Developers

### 🛠️ Common Investigation Scenarios

#### Q1: "We added a topic but no CDRs are being processed."
- **Check 1:** Ensure the source configuration is set to `status = "enabled"` in the database.
- **Check 2:** Wait up to 10 minutes (the default `@Scheduled` interval) for `refreshSubscriptions()` to pick up the new topic.
- **Check 3:** Ensure the topic name conforms to LOB rules (`interconnect` or `roaming`).
- **Check 4:** Look at the logs for `"Subscribed to topics: [...]"` to verify the consumer sees it.

#### Q2: "Fields are showing up in the wrong columns in the database."
- **Cause:** The `sequence` numbers in the `source_cdr_configuration` table do not match the actual comma-separated order generated by the switch in Kafka.
- **Fix:** Update the sequence mapping in the database and clear the cache.

#### Q3: "The consumer stopped pulling messages completely."
- **Check 1:** Check if `running.get()` was somehow flipped to false (only happens on shutdown).
- **Check 2:** Check for a deadlock. Ensure no heavy synchronous DB queries inside `processMessage()` are stalling the consumer thread indefinitely.

### 📝 Developer Best Practices
1. **Never throw unhandled exceptions out of `processMessage()`**: It runs in the core Kafka loop. Unhandled exceptions kill the consumer thread.
2. **Adding new LOBs**: Update the `detectLineOfBusiness` logic in `MessageProcessor`. Currently, it only checks string contains for "roaming" or "interconnect".
3. **Kafka Properties**: Connection settings (`bootstrap-servers`, `group-id`) are read from `application.properties`/`yml`. Modifying offset reset from `earliest` to `latest` should be done cautiously.
