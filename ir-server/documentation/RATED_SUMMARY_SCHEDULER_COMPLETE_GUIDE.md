# Rated Summary Scheduler — Complete Developer Guide

> **Purpose:** This guide is written for new and existing developers working on the OCS (Online Charging System) codebase. After reading this document you will understand every flow — happy-path and failure — that can occur in the summary scheduler, how data is stored, how auditing works, and how to diagnose or fix future bugs.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map](#2-component-map)
3. [Database Schema Reference](#3-database-schema-reference)
4. [Configuration — Setup & Meaning of Every Field](#4-configuration--setup--meaning-of-every-field)
5. [Lifecycle: Startup to First Run](#5-lifecycle-startup-to-first-run)
6. [Polling Loop — How the Scheduler Decides to Fire](#6-polling-loop--how-the-scheduler-decides-to-fire)
7. [Execution Flow — Step by Step](#7-execution-flow--step-by-step)
8. [Summary Generation Deep Dive](#8-summary-generation-deep-dive)
9. [Upsert Logic — How Summaries Are Stored](#9-upsert-logic--how-summaries-are-stored)
10. [Sliding Window — Cursor Advancement](#10-sliding-window--cursor-advancement)
11. [Auditing — Every Event Explained](#11-auditing--every-event-explained)
12. [Query Templates (YAML)](#12-query-templates-yaml)
13. [Scenario Playbook — Positive & Negative Cases](#13-scenario-playbook--positive--negative-cases)
14. [API Reference](#14-api-reference)
15. [Transaction Boundaries](#15-transaction-boundaries)
16. [Shutdown Safety & Crash Recovery](#16-shutdown-safety--crash-recovery)
17. [Configuration Reset Behavior](#17-configuration-reset-behavior)
18. [Common Bugs & How to Fix Them](#18-common-bugs--how-to-fix-them)
19. [Checklist for New Developers](#19-checklist-for-new-developers)

---

## 1. System Overview

The **Rated Summary Scheduler** is a background job in the OCS platform that takes raw, individually-rated CDRs (Call Detail Records) from the `voice_rated_cdr`, `sms_rated_cdr`, and `usage_rated_cdr` tables and aggregates them into daily summary rows stored in the `rated_summary` table.

### Why Does This Exist?

In a standard OCS / Interconnect billing system, you may receive millions of CDRs per day. Invoicing partners, calculating settlement amounts, or presenting usage dashboards requires aggregated totals — not raw CDR rows. The summary scheduler creates those aggregates:

- **VOICE**: Total calls, total duration (seconds), total charge per account/prefix/rate per day
- **SMS**: Total message count (`SUM(event_nos)`), total charge
- **DATA/USAGE**: Total sessions, total data volume, total charge

### Core Design Goals

| Goal | How It Is Achieved |
|---|---|
| **No CDR processed twice** | `is_summarized` flag set after each batch; checked in WHERE clause |
| **No CDR skipped on failure** | Window cursor unchanged on failure — same window retried |
| **No data loss on crash** | `@PostConstruct` recovery changes RUNNING to FAILED; window replayed on next startup |
| **Config changes don't corrupt in-flight run** | `resetPending` flag defers cursor clear until after the current run completes |
| **Auditable** | Every event written to `scheduler_audit_log` with CDR window, record count, execution time |

---

## 2. Component Map

```
HTTP API (REST)
    |
    +-- SchedulerConfigurationController  ---> SchedulerConfigurationService
    +-- SchedulerStatusController         ---> SchedulerService.getLastScheduler()
    +-- SchedulerAuditLogController       ---> SchedulerAuditLogService
    +-- RatedSummaryController            ---> RatedSummaryService (manual trigger)

Background Thread (Spring @Scheduled)
    |
    +-- SchedulerService.checkAndExecuteSchedulers()  [every 5 seconds]
            |
            +-- SchedulerExecutorService.executeScheduler()  [REQUIRES_NEW transaction]
                        |
                        +-- RatedSummaryService.generateDailySummaries(start, end)
                                    |
                                    +-- generateOutgoingSummaries(params)
                                    |       +-- DynamicQueryService("rated-summary-outgoing-daily")       -> voice_rated_cdr
                                    |       +-- DynamicQueryService("rated-summary-sms-outgoing-daily")   -> sms_rated_cdr
                                    |       +-- DynamicQueryService("rated-summary-usage-outgoing-daily") -> usage_rated_cdr
                                    |       +-- saveSummaries(results, "OUTGOING")  -> rated_summary (upsert)
                                    |
                                    +-- CdrMarkSummarizedService.markOutgoingAsSummarized(start, end)
                                    |       +-- UPDATE voice/sms/usage_rated_cdr SET is_summarized = true
                                    |
                                    +-- generateIncomingSummaries(params)
                                    |       +-- DynamicQueryService("rated-summary-incoming-daily")       -> voice_rated_cdr
                                    |       +-- DynamicQueryService("rated-summary-sms-incoming-daily")   -> sms_rated_cdr
                                    |       +-- DynamicQueryService("rated-summary-usage-incoming-daily") -> usage_rated_cdr
                                    |       +-- saveSummaries(results, "INCOMING")  -> rated_summary (upsert)
                                    |
                                    +-- CdrMarkSummarizedService.markIncomingAsSummarized(start, end)
                                            +-- UPDATE voice/sms/usage_rated_cdr SET is_summarized = true
```

### Class Responsibilities

| Class | Package | Role |
|---|---|---|
| `SchedulerService` | `summaryengine.service` | Polling loop, startup recovery, shutdown safety, reset-pending flag |
| `SchedulerExecutorService` | `summaryengine.service` | Single window execution, audit logging, cursor advancement |
| `RatedSummaryService` | `summaryengine.service` | Summary aggregation, upsert logic, DTO query methods |
| `CdrMarkSummarizedService` | `summaryengine.service` | Marks CDRs as `is_summarized = true` after each direction |
| `DynamicQueryService` | `summaryengine.service` | Builds and runs SQL from YAML query templates |
| `SchedulerConfigurationService` | `summaryengine.service` | CRUD for config, triggers status reset on update |
| `SchedulerConfigurationController` | `controller` | REST endpoints for config management |
| `SchedulerStatusController` | `controller` | REST endpoint to query current status |
| `SchedulerAuditLogController` | `controller` | Paginated / filtered audit log query |
| `RatedSummaryController` | `controller` | REST endpoints to query or manually trigger summaries |

---

## 3. Database Schema Reference

### 3.1 `scheduler_configuration`

Stores the single scheduler configuration row. Only one record exists at any time.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `config_id` | BIGINT PK | No | Auto-generated primary key |
| `start_timestamp` | TIMESTAMP | No | First wall-clock time the scheduler should fire |
| `interval_type` | ENUM(HOUR, DAY, WEEK) | No | Unit for how often the scheduler runs |
| `interval_value` | INT | No | Multiplier for `interval_type` (e.g. `6` HOUR = run every 6 hours) |
| `targeted_timestamp` | TIMESTAMP | No | The start of the **first** CDR time window to process |
| `targeted_interval_type` | ENUM(HOUR, DAY, WEEK) | No | Unit for the size of each CDR window |
| `targeted_max_interval_value` | INT | No | Multiplier for CDR window size (e.g. `1` DAY = 1 day per window) |
| `is_active` | BOOLEAN | No | Whether the scheduler is enabled |
| `interconnect_type` | ENUM | No | Type of interconnect (e.g. BILATERAL) |
| `is_deleted` | BOOLEAN | No | Soft-delete flag (inherited from BaseEntity) |

### 3.2 `scheduler_status`

Tracks the live cursor state. Exactly one active record; updated in-place on every run.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `scheduler_status_id` | BIGINT PK | No | Auto-generated primary key |
| `config_id` | BIGINT FK | No | References `scheduler_configuration.config_id` |
| `status` | ENUM | No | `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` |
| `last_scheduler_run_time` | TIMESTAMP | Yes | When the last execution window started |
| `next_scheduler_start_time` | TIMESTAMP | Yes | When the next polling tick should trigger execution |
| `targeted_rated_cdr_start_time` | TIMESTAMP | Yes | Start of the CDR window to process **next** |
| `targeted_rated_cdr_end_time` | TIMESTAMP | Yes | End of the CDR window **currently or last** processed |

### 3.3 `scheduler_audit_log`

Immutable append-only log. Never updated, only inserted.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `audit_id` | BIGINT PK | No | Auto-generated primary key |
| `config_id` | BIGINT | No | References the config at time of event |
| `scheduler_status_id` | BIGINT | Yes | References the status row at time of event |
| `event_type` | VARCHAR(50) | No | Event name string (see Section 11) |
| `event_message` | TEXT | Yes | Human-readable description with window, records, timing |
| `cdr_start_time` | TIMESTAMP | Yes | CDR window start for this event |
| `cdr_end_time` | TIMESTAMP | Yes | CDR window end for this event |
| `records_processed` | BIGINT | Yes | Number of CDRs marked as summarized |
| `execution_time_ms` | BIGINT | Yes | Wall-clock time of the run in milliseconds |
| `error_details` | TEXT | Yes | Exception class + message on failure |

### 3.4 `rated_summary`

The aggregated output table.

| Column | Type | Nullable | Description |
|---|---|---|---|
| `summary_id` | BIGINT PK | No | Auto-generated primary key |
| `summary_date` | DATE | No | Calendar date of CDRs summarized (`DATE(rated_at OR modified_date)`) |
| `account_code` | VARCHAR | No | Account identifier (outgoing or incoming depending on direction) |
| `partner_id` | BIGINT | Yes | Partner associated with the account |
| `partner_name` | VARCHAR | Yes | Partner name |
| `source_prefix` | VARCHAR | Yes | Matched source number prefix (null for ZONE rating) |
| `source_prefix_name` | VARCHAR | Yes | Human-readable name of source prefix |
| `source_country_code` | VARCHAR | Yes | ISO country code of source prefix |
| `source_country_name` | VARCHAR | Yes | Country name of source prefix |
| `destination_prefix` | VARCHAR | Yes | Matched destination prefix (null for ZONE rating) |
| `destination_prefix_name` | VARCHAR | Yes | Human-readable name of destination prefix |
| `destination_country_code` | VARCHAR | Yes | ISO country code of destination prefix |
| `destination_country_name` | VARCHAR | Yes | Country name of destination prefix |
| `zone_name` | VARCHAR(100) | Yes | Zone name (null for prefix-based rating) |
| `rating_type` | VARCHAR(30) | Yes | `SOURCE_DESTINATION_BASED` / `DESTINATION_BASED` / `ZONE_DESTINATION_BASED` |
| `service_type` | VARCHAR | No | `VOICE` / `SMS` / `USAGE` |
| `direction` | VARCHAR | No | `OUTGOING` or `INCOMING` |
| `applied_rate` | DECIMAL(10,4) | Yes | The rate that was applied during billing |
| `total_calls` | BIGINT | Yes | COUNT of VOICE CDRs (null for SMS/USAGE) |
| `total_sms` | BIGINT | Yes | SUM of `event_nos` from SMS CDRs (null for VOICE/USAGE) |
| `total_sessions` | BIGINT | Yes | COUNT of USAGE CDRs (null for VOICE/SMS) |
| `total_duration` | DECIMAL(10,2) | Yes | Total call duration in seconds (VOICE only) |
| `total_charge` | DECIMAL(10,4) | No | Total monetary charge |
| `total_data_volume` | DECIMAL(19,4) | Yes | Total bytes (USAGE only) |
| `is_deleted` | BOOLEAN | No | Soft-delete (via `@SQLDelete`) |

#### Upsert Key (Dedup Key)

A summary row is unique on the composite of:

```
(summary_date, account_code, direction, service_type, rating_type,
 source_prefix, destination_prefix, zone_name, applied_rate)
```

If a row with the same key already exists, the metrics are **accumulated** (not overwritten). See Section 9 for details.

---

## 4. Configuration — Setup & Meaning of Every Field

```json
POST /api/scheduler
{
  "startTimestamp":             "2026-01-01T06:00:00",
  "intervalType":               "HOUR",
  "intervalValue":              6,
  "targetedTimestamp":          "2026-01-01T00:00:00",
  "targetedIntervalType":       "DAY",
  "targetedMaxIntervalValue":   1,
  "interconnectType":           "BILATERAL",
  "active":                     true
}
```

| Field | What It Controls |
|---|---|
| `startTimestamp` | The wall-clock datetime when the **first** execution should happen. The scheduler will not fire before this time. |
| `intervalType` + `intervalValue` | How often the scheduler re-runs after each success. Example: `HOUR` + `6` = run every 6 hours. |
| `targetedTimestamp` | The start of the **first CDR window** to process. CDRs modified on or after this timestamp will be included in the first run. |
| `targetedIntervalType` + `targetedMaxIntervalValue` | How big each CDR window is. Example: `DAY` + `1` = each run processes 1 day of CDRs. |
| `interconnectType` | Metadata field — does not affect processing logic. |
| `active` | Master on/off switch. If false, the polling loop skips every 5 seconds without doing anything. |

### Configuration Example

```
Scenario: Process hourly CDR windows, scheduler runs every 6 hours.

startTimestamp            = 2026-01-01 06:00:00   <- First fire at 6 AM
intervalType              = HOUR
intervalValue             = 6                      <- Then every 6 hours

targetedTimestamp         = 2026-01-01 00:00:00   <- Start from midnight CDRs
targetedIntervalType      = HOUR
targetedMaxIntervalValue  = 1                      <- Process 1 hour of CDRs per run

Run 1 (06:00): process CDRs modified in [00:00, 01:00)
Run 2 (12:00): process CDRs modified in [01:00:001ms, 02:00:001ms)
Run 3 (18:00): process CDRs modified in [02:00:002ms, 03:00:002ms)
...
```

> **Important:** If real clock time overtakes the next scheduled time (e.g. server was down for hours), the scheduler immediately runs when it wakes up. The catch-up logic is: `if (now.isAfter(nextSchedulerTime)) { nextSchedulerTime = now; }`.

---

## 5. Lifecycle: Startup to First Run

```
Application Starts
        |
        v
@PostConstruct: recoverFromCrash()
        |
        +-- Query scheduler_status for latest row
        +-- If status == RUNNING -> set to FAILED + write SCHEDULER_EXECUTED_FAILED audit
        +-- (Scheduler was mid-run when app crashed -- will retry same window)
        |
        v
Spring @Scheduled fires: checkAndExecuteSchedulers() [every 5s]
        |
        +-- Load scheduler_configuration (configs table)
        |   +-- If empty -> skip, log debug
        |
        +-- If config.isActive == false -> skip, log debug
        |
        +-- Load scheduler_status
        |   +-- If not found -> create new status {PENDING, configId}
        |
        +-- If status == RUNNING -> skip (already in-flight)
        |
        +-- If nextSchedulerStartTime == null
        |   +-- Set from config.startTimestamp, save
        |
        +-- If nextSchedulerStartTime.isAfter(now) -> skip (not yet time)
        |
        +-- If targetedRatedCdrStartTime == null
        |   +-- Set from config.targetedTimestamp, save
        |
        +-- Call: SchedulerExecutorService.executeScheduler(status, config, resetPending)
```

---

## 6. Polling Loop — How the Scheduler Decides to Fire

```java
@Scheduled(fixedDelay = 5000)    // 5 seconds after previous completion
@Transactional
public void checkAndExecuteSchedulers()
```

**Key guards (all must pass to trigger execution):**

| Guard | Condition | Action on Fail |
|---|---|---|
| Config exists | `configRepository.findAll()` non-empty | skip |
| Config active | `config.isActive()` is true | skip |
| Not already running | `status.getStatus() != RUNNING` | skip |
| Next time initialized | `nextSchedulerStartTime != null` | set from `config.startTimestamp`, save |
| Next time has passed | `nextSchedulerStartTime <= now` | skip |
| CDR cursor initialized | `targetedRatedCdrStartTime != null` | set from `config.targetedTimestamp`, save |

When all guards pass, `SchedulerExecutorService.executeScheduler()` is called.

> **Note:** The `fixedDelay` annotation means Spring waits 5 seconds *after* the previous invocation *completes* before triggering the next one. The scheduler can never overlap with itself at the polling level. The `RUNNING` guard is an additional safety layer for edge cases.

---

## 7. Execution Flow — Step by Step

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void executeScheduler(SchedulerStatus status, SchedulerConfiguration config, boolean resetPending)
```

### Step 1 — Calculate Window End Time

```java
LocalDateTime cdrStartTime = status.getTargetedRatedCdrStartTime();
LocalDateTime endTime = calculateEndTime(
    cdrStartTime,
    config.getTargetedIntervalType(),
    config.getTargetedMaxIntervalValue()
);
```

Example: CDR window size = 1 DAY, `cdrStartTime = 2026-01-01 00:00:00`
Result: `endTime = 2026-01-02 00:00:00`

### Step 2 — Mark Status as RUNNING

```java
status.setStatus(RUNNING);
status.setLastSchedulerRunTime(now());
status.setTargetedRatedCdrEndTime(endTime);
statusRepository.save(status);
```

This is the first database write. If the app crashes here, `@PostConstruct` recovery detects RUNNING on next startup.

### Step 3 — Generate Summaries

```java
long recordsProcessed = ratedSummaryService.generateDailySummaries(cdrStartTime, endTime);
```

See Section 8 for full detail.

### Step 4 — Determine Effective End

After summaries are generated, the service checks the actual last-modified timestamp across all CDR tables in the window:

```java
LocalDateTime lastModifiedDate = ratedSummaryService.getLastModifiedDateOfProcessedCdrs(cdrStartTime, endTime);
```

This advances the cursor to **exactly the last CDR that was processed**, not the calculated window end.
This prevents gaps when a window has no CDRs right up to the edge.

### Step 5a — Success, No Reset Pending

```java
// Advance cursor: start from last processed CDR + 1ms
LocalDateTime nextCdrStartTime = effectiveEnd.plus(1, ChronoUnit.MILLIS);

// Advance scheduler time
LocalDateTime nextSchedulerTime = calculateNextTime(
    status.getNextSchedulerStartTime(),
    config.getIntervalType(), config.getIntervalValue());
if (now.isAfter(nextSchedulerTime)) {
    nextSchedulerTime = now;  // catch-up logic
}

status.setStatus(SUCCESS);
status.setTargetedRatedCdrStartTime(nextCdrStartTime);
status.setNextSchedulerStartTime(nextSchedulerTime);
status.setTargetedRatedCdrEndTime(lastModifiedDate);
statusRepository.save(status);
logAudit("SCHEDULER_EXECUTED_SUCCESS", ...);
```

### Step 5b — Success, Reset Pending

If config was updated while this run was in-flight:

```java
logAudit("SCHEDULER_EXECUTED_SUCCESS", ...);  // still logs success
status.setStatus(PENDING);
status.setNextSchedulerStartTime(null);
status.setTargetedRatedCdrStartTime(null);   // cursor wiped -- fresh start from new config
statusRepository.save(status);
```

### Step 5c — Exception / Failure

```java
status.setStatus(FAILED);
status.setNextSchedulerStartTime(now().plusMinutes(5));  // 5-minute backoff
// targetedRatedCdrStartTime is NOT changed -- same window will retry
statusRepository.save(status);
logAudit("SCHEDULER_EXECUTED_FAILED", ...);
```

---

## 8. Summary Generation Deep Dive

```java
@Transactional(timeout = 600, propagation = Propagation.REQUIRES_NEW)
public long generateDailySummaries(LocalDateTime startTime, LocalDateTime endTime)
```

> **Timeout:** The transaction has a 10-minute (600-second) timeout to prevent runaway queries from locking the database indefinitely.

### Step 1 — Early Exit Check

```java
long voiceCount  = voiceRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);
long smsCount    = smsRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);
long usageCount  = usageRatedCdrRepository.countUnsummarizedCdrs(startTime, endTime);

if (voiceCount == 0 && smsCount == 0 && usageCount == 0) {
    log.warn("All CDRs already summarized for {} to {}", startTime, endTime);
    return 0L;
}
```

CDRs are counted using:
```sql
WHERE modified_date >= :startTime
  AND modified_date <= :endTime
  AND (incoming_rating_status = 'RATED' OR outgoing_rating_status = 'RATED')
  AND is_deleted = false
  AND is_summarized = false
  AND line_of_business = 'INTERCONNECT'
```

If all three service types return 0 unsummarized CDRs, the run is a **no-op** and returns immediately. The scheduler still advances the cursor and schedules the next run normally.

### Step 2 — OUTGOING Pass

```
generateOutgoingSummaries(params)
    +-- Voice:  DynamicQueryService("rated-summary-outgoing-daily")
    +-- SMS:    DynamicQueryService("rated-summary-sms-outgoing-daily")
    +-- Usage:  DynamicQueryService("rated-summary-usage-outgoing-daily")

For each template result:
    +-- saveSummaries(results, "OUTGOING")  -> rated_summary upsert

Then immediately:
    +-- CdrMarkSummarizedService.markOutgoingAsSummarized(startTime, endTime)
        +-- UPDATE voice/sms/usage_rated_cdr SET is_summarized = true
            WHERE modified_date BETWEEN :start AND :end
              AND outgoing_rating_status = 'RATED'
              AND is_deleted = false
              AND is_summarized = false
              AND line_of_business = 'INTERCONNECT'
```

> **Why mark OUTGOING before processing INCOMING?** If INCOMING fails, the OUTGOING CDRs are already flagged so they will not be double-counted on retry.

### Step 3 — INCOMING Pass

Same pattern as OUTGOING but using:
- `"rated-summary-incoming-daily"`, `"rated-summary-sms-incoming-daily"`, `"rated-summary-usage-incoming-daily"` templates
- `incoming_account_id`, `incoming_rating_status`, `incoming_total_cost` columns
- `markIncomingAsSummarized()` runs after all three incoming templates succeed

### Step 4 — Return Count

```java
long totalMarked = outgoingMarked + incomingMarked;
return totalMarked;
```

This count flows back to `SchedulerExecutorService` where it is embedded in the audit log message.

---

## 9. Upsert Logic — How Summaries Are Stored

`saveSummaries(List<Map<String,Object>> results, String direction)` is called once per query template per run.

### Dedup Key Lookup

For each result row, the system checks:

```java
Optional<RatedSummary> existing = ratedSummaryRepository.findExistingSummary(
    summaryDate, accountCode, direction,
    serviceType, ratingType,
    sourcePrefix, destinationPrefix, zoneName,
    appliedRate
);
```

Translated to SQL:
```sql
SELECT rs FROM rated_summary rs
WHERE rs.summary_date       = :summaryDate
  AND rs.account_code       = :accountCode
  AND rs.direction          = :direction
  AND rs.service_type       = :serviceType
  AND rs.rating_type        = :ratingType
  AND (rs.source_prefix = :sourcePrefix
    OR (rs.source_prefix IS NULL AND :sourcePrefix IS NULL))
  AND (rs.destination_prefix = :destinationPrefix
    OR (rs.destination_prefix IS NULL AND :destinationPrefix IS NULL))
  AND (rs.zone_name = :zoneName
    OR (rs.zone_name IS NULL AND :zoneName IS NULL))
  AND rs.applied_rate       = :appliedRate
```

> **Why NULL-safe equals?** Source prefix is NULL for `DESTINATION_BASED` rated CDRs and zone_name is NULL for prefix-based CDRs. Standard SQL `NULL = NULL` evaluates to false, so explicit null-safe handling is required.

### UPDATE Path (record exists)

```java
if ("VOICE".equals(serviceType)) {
    summary.setTotalCalls(   nullSafeAdd(summary.getTotalCalls(),    getLong(row, "total_calls")));
    summary.setTotalDuration(nullSafeAdd(summary.getTotalDuration(), getBigDecimal(row, "total_duration")));
} else if ("SMS".equals(serviceType)) {
    summary.setTotalSms(nullSafeAdd(summary.getTotalSms(), getLong(row, "total_sms")));
} else if ("USAGE".equals(serviceType)) {
    summary.setTotalSessions(  nullSafeAdd(summary.getTotalSessions(), getLong(row, "total_sessions")));
    summary.setTotalDataVolume(nullSafeAdd(summary.getTotalDataVolume(), getBigDecimal(row, "total_data_volume")));
}
summary.setTotalCharge(summary.getTotalCharge().add(getBigDecimal(row, "total_charge")));
```

Accumulation is null-safe: if the existing field is null, it is treated as 0.

### INSERT Path (no record exists)

All fields are set from the SQL result row, with service-type specific metric fields:

- VOICE: `total_calls`, `total_duration`
- SMS: `total_sms`
- USAGE: `total_sessions`, `total_data_volume`

Common to all: `summary_date`, `account_code`, `partner_id`, `partner_name`, `source_prefix`, `destination_prefix`, `zone_name`, `rating_type`, `service_type`, `direction`, `applied_rate`, `total_charge`.

---

## 10. Sliding Window — Cursor Advancement

The **sliding window** ensures CDRs are processed exactly once, in chronological order.

### Window State in `scheduler_status`

| Field | Meaning |
|---|---|
| `targeted_rated_cdr_start_time` | Start of the NEXT CDR window to process |
| `targeted_rated_cdr_end_time` | End of the window that was LAST processed |

### After a Successful Run

```
Before run:
  targetedRatedCdrStartTime = 2026-01-01 00:00:00
  targetedRatedCdrEndTime   = null (first run)

After run:
  targetedRatedCdrEndTime   = 2026-01-01T22:47:33.211  <- lastModifiedDate of CDRs in window
  targetedRatedCdrStartTime = 2026-01-01T22:47:33.212  <- +1ms
```

By advancing to `lastModifiedDate + 1ms` rather than `windowEnd + 1ms`, the system avoids producing empty windows when CDRs do not fill the entire configured interval.

### Cursor Progression Example

```
Config: targetedIntervalType=HOUR, targetedMaxIntervalValue=1

Run 1: window [2026-01-01 00:00:00 -> 01:00:00]
       lastModified = 2026-01-01 00:47:12.500
       Next start   = 2026-01-01 00:47:12.501

Run 2: window [2026-01-01 00:47:12.501 -> 01:47:12.501]
       lastModified = 2026-01-01 01:22:00.000
       Next start   = 2026-01-01 01:22:00.001

Run 3: window [2026-01-01 01:22:00.001 -> 02:22:00.001]
       ...
```

---

## 11. Auditing — Every Event Explained

Every significant event is appended to `scheduler_audit_log`. The table is **never updated** — only inserted.

| `event_type` | When It Is Written | Key Fields Populated |
|---|---|---|
| `SCHEDULER_EXECUTED_SUCCESS` | A CDR window completed successfully | `cdr_start_time`, `cdr_end_time`, `records_processed`, `execution_time_ms` |
| `SCHEDULER_EXECUTED_FAILED` | An exception was thrown, OR app crashed or shut down while RUNNING | `cdr_start_time`, `cdr_end_time`, `execution_time_ms`, `error_details` |

The `event_message` field contains a full, human-readable description including exact CDR range, record count, execution time, and next run time (on success) or error details (on failure).

### Sample Audit Messages

**Success:**
```
Scheduler executed successfully. CDR range: 2026-01-01T00:00:00 to 2026-01-01T00:47:12.500.
Records processed: 14523. Execution time: 3241ms. Next run scheduled at: 2026-01-01T06:00:00.
```

**Failure (SQL error):**
```
Scheduler execution failed. CDR range: 2026-01-01T00:00:00 to 2026-01-01T01:00:00.
Error: Unable to acquire JDBC Connection. Execution time: 127ms
```

**Crash Recovery (written by @PostConstruct):**
```
Scheduler execution failed. CDR range: 2026-01-01T00:00:00 to 2026-01-01T01:00:00.
Error: Application crash/forced shutdown. Execution time: N/A
```

**Shutdown (written by @PreDestroy):**
```
Scheduler execution failed. CDR range: 2026-01-01T00:00:00 to 2026-01-01T01:00:00.
Error: Application shutdown. Execution time: N/A
```

---

## 12. Query Templates (YAML)

Summaries are generated by SQL built dynamically from YAML templates in `query-config.yml`.

### Templates Overview

| Template Name | CDR Table | Direction | Metric Fields |
|---|---|---|---|
| `rated-summary-outgoing-daily` | `voice_rated_cdr` | OUTGOING | `total_calls`, `total_duration`, `total_charge` |
| `rated-summary-incoming-daily` | `voice_rated_cdr` | INCOMING | `total_calls`, `total_duration`, `total_charge` |
| `rated-summary-sms-outgoing-daily` | `sms_rated_cdr` | OUTGOING | `total_sms`, `total_charge` |
| `rated-summary-sms-incoming-daily` | `sms_rated_cdr` | INCOMING | `total_sms`, `total_charge` |
| `rated-summary-usage-outgoing-daily` | `usage_rated_cdr` | OUTGOING | `total_sessions`, `total_data_volume`, `total_charge` |
| `rated-summary-usage-incoming-daily` | `usage_rated_cdr` | INCOMING | `total_sessions`, `total_data_volume`, `total_charge` |

### Generated SQL Example (Voice OUTGOING)

```sql
SELECT
    DATE(COALESCE(rc.rated_at, rc.modified_date)) AS summary_date,
    rc.outgoing_account_id AS account_code,
    p.partner_id,
    p.partner_name,
    rc.outgoing_matched_source_prefix AS source_prefix,
    sp.prefix_name AS source_prefix_name,
    sc.country_code AS source_country_code,
    sc.name AS source_country_name,
    CASE WHEN rc.zone_name IS NULL THEN rc.outgoing_matched_destination_prefix ELSE NULL END AS destination_prefix,
    CASE WHEN rc.zone_name IS NULL THEN dp.prefix_name ELSE NULL END AS destination_prefix_name,
    CASE WHEN rc.zone_name IS NULL THEN dc.country_code ELSE NULL END AS destination_country_code,
    CASE WHEN rc.zone_name IS NULL THEN dc.name ELSE NULL END AS destination_country_name,
    rc.zone_name,
    CASE WHEN rc.outgoing_matched_source_prefix IS NOT NULL THEN 'SOURCE_DESTINATION_BASED'
         WHEN rc.zone_name IS NOT NULL THEN 'ZONE_DESTINATION_BASED'
         ELSE 'DESTINATION_BASED' END AS rating_type,
    'VOICE' AS service_type,
    rc.outgoing_applied_rate AS applied_rate,
    COUNT(*) AS total_calls,
    SUM(rc.duration_seconds) AS total_duration,
    SUM(rc.outgoing_total_cost) AS total_charge
FROM voice_rated_cdr rc
LEFT JOIN accounts a ON rc.outgoing_account_id = a.account_code_or_hplmn
LEFT JOIN partners p ON a.partner_id = p.partner_id
LEFT JOIN prefixes sp ON rc.outgoing_matched_source_prefix = sp.prefix
LEFT JOIN countries sc ON sp.country_id = sc.country_id
LEFT JOIN prefixes dp ON rc.outgoing_matched_destination_prefix = dp.prefix
LEFT JOIN countries dc ON dp.country_id = dc.country_id
WHERE rc.modified_date >= :startTime
  AND rc.modified_date <= :endTime
  AND rc.outgoing_rating_status = :ratingStatus
  AND rc.is_deleted = :isDeleted
  AND rc.is_summarized = :isSummarized
  AND rc.line_of_business = :lineOfBusiness
GROUP BY DATE(COALESCE(rc.rated_at, rc.modified_date)), rc.outgoing_account_id, p.partner_id, ...
ORDER BY total_charge DESC
```

### Rating Type Decision Logic

| Rating Type | When Used | `source_prefix` | `destination_prefix` | `zone_name` |
|---|---|---|---|---|
| `SOURCE_DESTINATION_BASED` | Rate depends on both origin and destination prefix | Populated | Populated | NULL |
| `DESTINATION_BASED` | Rate depends on destination prefix only | NULL | Populated | NULL |
| `ZONE_DESTINATION_BASED` | Rate depends on zone (group of prefixes) | NULL | NULL | Populated |

---

## 13. Scenario Playbook — Positive & Negative Cases

---

### Scenario 1 — Normal First Run (POSITIVE)

**Setup:**
- Config: `startTimestamp = 2026-01-01 06:00:00`, `targetedTimestamp = 2026-01-01 00:00:00`, window size = 1 DAY
- 50,000 INTERCONNECT VOICE CDRs rated on 2026-01-01
- Current time: 2026-01-01 06:00:00

**Flow:**
1. Polling tick fires; all 6 guards pass
2. Window calculated: `[2026-01-01 00:00:00 -> 2026-01-02 00:00:00]`
3. `countUnsummarizedCdrs()` returns 50,000 > 0 — proceed
4. 6 query templates run; aggregated rows saved to `rated_summary`
5. `markOutgoingAsSummarized` + `markIncomingAsSummarized` set `is_summarized = true` on 50,000 CDRs
6. `lastModified = 2026-01-01T23:59:59.100`
7. Cursor advances to `2026-01-01T23:59:59.101`
8. `next_scheduler_start_time = 2026-01-01 12:00:00` (06:00 + 6 hours)
9. Audit: `SCHEDULER_EXECUTED_SUCCESS` with `records_processed = 50000`

**Database after run:**
- `scheduler_status`: `status=SUCCESS`, `targeted_rated_cdr_start_time=2026-01-01T23:59:59.101`
- `rated_summary`: Multiple rows for 2026-01-01, one per unique (account, prefix, rate, direction, service_type) group
- `voice_rated_cdr.is_summarized`: all 50,000 CDRs = `true`
- `scheduler_audit_log`: 1 new row with `event_type=SCHEDULER_EXECUTED_SUCCESS`

---

### Scenario 2 — Window Has No Unsummarized CDRs (POSITIVE)

**Setup:**
- Window: `[2026-01-03 12:00:00 -> 2026-01-04 12:00:00]`
- All CDRs in this range have `is_summarized = true` already

**Flow:**
1. `countUnsummarizedCdrs()` returns 0 for all three service types
2. `generateDailySummaries()` returns `0L` immediately
3. `lastModifiedDate = null` — no CDRs found in range
4. `effectiveEnd = endTime` (fallback since lastModified is null)
5. Cursor advances to `endTime + 1ms`
6. Audit: `SCHEDULER_EXECUTED_SUCCESS` with `records_processed = 0`

**What to watch for:**
Repeated zero-record windows may indicate the rating engine is not producing CDRs in the time range being scanned. Check `modified_date` on rated CDRs against the scheduler cursor.

---

### Scenario 3 — Mixed Service Types in Same Window (POSITIVE)

**Setup:**
- Window: `[2026-02-15 00:00 -> 2026-02-16 00:00]`
- 10,000 VOICE CDRs, 3,000 SMS CDRs, 500 USAGE CDRs — all unsummarized

**Flow:**
1. `countUnsummarizedCdrs()`: voice=10000, sms=3000, usage=500 — total > 0
2. OUTGOING pass: 3 templates run in order (voice, sms, usage), each producing grouped rows
3. `markOutgoingAsSummarized` marks all OUTGOING voice + sms + usage CDRs
4. INCOMING pass: 3 templates run, `markIncomingAsSummarized` marks INCOMING CDRs

**Final `rated_summary` rows for 2026-02-15:**
- Rows with `service_type='VOICE'`, `direction='OUTGOING'`
- Rows with `service_type='VOICE'`, `direction='INCOMING'`
- Rows with `service_type='SMS'`, both directions
- Rows with `service_type='USAGE'`, both directions

---

### Scenario 4 — Late-Arriving CDRs / Accumulation (POSITIVE)

**Setup:**
- Run 1 already processed CDRs for 2026-03-01 and created a `rated_summary` row:
  `(2026-03-01, ACC001, OUTGOING, VOICE, DESTINATION_BASED, null, +44, null, 0.05, calls=100, charge=50.00)`
- 20 more VOICE CDRs for 2026-03-01 arrived late and were rated after Run 1
- Run 2 window covers the `modified_date` of these late CDRs

**Flow:**
1. 20 CDRs have `is_summarized = false`, `modified_date` within Run 2's window
2. OUTGOING voice query returns a row:
   `(2026-03-01, ACC001, DESTINATION_BASED, VOICE, OUTGOING, +44, 0.05, calls=20, charge=10.00)`
3. `findExistingSummary()` finds the existing row from Run 1
4. **UPDATE path**: `total_calls = 100 + 20 = 120`, `total_charge = 50.00 + 10.00 = 60.00`
5. 20 CDRs marked as `is_summarized = true`

**Result:** Correct accumulation without duplication.

---

### Scenario 5 — Database Connectivity Failure During Summary Generation (NEGATIVE)

**Setup:**
- Window: `[2026-01-10 00:00 -> 2026-01-11 00:00]`
- Database goes down mid-run (connection pool timeout, network partition)

**Flow:**
1. Status set to RUNNING
2. `generateDailySummaries()` called — OUTGOING voice query begins — connection lost
3. Exception: `CannotGetJdbcConnectionException: Unable to acquire JDBC Connection`
4. `SchedulerExecutorService` catch block:
   - `status.setStatus(FAILED)`
   - `status.setNextSchedulerStartTime(now + 5 minutes)` — backoff
   - `targetedRatedCdrStartTime` unchanged — same window will retry
   - Audit: `SCHEDULER_EXECUTED_FAILED`
5. After 5 minutes: retry same window

**Partial success nuance:**
If OUTGOING phase fully succeeded before the failure:
- OUTGOING CDRs: `is_summarized = true` — will not be re-processed on retry
- OUTGOING `rated_summary` rows exist — on retry, `findExistingSummary()` finds them and accumulates (no duplicate)
- INCOMING CDRs: `is_summarized = false` — re-processed correctly

---

### Scenario 6 — Application Crash While RUNNING (NEGATIVE)

**Setup:**
- Status: RUNNING, CDR window in progress
- Server process killed (OOM kill, power failure, `kill -9`)

**On Next Startup:**

```java
@PostConstruct
public void recoverFromCrash() {
    Optional<SchedulerStatus> existing = statusRepository.findTopByOrderByCreatedDateDesc();
    if (existing.isPresent() && existing.get().getStatus() == RUNNING) {
        existing.get().setStatus(FAILED);
        statusRepository.save(existing.get());
        logAudit(..., "SCHEDULER_EXECUTED_FAILED", ..., "Application crash/forced shutdown");
    }
}
```

**Result:**
- Status: `FAILED`
- `targeted_rated_cdr_start_time` unchanged (same window replayed after 5-minute backoff)
- CDRs already `is_summarized = true` before crash are safe — not re-processed

---

### Scenario 7 — Query Template Not Found (NEGATIVE)

**Setup:**
- `query-config.yml` is missing `rated-summary-usage-incoming-daily` (misconfigured or file not loaded)

**Flow:**
1. `DynamicQueryService.executeQuery("rated-summary-usage-incoming-daily", params)` called
2. `queryTemplateConfig.getQueryTemplates().get(templateName)` returns `null`
3. `throw new IllegalArgumentException("Query template not found: rated-summary-usage-incoming-daily")`
4. Exception propagates — window fails — status = FAILED — retry after 5 minutes

**How to Fix:**
- Verify `query-config.yml` is on the classpath and correctly formatted
- Check the `@ConfigurationProperties(prefix = "query-templates")` bean is loaded
- Look for YAML parse errors in application startup logs

---

### Scenario 8 — CDR Has NULL `account_code` (NEGATIVE)

**Setup:**
- Some CDRs have `outgoing_account_id = null` (rating engine assigned no account)

**Flow:**
1. SQL aggregation groups by `rc.outgoing_account_id` — one group has `account_code = null`
2. `saveSummaries()` processes this row — `account_code = null`
3. `ratedSummaryRepository.save(summary)` throws `DataIntegrityViolationException` (NOT NULL constraint on `account_code`)
4. `saveSummaries()` rethrows `RuntimeException("Failed to save OUTGOING summary row")`
5. Window fails — status = FAILED — retries

**How to Fix:**
- Investigate why `outgoing_account_id` is null in the rating engine
- Add a WHERE filter in the query template: `rc.outgoing_account_id IS NOT NULL`
- Or add null-check in `saveSummaries()` to skip rows with null account code

---

### Scenario 9 — Config Updated While Scheduler Is RUNNING (NEGATIVE)

**Setup:**
- Status: RUNNING (window in-flight)
- Operator calls `PUT /api/scheduler/{id}` to change `targetedMaxIntervalValue` from 1 DAY to 2 DAYS

**Flow:**
1. `SchedulerConfigurationService.createOrUpdate()` saves new config
2. Calls `schedulerService.markResetPending()`
3. `markResetPending()` reads status — sees RUNNING — sets `this.resetPending = true` (volatile in-memory flag)
4. Current run completes normally — `executeScheduler(..., resetPending=true)` executes
5. In executor:
   - Audit `SCHEDULER_EXECUTED_SUCCESS` logged (the run itself was fine)
   - Status set to `PENDING`, `nextSchedulerStartTime = null`, `targetedRatedCdrStartTime = null`
6. Next polling tick: cursor null — reset from new `config.targetedTimestamp`

**Why not interrupt mid-run?**
Interrupting mid-run could leave CDRs partially summarized — some `is_summarized = true`, some not — creating inconsistent data.

---

### Scenario 10 — No Configuration in Database (NEGATIVE)

**Setup:**
- `scheduler_configuration` table is empty — application just deployed for the first time

**Flow:**
1. Polling tick fires
2. `configRepository.findAll()` returns empty list
3. `log.debug("No scheduler configuration found — skipping")`
4. Nothing happens; repeats every 5 seconds

**Resolution:** Call `POST /api/scheduler` to create the first configuration.

---

### Scenario 11 — Scheduler Deactivated Mid-Operation (NEGATIVE)

**Setup:**
- Status: SUCCESS, scheduler running every 6 hours
- Operator calls `PATCH /api/scheduler?isActive=false`

**Flow:**
1. `config.isActive = false`, saved
2. Next polling tick: `config.isActive()` is false — skip
3. No new windows processed
4. Cursor preserved in `scheduler_status`

On re-activation (`PATCH /api/scheduler?isActive=true`): scheduler continues from where it left off. The cursor is NOT reset — no data is re-processed.

---

### Scenario 12 — Graceful Shutdown While RUNNING (NEGATIVE)

**Setup:**
- Status: RUNNING (window partially processed)
- Application receives SIGTERM

**Flow:**
1. Spring calls `@PreDestroy` on `SchedulerService.onShutdown()`
2. Checks `status == RUNNING` — yes
3. Sets status to `FAILED`, saves
4. Logs `SCHEDULER_EXECUTED_FAILED` audit with "Application shutdown" error
5. Application shuts down cleanly

**On Restart:** Same as crash recovery (Scenario 6). Window replayed from beginning.

---

## 14. API Reference

### Scheduler Configuration

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/scheduler` | Create or update the scheduler configuration |
| `PUT` | `/api/scheduler/{id}` | Update existing configuration (triggers reset-pending) |
| `PATCH` | `/api/scheduler?isActive=true\|false` | Toggle active/inactive |
| `GET` | `/api/scheduler` | Read current configuration |

#### Create/Update Request Body

```json
{
  "startTimestamp": "2026-01-01T06:00:00",
  "intervalType": "HOUR",
  "intervalValue": 6,
  "targetedTimestamp": "2026-01-01T00:00:00",
  "targetedIntervalType": "DAY",
  "targetedMaxIntervalValue": 1,
  "interconnectType": "BILATERAL",
  "active": true
}
```

### Scheduler Status

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/schedulerStatus` | Get the latest scheduler status (cursor state) |

**Sample Response:**
```json
{
  "schedulerStatusId": 1,
  "configId": 1,
  "status": "SUCCESS",
  "lastSchedulerRunTime": "2026-01-01T06:00:00",
  "nextSchedulerStartTime": "2026-01-01T12:00:00",
  "targetedRatedCdrStartTime": "2026-01-01T23:59:59.101",
  "targetedRatedCdrEndTime": "2026-01-01T23:59:59.100"
}
```

### Scheduler Audit Logs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/scheduler-audit-logs/paginated` | Paginated + filtered audit log query |

**Sample Request:**
```json
{
  "page": 1,
  "pageSize": 10,
  "searchCriteria": {
    "configId": 1,
    "eventType": "SCHEDULER_EXECUTED_FAILED"
  }
}
```

### Rated Summary Queries

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/rated-summary/account/{accountCode}?startDate=&endDate=` | Summaries for an account and date range |
| `GET` | `/api/rated-summary/date/{date}?direction=` | All summaries for a given date (optionally filtered by direction) |
| `GET` | `/api/rated-summary/partner/{partnerId}?startDate=&endDate=` | Summaries for a partner and date range |
| `POST` | `/api/rated-summary/generate?startTime=&endTime=` | **Manually trigger** summary generation (bypasses scheduler) |

---

## 15. Transaction Boundaries

Understanding transactions is critical for debugging data consistency issues.

```
checkAndExecuteSchedulers()     <- @Transactional (outer)
    |
    +-- executeScheduler()      <- @Transactional(REQUIRES_NEW) -- commits independently
            |
            +-- generateDailySummaries()          <- @Transactional(REQUIRES_NEW, timeout=600)
                        |
                        +-- generateOutgoingSummaries()     <- @Transactional (inner, uses existing tx)
                        |
                        +-- markOutgoingAsSummarized()      <- @Transactional(REQUIRES_NEW) -- commits independently
                        |
                        +-- generateIncomingSummaries()     <- @Transactional (inner)
                        |
                        +-- markIncomingAsSummarized()      <- @Transactional(REQUIRES_NEW) -- commits independently
```

### Why `REQUIRES_NEW` on the Executor?

`SchedulerService.checkAndExecuteSchedulers()` runs in a transaction. If `executeScheduler()` used the same transaction and threw an exception, the outer rollback would undo `is_summarized` flag updates that happened before the failure — leaving CDRs eligible for re-summarization incorrectly. `REQUIRES_NEW` gives the executor its own independent commit scope.

### Why `REQUIRES_NEW` on `markOutgoingAsSummarized` / `markIncomingAsSummarized`?

These operations are committed independently of the summary row inserts. If OUTGOING summary SQL succeeds and marks CDRs but INCOMING summary SQL fails:
- OUTGOING CDRs remain `is_summarized = true` — not re-processed on retry
- INCOMING CDRs remain `is_summarized = false` — re-processed correctly on retry

---

## 16. Shutdown Safety & Crash Recovery

### `@PreDestroy` — Graceful Shutdown

```java
@PreDestroy
public void onShutdown() {
    statusRepository.findTopByOrderByCreatedDateDesc().ifPresent(scheduler -> {
        if (scheduler.getStatus() == RUNNING) {
            scheduler.setStatus(FAILED);
            statusRepository.save(scheduler);
            logAudit(..., "SCHEDULER_EXECUTED_FAILED", ..., "Application shutdown");
        }
    });
}
```

### `@PostConstruct` — Crash Recovery

```java
@PostConstruct
public void recoverFromCrash() {
    Optional<SchedulerStatus> existing = statusRepository.findTopByOrderByCreatedDateDesc();
    if (existing.isPresent() && existing.get().getStatus() == RUNNING) {
        existing.get().setStatus(FAILED);
        statusRepository.save(existing.get());
        logAudit(..., "SCHEDULER_EXECUTED_FAILED", ..., "Application crash/forced shutdown");
    }
}
```

### Recovery Guarantee

The cursor (`targeted_rated_cdr_start_time`) is **never advanced** unless a run fully succeeds. Therefore:
- A crashed run can be safely replayed from the same start point
- CDRs marked `is_summarized = true` during the partial run are not re-processed
- Summary rows partially written before the crash are found by `findExistingSummary()` on retry and the delta is added — not duplicated

---

## 17. Configuration Reset Behavior

When `PUT /api/scheduler/{id}` or `POST /api/scheduler` is called on an **existing** configuration:

1. `SchedulerConfigurationService.createOrUpdate()` saves new config values
2. Calls `schedulerService.markResetPending()`

**If status is PENDING / SUCCESS / FAILED:**
```java
schedulerStatus.setStatus(PENDING);
schedulerStatus.setNextSchedulerStartTime(null);
schedulerStatus.setTargetedRatedCdrStartTime(null);
statusRepository.save(schedulerStatus);
```
Cursor immediately cleared. On next polling tick, cursor is reinitialized from new `targetedTimestamp`.

**If status is RUNNING:**
```java
this.resetPending = true;
```
In-memory flag set. After the current run completes, the executor checks this flag and clears the cursor.

> **Note:** `resetPending` is a `volatile boolean` field on `SchedulerService`. The `@Scheduled` thread and `SchedulerConfigurationService` may run in different threads — `volatile` ensures the flag is immediately visible across threads without synchronization overhead.

---

## 18. Common Bugs & How to Fix Them

### Bug 1 — Summaries Show Wrong `total_calls` (Too High)

**Symptom:** A `rated_summary` row shows `total_calls = 200` but only 100 voice CDRs exist for that combination.

**Likely Cause:** `markOutgoingAsSummarized` failed or was not committed, so CDRs were processed again in the next window.

**Investigation:**
```sql
-- Check CDRs that should be summarized but are not
SELECT COUNT(*) FROM voice_rated_cdr
WHERE modified_date BETWEEN '2026-01-01' AND '2026-01-02'
  AND is_summarized = false
  AND outgoing_rating_status = 'RATED';

-- Check audit log for the window
SELECT * FROM scheduler_audit_log
WHERE cdr_start_time = '2026-01-01'
ORDER BY created_date DESC;
```

---

### Bug 2 — Scheduler Stuck in FAILED Loop

**Symptom:** Audit log shows repeated `SCHEDULER_EXECUTED_FAILED` every 5 minutes, same CDR range.

**Investigation:**
- Read `error_details` from the latest `scheduler_audit_log` row
- Look for `IllegalArgumentException` (template not found), `DataIntegrityViolationException` (constraint violation), `BadSqlGrammarException` (query error)

**Fix options:**
1. Fix the underlying issue (schema, template YAML, data quality)
2. To skip the bad window, call `POST /api/scheduler` with an updated `targetedTimestamp` past the problem range — triggers reset — scheduler starts fresh

---

### Bug 3 — Scheduler Never Fires

**Symptom:** No new audit log rows; `next_scheduler_start_time` is always in the future.

**Investigation:**
```sql
SELECT * FROM scheduler_configuration;
SELECT * FROM scheduler_status ORDER BY created_date DESC LIMIT 1;
```

**Common causes:**
- `is_active = false` — call `PATCH /api/scheduler?isActive=true`
- `next_scheduler_start_time` is far in the future — `startTimestamp` was misconfigured
- Status stuck at `RUNNING` (crash recovery did not run) — manually set to `FAILED` in DB

---

### Bug 4 — `rated_summary` Has Rows with All Metric Columns NULL

**Symptom:** A summary row exists but `total_calls`, `total_sms`, `total_sessions` are all null.

**Likely Cause:** The `serviceType` value in the SQL result doesn't match `'VOICE'`, `'SMS'`, or `'USAGE'` exactly (Java comparison is case-sensitive), or the template returned an unexpected value.

**Investigation:**
```sql
SELECT service_type, COUNT(*) FROM rated_summary
WHERE total_calls IS NULL AND total_sms IS NULL AND total_sessions IS NULL
GROUP BY service_type;
```

**Fix:** Verify the `'VOICE'` / `'SMS'` / `'USAGE'` literal in the `select-columns` of the YAML template.

---

### Bug 5 — Window Boundary Gaps or Overlaps

**Symptom:** CDRs just outside the window boundary are missed, or the same CDRs appear in two consecutive windows.

**Root Cause:** Boundary logic uses `modified_date >= :startTime AND modified_date <= :endTime` — both ends inclusive. Cursor advances to `lastModifiedDate + 1ms`. If CDR timestamps have sub-millisecond precision that is truncated in the DB, gaps can appear.

**Verification:** Check that the DB column type supports millisecond precision (e.g. `DATETIME(3)` in MySQL).

---

## 19. Checklist for New Developers

Before working on a bug or feature in the summary engine, verify:

- [ ] You understand the sliding window mechanism (Section 10) — cursor advances to `lastModifiedDate + 1ms`
- [ ] You know all three CDR table types: `voice_rated_cdr`, `sms_rated_cdr`, `usage_rated_cdr`
- [ ] You understand the `is_summarized` flag and why it must be set before the window advances
- [ ] You understand the two independent transaction scopes: `generateDailySummaries` (REQUIRES_NEW) and `markAsSummarized` (REQUIRES_NEW)
- [ ] You can read and modify YAML query templates in `query-config.yml`
- [ ] You know the 8-column dedup key for `rated_summary` (Section 9)
- [ ] You understand the `resetPending` flag and when it is used (Section 17)
- [ ] You know the difference between the two audit event types and what each message contains
- [ ] You have checked `scheduler_audit_log` as the **first step** in any investigation

---

## Appendix A — SQL Cheat Sheet for Debugging

```sql
-- 1. Current scheduler state
SELECT * FROM scheduler_configuration;
SELECT * FROM scheduler_status ORDER BY created_date DESC LIMIT 1;

-- 2. Last 10 audit events
SELECT audit_id, event_type, cdr_start_time, cdr_end_time,
       records_processed, execution_time_ms,
       LEFT(event_message, 200) AS message,
       LEFT(error_details, 300) AS error
FROM scheduler_audit_log
ORDER BY created_date DESC
LIMIT 10;

-- 3. Unsummarized VOICE CDRs in a range
SELECT COUNT(*) FROM voice_rated_cdr
WHERE modified_date BETWEEN '2026-01-01 00:00:00' AND '2026-01-02 00:00:00'
  AND (incoming_rating_status = 'RATED' OR outgoing_rating_status = 'RATED')
  AND is_deleted = false
  AND is_summarized = false
  AND line_of_business = 'INTERCONNECT';

-- 4. Rated summaries for a specific date and account
SELECT * FROM rated_summary
WHERE summary_date = '2026-01-01'
  AND account_code = 'ACC001'
ORDER BY direction, service_type;

-- 5. Detect duplicate summary rows (should return empty)
SELECT summary_date, account_code, direction, service_type, rating_type,
       source_prefix, destination_prefix, zone_name, applied_rate, COUNT(*) AS cnt
FROM rated_summary
WHERE is_deleted = false
GROUP BY summary_date, account_code, direction, service_type, rating_type,
         source_prefix, destination_prefix, zone_name, applied_rate
HAVING COUNT(*) > 1;

-- 6. Manually reset a stuck scheduler (use only as last resort)
UPDATE scheduler_status
SET status = 'PENDING',
    next_scheduler_start_time = NOW(),
    targeted_rated_cdr_start_time = '2026-01-01 00:00:00'
WHERE scheduler_status_id = <id>;
```

---

## Appendix B — Key Source Files

| Class | Path |
|---|---|
| SchedulerService | `src/main/java/com/xcess/ocs/summaryengine/service/SchedulerService.java` |
| SchedulerExecutorService | `src/main/java/com/xcess/ocs/summaryengine/service/SchedulerExecutorService.java` |
| RatedSummaryService | `src/main/java/com/xcess/ocs/summaryengine/service/RatedSummaryService.java` |
| CdrMarkSummarizedService | `src/main/java/com/xcess/ocs/summaryengine/service/CdrMarkSummarizedService.java` |
| DynamicQueryService | `src/main/java/com/xcess/ocs/summaryengine/service/DynamicQueryService.java` |
| SchedulerConfigurationService | `src/main/java/com/xcess/ocs/summaryengine/service/SchedulerConfigurationService.java` |
| SchedulerConfiguration | `src/main/java/com/xcess/ocs/entity/SchedulerConfiguration.java` |
| SchedulerStatus | `src/main/java/com/xcess/ocs/entity/SchedulerStatus.java` |
| SchedulerAuditLog | `src/main/java/com/xcess/ocs/entity/SchedulerAuditLog.java` |
| RatedSummary | `src/main/java/com/xcess/ocs/entity/RatedSummary.java` |
| RatedSummaryRepository | `src/main/java/com/xcess/ocs/repository/RatedSummaryRepository.java` |
| VoiceRatedCdrRepository | `src/main/java/com/xcess/ocs/repository/VoiceRatedCdrRepository.java` |
| query-config.yml | `src/main/resources/query-config.yml` |
| SchedulerConfigurationController | `src/main/java/com/xcess/ocs/controller/SchedulerConfigurationController.java` |
| RatedSummaryController | `src/main/java/com/xcess/ocs/controller/RatedSummaryController.java` |
