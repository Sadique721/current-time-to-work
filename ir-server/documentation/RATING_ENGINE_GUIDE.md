# Rating Engine — Complete Developer Guide

> **Purpose:** This guide provides a detailed technical deep dive into the OCS Rating Engine. It explains exactly how an unrated CDR (Voice, SMS, or Usage) is ingested, how rate packages are resolved, how the in-memory RadixTrie system works for ultra-fast prefix matching, and how final costs are calculated and stored.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map & Data Flow](#2-component-map--data-flow)
3. [Trie Data Structures (RadixTrie)](#3-trie-data-structures-radixtrie)
4. [Step-by-Step Rating Flow](#4-step-by-step-rating-flow)
5. [Cost Calculation Logic](#5-cost-calculation-logic)
6. [Scenario Playbook — Positive & Negative Cases](#6-scenario-playbook--positive--negative-cases)
7. [Checklist for New Developers](#7-checklist-for-new-developers)

---

## 1. System Overview

The OCS Rating Engine is designed for high-throughput, low-latency rate lookups. To achieve this, it relies heavily on an in-memory **Radix Trie** (a compressed prefix tree) caching layer. 

Instead of querying the database for every CDR to find matching prefixes (which is extremely slow), the system loads all `RatePackage` and `RateDetails` (including Zone mappings) into memory at startup. When a CDR arrives from Kafka, the lookup against millions of prefixes takes fractions of a millisecond.

---

## 2. Component Map & Data Flow

```text
Kafka (Raw CDR)
      │
      ▼
MessageProcessor
      │ (Parses CSV, identifies LOB and ServiceType)
      ▼
CdrRatingIntegrationService
      │ (The core orchestrator for rating)
      │
      ├── 1. Account Resolution (Finds Account -> ProductPlan -> RatePackage)
      │
      ├── 2. Rate Lookup (Memory Trie Search)
      │      ├── Interconnect ──► RateLookupService ──► RateTrieService
      │      └── Roaming ───────► ZoneLookupService ──► ZonePrefixTrie
      │
      ├── 3. Cost Calculation (Applies pulses, units, and rounding)
      │
      └── 4. State Update (marks CDR as Rated or Failed)
             │
             ▼
Database (voice_rated_cdr, sms_rated_cdr, usage_rated_cdr)
```

---

## 3. Trie Data Structures (RadixTrie)

To understand rating, you must understand the `RadixTrie` implementation. 

### Why RadixTrie?
A standard database lookup for longest-prefix match requires complex SQL (`WHERE '12345' LIKE prefix || '%' ORDER BY length(prefix) DESC`). This is highly inefficient.
A RadixTrie compresses common prefixes into single nodes. Searching for the longest matching prefix for a 15-digit number takes exactly 15 operations (O(k) where k is the length of the string), independent of how many millions of rates exist.

### Initialization (`RateTrieService` & `ZoneLookupService`)
At application startup (`@PostConstruct`), the system fetches all active `RatePackage`s from the database.
- **For Standard Packages:** It builds a `RateTrie` which internally contains three `RadixTrie`s:
  1. `sourceDestTrie`: Looks up by Source Prefix, which then maps to a secondary Trie for Destination Prefix.
  2. `destOnlyTrie`: Looks up only by Destination Prefix.
  3. `plmnTrie`: Looks up by Home PLMN (used heavily in Roaming).
- **For Zone Packages:** It builds a `ZonePrefixTrie`. It maps thousands of number prefixes to a named Zone (e.g., "South Asia").

The tries are stored in a `ConcurrentHashMap` keyed by `ratePackageId` for instant retrieval.

---

## 4. Step-by-Step Rating Flow

When `CdrRatingIntegrationService.applyRating()` is called, the following steps occur. Note that for Interconnect, rating happens **twice** per CDR (once for the Incoming side, once for the Outgoing side).

### Step 1: Timestamp & Null Validation
The `startTime` of the CDR is extracted. If it is null (for Voice), the CDR immediately fails with `INVALID_TIMESTAMP`. This timestamp is critical for resolving time-based rate packages.

### Step 2: Rate Package Resolution (`getRatePackage`)
Before we can check a Trie, we need to know *which* Trie to check.
1. The system looks up the `Account` using `incomingAccountId` (and subsequently `outgoingAccountId`).
2. It fetches the `ProductPlan` attached to the Account.
3. It finds the `ProductPlanAssociation` matching the CDR's `ServiceType` (Voice, SMS, Usage).
4. It iterates through the `RatePackageAssociation`s. It filters out packages where the `callTimestamp` falls outside the effective date range. It also evaluates any dynamic expressions (e.g., `callType=IDD`) against the CDR properties via Java Reflection.
5. Returns the matched `RatePackage`.

### Step 3: Rate Lookup
Once the `RatePackage` is identified, the system attempts to find the specific `RateDetails` (the price row).

**Path A: Non-Zone Packages (Prefix-Based)**
- Calls `RateLookupService.findBestRate`.
- Searches the memory Trie using the calling number (source) and called number (destination).
- The Trie finds the longest matching source prefix. If found, it searches the destination prefix.
- If no source match is found, it falls back to the Destination-only Trie.
- Finally, it validates that the `callTimestamp` falls within the `startTime`/`endTime` of the specific rate row.

**Path B: Zone Packages (Zone-Based)**
- Calls `ZoneLookupService.getZoneForNumber`.
- Searches the `ZonePrefixTrie` using the called number.
- It finds the longest matching prefix and returns the attached **Zone Name** (e.g., "EU").
- It then filters the `RatePackage`'s rate details to find the price row that explicitly maps to `zoneName = "EU"`.

---

## 5. Cost Calculation Logic

Once the best `RateDetails` is found, the final cost is calculated. 

### Voice Cost Calculation
1. **Duration Extraction:** Calculated in seconds using `ChronoUnit.SECONDS.between(startTime, endTime)`.
2. **Pulse Application:** Rates aren't always per second. A `Pulse` might be "60 seconds". 
   - `durationInPulseUnits` is calculated (e.g., converting seconds to minutes).
   - The billable units are calculated by dividing the duration by the `pulse.noOfUnits`, applying a specific rounding rule (e.g., `CEILING` to charge a full minute for a 2-second call).
3. **Multiplication:** `Cost = Billable Units * Rate`.
4. **Price Rounding:** The final monetary amount is rounded (e.g., `ROUND_HALF_UP` to 4 decimal places) based on the package configuration.

### SMS Cost Calculation
1. **Event Extraction:** Reads the `eventNos` (message count).
2. **Multiplication:** `Cost = Event Count * Rate`. No pulses involved.

### Usage (Data) Cost Calculation
1. **Volume Normalization:** Data can arrive in KB, MB, or Bytes. `normalizeDataVolume()` converts the raw usage into the `Pulse` unit defined on the Rate Package (e.g., billing per MB).
2. **Multiplication:** `Cost = Normalized Volume * Rate`.

### State Finalization
- `markIncomingAsRated` / `markOutgoingAsRated` is called.
- The `rate`, `totalCost`, `ratePackageId`, and matched prefixes are explicitly saved directly on the CDR entity.
- The CDR is then saved to the database by the `MessageProcessor`.

---

## 6. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard International Voice Call (Positive)
**Context:** An interconnect voice call from US (+1) to UK (+44). 
**Flow:**
1. CDR arrives. LOB = `INTERCONNECT`.
2. Incoming account resolves to "US_PARTNER", Outgoing resolves to "UK_PARTNER".
3. **Incoming Rating:** Looks up US_PARTNER's rate package. The destination Trie finds the longest prefix `44` and returns a rate of $0.05/min. Pulse is 60s (CEILING). Duration is 65s. Billable units = 2. Cost = $0.10.
4. **Outgoing Rating:** Looks up UK_PARTNER's rate package. Finds prefix `44`. Returns rate of $0.03/min. Cost = $0.06.
5. CDR is marked as `RATED` on both sides and saved.

### ✅ Scenario 2: Zone-Based Roaming Call (Positive)
**Context:** A roaming subscriber makes a TAP_OUT call to Spain (+34).
**Flow:**
1. LOB = `ROAMING`.
2. Rate package is resolved as `ZONE_DESTINATION_BASED`.
3. `ZonePrefixTrie` is queried with the called number (+34...). It matches the prefix `34` which maps to zone `"EUROPE"`.
4. The `RateDetails` list is filtered for `zoneName = "EUROPE"`. A rate of $0.15/min is found.
5. Cost is calculated and saved.

### ❌ Scenario 3: No Matching Prefix Found (Negative)
**Context:** A call is made to an obscure satellite network prefix (+881) that is not defined in the partner's rate package.
**Flow:**
1. `RateLookupService` searches the Trie.
2. The Trie returns `null` because no node matches `881`.
3. `CdrRatingIntegrationService` catches the `null` return.
4. Calls `markIncomingAsUnrated(cdr, "NO_MATCHING_RATE")`.
**Handling:** The CDR is saved to the database with a status of `UNRATED`. An administrator must add the prefix to the package and trigger a re-rating job. The system does *not* crash.

### ❌ Scenario 4: Missing Account / Null Product Plan (Negative)
**Context:** A CDR arrives with `incomingAccountId = "UNKNOWN_ACC"`.
**Flow:**
1. `getRatePackage()` attempts to fetch the Account from the DB.
2. Returns `null`.
3. `CdrRatingIntegrationService` immediately halts rating for the incoming leg.
4. Calls `markIncomingAsFailed(cdr, "NULL_ACCOUNT_ID_AND_PLMN")` or `"NO_RATE_PACKAGE"`.
**Handling:** CDR saved as `FAILED`. Requires partner configuration correction before re-rating.

### ❌ Scenario 5: Cache Miss / Trie Rebuild Trigger (Negative to Positive Recovery)
**Context:** An admin adds a new Zone Prefix directly into the database, but a cache invalidate event was somehow dropped.
**Flow:**
1. `ZoneLookupService` searches the L1 Trie Cache for the package.
2. Trie returns `null`.
3. The service logs `"ZoneLookup: no zone found... rebuilding from DB"`.
4. The system safely rebuilds the specific Trie for that package from the database synchronously.
5. It retries the lookup against the freshly built Trie and finds the new prefix.
**Handling:** Self-healing cache mechanism ensures rating never fails permanently due to a missed synchronization event.

---

## 7. Checklist for New Developers

If you are modifying or debugging the Rating Engine:

- [ ] **Beware of Memory Leaks:** If you add new maps or caches to `RateTrieService`, ensure there is a mechanism to `removePackage()` when packages are deleted in the UI. Otherwise, the JVM heap will fill up with orphaned Tries.
- [ ] **Check Trie Initialization:** If rates are suddenly returning `null` in a specific environment, verify the application logs on startup. Search for `"Initializing rate tries"`. If this failed, the Tries are empty.
- [ ] **Understand Pulse Math:** Rating math is highly sensitive. Always use `BigDecimal` for currency and duration division. Understand the difference between `ROUND_HALF_UP` and `CEILING` (often used for Telecom pulses).
- [ ] **Reflection is Slow:** The `isExpressionMatch` uses Java Reflection to evaluate rules like `callType=IDD`. This is slightly slower than direct getters. Avoid creating complex expressions if simple enum matching suffices.
