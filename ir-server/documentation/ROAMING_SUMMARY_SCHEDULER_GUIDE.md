# Roaming Summary Scheduler & TAP OUT Guide

> **Purpose:** This developer guide details the TAP OUT lifecycle within the OCS system. It covers how outbound TAP files are generated from our local rated CDRs, how the binary file is encoded, and finally how the `RoamingSummaryScheduler` aggregates these records for billing purposes.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map & Data Flow](#2-component-map--data-flow)
3. [Phase 1: TAP OUT File Generation](#3-phase-1-tap-out-file-generation)
4. [Phase 2: Roaming Summary Scheduler (Daily Cron)](#4-phase-2-roaming-summary-scheduler-daily-cron)
5. [Phase 3: Summary Generation Logic](#5-phase-3-summary-generation-logic)
6. [Scenario Playbook — Positive & Negative Cases](#6-scenario-playbook--positive--negative-cases)
7. [Checklist for New Developers](#7-checklist-for-new-developers)

---

## 1. System Overview

In roaming, when a partner's subscriber roams onto *our* network, we generate local CDRs. To bill the partner, we must encode these CDRs into standard GSMA TAP binary files and send them over SFTP (**TAP OUT**). 

Because the billing engine cannot efficiently process millions of individual CDR rows at the end of the month, the `RoamingSummaryScheduler` runs nightly. It takes all the CDRs that were successfully exported as TAP OUT files and aggregates them into grouped `TapOutRatedSummary` records.

---

## 2. Component Map & Data Flow

```text
Local Rated CDRs (Voice, SMS, Usage)
       │
       ▼
1. TapOutFileGenerationService (Encodes ASN.1 binary, pushes to SFTP)
       │
       ├──► Updates CDRs: isTapOutGenerated = true
       ├──► Creates TapFileRecord (TAP_OUT)
       │
       ▼
2. RoamingSummaryScheduler (Cron job: 3:00 AM)
       │
       ▼
3. RoamingSummaryGenerationService (Fetches unsummarized TAP OUT CDRs)
       │
       ├──► Groups by: TapFileId + ServiceType + ZoneName
       ├──► Aggregates durations, bytes, and financial charges
       ├──► Saves to tap_out_rated_summaries table
       └──► Updates CDRs: isSummarized = true
```

---

## 3. Phase 1: TAP OUT File Generation

**Service:** `TapOutFileGenerationService`

When triggered for a specific time window and Partner:
1. **Query:** Fetches all rated Voice, SMS, and Usage CDRs for the partner's `HPLMN` where `isTapOutGenerated = false`.
2. **Profile Resolution:** Looks up the partner's assigned `TapProfileGroup`.
3. **Encoding (ASN.1):** 
   - Uses the `TapFieldPathResolver` to map flat local CDR properties (like `callingNumber`, `durationSeconds`) back into nested GSMA ASN.1 `CallEventDetail` structures based on the assigned TAP Profile.
   - Applies strict formatting, such as converting decimals based on `TAP_DECIMAL_PLACES` (default 4).
4. **Binary Generation:** Encodes the `DataInterChange` into a byte array and writes it to the local `outboxDir`.
5. **SFTP Push:** Calls `TapSftpPushService` to deliver the file to the partner or DCH.
6. **State Update:** Creates a `TapFileRecord` in the database and updates all included CDRs to `isTapOutGenerated = true`.

---

## 4. Phase 2: Roaming Summary Scheduler (Daily Cron)

**Component:** `RoamingSummaryScheduler`

This is the automated orchestrator that runs nightly (configured via `${roaming.summary.cron:0 0 3 * * ?}`).

### Concurrency & Lifecycle
- **Locking:** It calls `waitForSchedulerLock()`. If a previous day's job is stuck in a `RUNNING` state, it will wait up to 60 minutes. If the lock doesn't clear, it safely skips execution to prevent DB deadlocks.
- **Window:** It processes exactly "yesterday's" data (`00:00:00` to `23:59:59`).
- **Graceful Shutdown:** If the Kubernetes pod/server restarts during processing, a `@PreDestroy` hook intercepts the shutdown, sets an `interrupted` flag, and cleanly updates the job status to `INTERRUPTED` without corrupting data.

---

## 5. Phase 3: Summary Generation Logic

**Service:** `RoamingSummaryGenerationService`

1. **Query:** It selects all Voice/SMS/Usage CDRs where `isTapOutGenerated = true` and `isSummarized = false` for the scheduler's time window.
2. **Grouping:** It partitions the CDRs by the `TapFileId` they belong to. Within that file, it further groups by `ServiceType` (Voice, SMS, Usage) and `ZoneName`.
3. **Aggregation Math:**
   - **Voice:** Counts records and sums `durationSeconds`.
   - **SMS:** Sums the `eventNos` (number of SMS parts).
   - **Usage (Data):** Normalizes units. If the local CDR was recorded in KB/MB/GB, it multiplies by the respective factors (e.g., 1024, 1048576) to standardize all usage into **BYTES**.
   - **Financials:** Sums the `outgoingTotalCost` to represent the final `totalCharge`.
4. **Persistence:** Saves a `TapOutRatedSummary` row for each group.
5. **State Update:** Bulk-updates the processed CDRs to `isSummarized = true`.

---

## 6. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard End-to-End Success (Positive)
**Context:** Yesterday, we generated 3 TAP OUT files for Vodafone containing 10,000 CDRs.
**Flow:**
1. Scheduler triggers at 3:00 AM.
2. Acquires the lock and logs `RUNNING`.
3. Fetches 10,000 unsummarized CDRs.
4. Groups them by the 3 TapFileIds, and further by Zone (e.g., EU, ASIA).
5. Aggregates data, creating exactly 6 `TapOutRatedSummary` rows (if 2 zones per file).
6. Updates 10,000 CDRs to `isSummarized = true`.
7. Job logs `SUCCESS`.

### ❌ Scenario 2: Scheduler Concurrency / Manual Override (Negative)
**Context:** An admin manually triggers the summary endpoint via Swagger at 3:05 AM while the cron job is currently executing.
**Flow:**
1. Manual thread calls `waitForSchedulerLock()`.
2. Detects `statusService.isRunning() == true`.
3. Waits in a sleep loop for up to 60 minutes.
4. If the cron finishes, the manual job proceeds (finding 0 unsummarized CDRs, acting as a safe no-op). If it times out, it logs `"timeout waiting for lock — skipping"` and terminates.
**Handling:** Completely prevents database deadlocks and double-counting of summaries.

### ❌ Scenario 3: Missing Mandatory ASN.1 Field during TAP OUT (Negative)
**Context:** During `TapOutFileGenerationService`, a CDR is missing a field that the partner's `TapProfile` marks as mandatory (e.g., `imsi`).
**Flow:**
1. Field mapping fails validation.
2. Service throws `IllegalArgumentException: "Mandatory field [IMSI] missing..."`.
3. Generation for that partner halts.
**Handling:** No TAP OUT file is created. No CDRs are marked as generated. The `RoamingSummaryScheduler` will safely ignore these CDRs tonight. An engineer must fix the CDR data or update the Profile, then re-trigger generation.

### ❌ Scenario 4: Unexpected Server Shutdown (Negative -> Recovery)
**Context:** The server receives a SIGTERM from Kubernetes at 3:02 AM while summarizing 500,000 CDRs.
**Flow:**
1. Spring triggers `@PreDestroy` -> `onShutdown()`.
2. `interrupted = true` is set.
3. The main loop detects the flag before the next batch of file processing.
4. It halts immediately and updates the DB status to `INTERRUPTED`.
5. Because DB transactions (`@Transactional`) wrap the summary creation and the CDR `isSummarized` flag update together, no partial state is left behind.
**Handling:** The next manual or scheduled run will simply pick up the exact CDRs that failed to commit, ensuring 100% data integrity.

---

## 7. Checklist for New Developers

- **Double-Check Byte Conversions:** The TAP standard requires Data Volume to be encoded in octets (bytes). If you are adding a new data unit type (e.g., TB), you MUST update the multiplier logic in `RoamingSummaryGenerationService`.
- **Beware of `@Transactional` Scope:** The summary generation is tightly coupled in a transaction. If processing millions of CDRs, monitor JVM heap usage. The bulk updates are efficient, but pulling massive lists into memory at once can cause OOMs on very large networks.
- **Do not bypass the Locking Service:** If writing custom scripts to trigger summaries, always use the `RoamingSummaryScheduler` entry point or respect `statusService.isRunning()` to avoid corrupting financial data.
