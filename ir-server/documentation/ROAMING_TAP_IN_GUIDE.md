# Roaming TAP IN Processing — Complete Developer Guide

> **Purpose:** This guide documents the end-to-end flow of processing incoming TAP (Transferred Account Procedure) files. It covers pulling files from Clearing Houses/Partners, decoding the GSMA binary formats, extracting CDRs, rating them using zone-based logic, and summarizing them for billing.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map & Data Flow](#2-component-map--data-flow)
3. [Step 1: Pulling TAP Files (SFTP)](#3-step-1-pulling-tap-files-sftp)
4. [Step 2: Decoding the Binary TAP File](#4-step-2-decoding-the-binary-tap-file)
5. [Step 3: CDR Extraction & Profile Mapping](#5-step-3-cdr-extraction--profile-mapping)
6. [Step 4: Roaming Rating & Currency Conversion](#6-step-4-roaming-rating--currency-conversion)
7. [Step 5: Rated Summary Generation](#7-step-5-rated-summary-generation)
8. [Scenario Playbook — Positive & Negative Cases](#8-scenario-playbook--positive--negative-cases)

---

## 1. System Overview

In a roaming agreement, when our subscriber travels abroad and uses a partner network, the partner sends us a **TAP IN** file. This file contains billing records (CDRs) detailing what our subscriber did (Voice, SMS, Data) and how much the partner intends to charge us.

The system must reliably download these files, parse the strict ASN.1 binary format, map the complex nested fields into a flat CDR structure, rate the calls against our own roaming agreements, and generate daily summaries.

---

## 2. Component Map & Data Flow

```text
External SFTP (DCH or Direct Partner)
       │
       ▼
1. TapSftpPullService (Downloads to local inbox)
       │
       ▼
2. TapFileProcessingService (Orchestrator, updates TapFileStatus)
       │
       ├──► 3. TapFileDecoderService (Parses GSMA ASN.1 binary)
       │
       ├──► 4. TapCdrExtractorService (Maps ASN.1 paths to TapCdrDTO)
       │
       ├──► 5. RoamingRatingService (Rates using ZonePrefixTrie & CdrRatingIntegrationService)
       │
       └──► 6. RoamingRatedSummaryService (Groups and aggregates totals)
              │
              ▼
    Database (roaming_cdrs, roaming_rated_summaries)
```

---

## 3. Step 1: Pulling TAP Files (SFTP)

**Service:** `TapSftpPullService`

The service connects to SFTP servers to download files into a local watch directory (e.g., `tap-files/inbox`). It supports two modes:
- **DCH Mode (Data Clearing House):** Iterates through active `ClearingHouse` records (e.g., Syniverse, Comfone) and connects using the configured `sftpHost`, port, and `sftpInboxPath`.
- **Direct Mode:** Iterates through `Partner` records that bypass a DCH. Connects directly to the partner's SFTP server.

*Note: Once downloaded, a file watcher (or cron) triggers the `TapFileProcessingService`.*

---

## 4. Step 2: Decoding the Binary TAP File

**Service:** `TapFileDecoderService`

TAP files are strictly encoded in ASN.1. The system uses a GSMA SDK (`TapFiles.read()`) to decode the file into a Java `DataInterChange` object.

The decoder validates the file and extracts header/footer metadata:
- **BatchControlInfo:** Validates the TAP version and ensures the `senderTadig` and `recipientTadig` inside the file match the expected values.
- **AccountingInfo:** Extracts `localCurrency` and `tapDecimalPlaces` (crucial for converting integer TAP charges into floating-point decimals).
- **AuditControlInfo:** Extracts `totalRecords` and `totalCharge` to verify file integrity.

**State Transition:** `RECEIVED` → `DECODING` → `DECODED`

---

## 5. Step 3: CDR Extraction & Profile Mapping

**Service:** `TapCdrExtractorService`

Because different partners encode TAP files slightly differently (e.g., placing the called number in different nested nodes), the system uses configurable **Tap Profiles**.

1. **Event Dispatch:** Iterates through `CallEventDetail` records. Identifies the event type (`MO_VOICE`, `MT_VOICE`, `GPRS`, `MO_SMS`).
2. **Profile Resolution:** Looks up the `TapProfileGroup` assigned to the Partner and selects the profile matching the `ServiceType`.
3. **Field Mapping:** Uses `TapFieldPathResolver` to navigate the ASN.1 tree using configured paths (e.g., `mobileOriginatedCall.basicCallInformation.destination.calledNumber`).
4. **Validation:** If a field is mapped as `isMandatory` but missing in the file, it throws an error (unless a `defaultValue` is configured).

---

## 6. Step 4: Roaming Rating & Currency Conversion

**Service:** `RoamingRatingService`

The extracted `TapCdrDTO`s are saved as `RoamingCdr`s. We must now "Rate" them to determine our own internal charge.

1. **Integration:** Calls the unified `CdrRatingIntegrationService`.
2. **Zone Resolution:** Roaming rating relies on Zones. `ZoneLookupService` uses the `ZonePrefixTrie` to map the `visitedPlmn` to a Zone (e.g., "EUROPE").
3. **Calculation:** Matches the zone against the partner's Rate Package, applies pulse logic, and calculates `ourCharge`.
4. **Currency Conversion:** TAP charges are in the partner's `localCurrency` (e.g., EUR). If the partner's billing agreement requires USD, it calls `ExchangeRateRepository` to fetch the exact exchange rate for the `callStartTime` and converts `ourCharge` to the billing currency.

**State Transition:** `DECODED` → `RATING` → `RATED`

---

## 7. Step 5: Rated Summary Generation

**Service:** `RoamingRatedSummaryService`

To prevent the invoice engine from scanning millions of CDRs, the system immediately pre-aggregates the rated CDRs.

1. **Grouping:** CDRs are grouped by a composite key: `TapDirection | ServiceType | ZoneName`.
2. **Aggregation:**
   - **Voice:** Sums up total calls and total duration (seconds).
   - **SMS:** Sums up total SMS (`eventNos`).
   - **Data (USAGE):** Normalizes KB/MB into BYTES and sums it up.
   - **Financials:** Sums `tapCharge` (what they charge us) and `ourCharge` (our calculated cost).
3. **Persistence:** Saves the grouped totals into the `roaming_rated_summaries` table. The individual `RoamingCdr`s are flagged with `isSummarized = true`.

---

## 8. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard Voice Call (Positive)
**Context:** A partner (VODAFONE) sends a TAP file containing a 5-minute Voice call.
**Flow:**
1. SFTP downloads the file.
2. Decoder successfully parses the ASN.1 format.
3. Extractor uses VODAFONE's TAP profile. Finds the called number in the expected ASN.1 node.
4. Rater maps the call to the "EU Zone". Calculates `ourCharge` at $0.05/min.
5. Currency converter checks the exchange rate for the call date.
6. Summarizer aggregates the 5-minute call into the daily Voice/EU bucket.

### ❌ Scenario 2: Corrupt Binary File (Negative)
**Context:** The SFTP transfer drops packets, resulting in a truncated TAP file.
**Flow:**
1. `TapFileDecoderService` attempts `TapFiles.read()`.
2. GSMA SDK throws an `IOException` or returns `Optional.empty()`.
3. Service catches the error and calls `markError()`.
4. `TapFileRecord` status is set to `ERROR`. Processing halts immediately.
**Handling:** No CDRs are extracted. An admin must re-download or request the partner to re-send the file.

### ❌ Scenario 3: Missing Mandatory ASN.1 Field (Negative)
**Context:** A partner modifies their TAP generation and stops sending the `chargeableUnits` field for GPRS calls, but our profile expects it.
**Flow:**
1. Decoder succeeds. Extractor processes the GPRS event.
2. `TapFieldPathResolver` returns `null` for `chargeableUnits`.
3. The mapping is marked `isMandatory = true`.
4. Throws `IllegalArgumentException: "Mandatory TAP IN field missing: DataVolume..."`.
5. `TapFileProcessingService` catches the exception. Status set to `ERROR`.
**Handling:** An admin must update the Partner's TAP Profile to either point to the new ASN.1 node or provide a default value.

### ❌ Scenario 4: Missing Exchange Rate (Negative -> Graceful Degradation)
**Context:** The CDR is rated, but the system needs to convert EUR to BHD. The Exchange Rate API was down for the call date, and no fallback exists.
**Flow:**
1. `RoamingRatingService` attempts to fetch the exchange rate.
2. `exchangeRateRepository` returns empty.
3. Service logs a warning: `"No exchange rate found for EUR->BHD, using unconverted amount"`.
4. The CDR is saved with the original EUR amount, but it might cause billing discrepancies.
**Handling:** Developers/Admins should ensure the Exchange Rate Scheduler is running correctly. A re-rating script can be run later once the rate is manually inserted.

### ❌ Scenario 5: Missing Rating Zone (Negative)
**Context:** A subscriber visits a new obscure network whose PLMN prefix is not defined in any Zone.
**Flow:**
1. `ZoneLookupService` searches the `ZonePrefixTrie`. Returns `null`.
2. `CdrRatingIntegrationService` fails to rate the CDR.
3. CDR `ratingStatus` is marked as `FAILED` with reason `NO_MATCHING_RATE` or `NO_ZONE_FOUND`.
4. Summarizer ignores the FAILED CDR (or groups it with 0 cost).
**Handling:** Admin adds the PLMN to the appropriate Zone in the UI, clears the summary, and clicks "Re-Rate".
