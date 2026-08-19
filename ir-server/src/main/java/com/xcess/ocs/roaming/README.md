# Roaming Features — Implementation Guide

## Overview

This document covers the full roaming billing pipeline added to the OCS platform:

```
TAP IN File
    ↓
TapFileProcessingService  (decode ASN.1)
    ↓
RoamingRatingService      (zone lookup → rate lookup → cost calculation)
    ↓
RoamingRatedSummary
    ↓
TapOutFileGenerationService (ASN.1 encode → TAP OUT file)
    ↓
ClearingHouse (Syniverse / BICS)
```

---

## 1. Data Model Changes

### 1.1 ClearingHouse
Represents a roaming clearing house (e.g. Syniverse, BICS).

| Field            | Type                    | Description                              |
|------------------|-------------------------|------------------------------------------|
| id               | BIGINT PK               | Auto-generated                           |
| name             | VARCHAR(100) UNIQUE     | e.g. "Syniverse", "BICS"                 |
| type             | ENUM(DCH, FCH, BOTH)    | Data/Financial/Both clearing house       |
| status           | ENUM(ACTIVE, INACTIVE)  |                                          |
| default_currency | VARCHAR(3)              | ISO 4217 e.g. "USD"                      |
| timezone         | VARCHAR(50)             | e.g. "UTC", "Europe/London"              |
| protocols        | → clearing_house_protocols | SFTP / API / AS2 (join table)         |

### 1.2 ClearingHouseProtocol (join table)
| Field             | Type                      |
|-------------------|---------------------------|
| clearing_house_id | FK → clearing_houses      |
| protocol          | ENUM(SFTP, API, AS2)      |

### 1.3 ExchangeRate
| Field           | Type            | Description                              |
|-----------------|-----------------|------------------------------------------|
| base_currency   | VARCHAR(3)      | Currency converting FROM (e.g. "USD")    |
| target_currency | VARCHAR(3)      | Currency converting TO (e.g. "INR")      |
| rate            | DECIMAL(18,6)   | 1 base = rate target                     |
| valid_from      | DATE            | Rate effective date                      |
| source          | VARCHAR(50)     | Provider: "ECB", "XE", "MANUAL"          |

### 1.4 Partner (updated)
- Added `clearing_house_id` FK → `clearing_houses`
- For ROAMING partners: set `line_of_business = ROAMING`, `interconnect_type = ROAMING`, `tadig_code` required

### 1.5 Agreement (updated)
| Field         | Description                                                  |
|---------------|--------------------------------------------------------------|
| home_plmn     | TAP IN: our TADIG. TAP OUT: partner TADIG                    |
| visitor_plmn  | TAP IN: partner TADIG. TAP OUT: our TADIG                    |
| tap_direction | TAP_IN or TAP_OUT                                            |

### 1.6 RatePackage (updated)
- Added `currency` (VARCHAR 3) — ISO 4217 currency for this package's rates

### 1.7 Zone (updated)
- Removed `min_length`, `max_length` (redundant — PLMNs are always 5-6 chars)
- Added `priority` INT (default=100) — lower number = higher priority in zone lookup conflict

### 1.8 TapOutConfig
| Field               | Description                                          |
|---------------------|------------------------------------------------------|
| type                | GLOBAL or PARTNER_SPECIFIC                           |
| our_tadig           | Our operator's TADIG code (sender in TAP OUT file)   |
| partner_id          | Null for GLOBAL, set for PARTNER_SPECIFIC            |
| clearing_house_id   | FK → clearing_houses                                 |
| file_transfer_config| JSON/text with SFTP/API transfer details             |
| is_active           | Boolean                                              |

**Lookup rule:** PARTNER_SPECIFIC overrides GLOBAL for a given partner.

### 1.9 ServiceType (updated)
```
Existing (INTERCONNECT — unchanged):  VOICE, SMS, USAGE, DATA
New      (ROAMING):                   VOICE_MO, VOICE_MT, SMS_MO
```

---

## 2. Zone-Based Rating Architecture

### 2.1 Zone Lookup Flow
```
RoamingCdr.visitedPlmn
    ↓
ZoneLookupService.getZoneForNumber(groupId, visitedPlmn)
    ↓  L1: ZonePrefixTrie (in-memory, microseconds)
    ↓  L2: DB rebuild on cache miss
    ↓
zoneName (e.g. "ZONE_EU")
    ↓
ZoneRateMappingRepository.findActiveByGroupIdAndZoneId()
    ↓
RatePackage [type=ZONE, lineOfBusiness=ROAMING]
    ↓
RateLookupService.findBestRate() → RateDetails
    ↓
calculateTotalCost() → pulse-based billing
```

### 2.2 Zone Priority Resolution
When a visitedPlmn matches multiple zones:
1. **Lower `priority` number wins** (admin-defined, e.g. priority=1 beats priority=2)
2. **Tiebreak: longer prefix wins** (more specific match)

Example:
```
Zone: ZONE_EU_PREMIUM  priority=1  prefix: "23801"
Zone: ZONE_EU          priority=2  prefix: "238"
Zone: ZONE_GLOBAL      priority=3  prefix: "2"

visitedPlmn = "23801" → matches all three → ZONE_EU_PREMIUM wins (priority=1)
visitedPlmn = "23805" → matches ZONE_EU + ZONE_GLOBAL → ZONE_EU wins (priority=2)
```

### 2.3 Cache Structure
```java
// One ZonePrefixTrie per RatePackageGroup
Map<Long, ZonePrefixTrie> trieCache   // key = ratePackageGroupId

// Inside ZonePrefixTrie: character trie with priority at each end-node
// Conflict resolution: lowest zonePriority wins, depth as tiebreaker
```

---

## 3. Setup Guide (Step-by-Step)

### Step 1 — Create Clearing House
```
POST /api/clearing-houses
{
  "name": "Syniverse",
  "type": "DCH",
  "status": "ACTIVE",
  "defaultCurrency": "USD",
  "timezone": "UTC",
  "protocols": ["SFTP", "API"]
}
```

### Step 2 — Create Roaming Partner
```
POST /api/partners
{
  "partnerName": "Vodafone DE",
  "partnerCode": "VFDE01",
  "partnerType": "VENDOR",
  "lineOfBusiness": "ROAMING",
  "interconnectType": "ROAMING",
  "tadigCode": "DEUVO",
  "billingCurrency": "USD",
  "clearingHouseId": 1,
  ... (other required fields)
}
```

### Step 3 — Create Zones
```
POST /api/zones
{
  "zoneName": "ZONE_EU",
  "prefixPattern": "23801,23802,26201,26202,23430",
  "description": "European Union zone",
  "priority": 1
}

POST /api/zones
{
  "zoneName": "ZONE_GLOBAL",
  "prefixPattern": "2,3,4,5,6,7,8,9",
  "description": "Global fallback zone",
  "priority": 99
}
```

### Step 4 — Create Exchange Rate
```
POST /api/exchange-rates
{
  "baseCurrency": "USD",
  "targetCurrency": "INR",
  "rate": 83.50,
  "validFrom": "2025-01-01",
  "source": "ECB"
}
```

### Step 5 — Create Rate Package (ZONE type)
```
POST /api/rate-packages
{
  "packageName": "Roaming EU Voice MO",
  "ratePackageType": "ZONE",
  "lineOfBusiness": "ROAMING",
  "serviceType": "VOICE_MO",
  "currency": "USD",
  "pulseId": 1,
  "rounding": "DEFAULT",
  "priceRounding": "DEFAULT",
  "type": "BUYING"
}
```

### Step 6 — Create Rate Package Group and associate Rate Package
```
POST /api/rate-package-groups
{
  "name": "Roaming Buying Group",
  "description": "Zone-based buying rates",
  "packageType": "BUYING"
}

POST /api/rate-package-groups/{groupId}/associations
{
  "ratePackageId": <id from step 5>,
  "startTime": "2025-01-01T00:00:00",
  "endTime": "2099-12-31T23:59:59"
}
```

### Step 7 — Create Zone Rate Mapping
Links a Zone to a RatePackage within a RatePackageGroup:
```
POST /api/zone-rate-mappings
{
  "ratePackageGroupId": <groupId>,
  "zoneId": <zoneId>,
  "ratePackageId": <ratePackageId>,
  "effectiveFrom": "2025-01-01T00:00:00",
  "effectiveTo": null
}
```

### Step 8 — Create Product Plan and associate Rate Package Group
```
POST /api/product-plans
{
  "name": "Roaming Plan DE",
  "description": "Vodafone DE roaming plan",
  "packageType": "BUYING"
}

POST /api/product-plans/{planId}/associations
{
  "ratePackageGroupId": <groupId>,
  "startTime": "2025-01-01T00:00:00",
  "endTime": "2099-12-31T23:59:59"
}
```

### Step 9 — Create Account for Partner
```
POST /api/accounts
{
  "accountCode": "VFDE01-ACC",
  "partnerType": "VENDOR",
  "partnerId": <partnerId>,
  "productPlanId": <planId>
}
```

### Step 10 — Create Agreement
```
POST /api/agreements
{
  "agreementCode": "AGR-VFDE-2025",
  "lineOfBusiness": "ROAMING",
  "tapDirection": "TAP_IN",
  "homePlmn": "26201",
  "visitorPlmn": "DEUVO",
  "billingCycleStartDate": "2025-01-01",
  "billingCyclePeriod": 30,
  "settlementType": "NET_SETTLEMENT"
}
```

### Step 11 — Configure TAP OUT
```
POST /api/tap-out-configs
{
  "type": "GLOBAL",
  "ourTadig": "INDXX",
  "clearingHouseId": 1,
  "fileTransferConfig": "{\"host\":\"sftp.syniverse.com\",\"port\":22,\"path\":\"/tapout/\"}",
  "isActive": true
}
```

---

## 4. Testing Guide

### 4.1 Unit Test — ZonePrefixTrie Priority
```java
@Test
void testZonePriorityResolution() {
    ZonePrefixTrie trie = new ZonePrefixTrie(1L);
    trie.insert("238",   "ZONE_EU",          null, 2);  // lower priority
    trie.insert("23801", "ZONE_EU_PREMIUM",  null, 1);  // higher priority

    // Exact match — priority 1 wins
    assertEquals("ZONE_EU_PREMIUM", trie.search("23801"));
    // Partial match — only ZONE_EU matches
    assertEquals("ZONE_EU",         trie.search("23805"));
    // No match
    assertNull(trie.search("99999"));
}

@Test
void testZonePriorityTiebreakByDepth() {
    ZonePrefixTrie trie = new ZonePrefixTrie(1L);
    trie.insert("238",   "ZONE_A", null, 1);
    trie.insert("23801", "ZONE_B", null, 1);  // same priority, longer prefix

    // Longer prefix wins on tiebreak
    assertEquals("ZONE_B", trie.search("23801"));
}
```

### 4.2 Integration Test — Full TAP IN Rating Flow
```java
@Test
void testTapInRatingFlow() {
    // 1. Setup: partner with ROAMING LOB, account, product plan,
    //    rate package group, zone, zone rate mapping, rate package with rate details

    // 2. Create a TapCdrDTO
    TapCdrDTO cdr = new TapCdrDTO();
    cdr.setVisitedPlmn("23801");
    cdr.setCallDurationSec(120);
    cdr.setCallStartTime(LocalDateTime.now());
    cdr.setServiceType(ServiceType.VOICE_MO);
    cdr.setTapDirection(TapDirection.TAP_IN);

    // 3. Rate
    roamingRatingService.rateCdrs(tapFileRecord, List.of(cdr));

    // 4. Assert
    RoamingCdr rated = roamingCdrRepository.findAll().get(0);
    assertEquals(RoamingRatingStatus.RATED, rated.getRatingStatus());
    assertEquals("ZONE_EU", rated.getZoneName());
    assertNotNull(rated.getOurCharge());
    assertTrue(rated.getOurCharge().compareTo(BigDecimal.ZERO) > 0);
}
```

### 4.3 Integration Test — TapOutConfig Lookup (PARTNER_SPECIFIC overrides GLOBAL)
```java
@Test
void testPartnerSpecificConfigOverridesGlobal() {
    // Create GLOBAL config
    TapOutConfig global = TapOutConfig.builder()
        .type(TapOutConfig.TapOutConfigType.GLOBAL)
        .ourTadig("INDXX")
        .clearingHouse(clearingHouse)
        .isActive(true).build();
    tapOutConfigRepository.save(global);

    // Create PARTNER_SPECIFIC config for partner A
    TapOutConfig specific = TapOutConfig.builder()
        .type(TapOutConfig.TapOutConfigType.PARTNER_SPECIFIC)
        .ourTadig("INDYY")
        .partner(partnerA)
        .clearingHouse(clearingHouse)
        .isActive(true).build();
    tapOutConfigRepository.save(specific);

    // Lookup for partnerA should return PARTNER_SPECIFIC
    TapOutConfig result = tapOutConfigRepository
        .findByPartnerIdOrGlobal(partnerA.getPartnerId());
    assertEquals("INDYY", result.getOurTadig());
}
```

### 4.4 Manual API Test Sequence
```bash
# 1. Create clearing house
curl -X POST http://localhost:8080/api/clearing-houses \
  -H "Content-Type: application/json" \
  -d '{"name":"Syniverse","type":"DCH","status":"ACTIVE","defaultCurrency":"USD","timezone":"UTC","protocols":["SFTP"]}'

# 2. Create roaming partner
curl -X POST http://localhost:8080/api/partners \
  -H "Content-Type: application/json" \
  -d '{"partnerName":"Vodafone DE","partnerCode":"VFDE01","lineOfBusiness":"ROAMING","interconnectType":"ROAMING","tadigCode":"DEUVO","billingCurrency":"USD","clearingHouseId":1,...}'

# 3. Upload TAP IN file
curl -X POST http://localhost:8080/api/tap-files/upload \
  -F "file=@/path/to/TDINDXXDEUVO00001" \
  -F "partnerId=1"

# 4. Check rated CDRs
curl http://localhost:8080/api/roaming-cdrs?tapFileId=1

# 5. Verify zone was resolved
# Expected: ratingStatus=RATED, zoneName=ZONE_EU, ourCharge > 0
```

### 4.5 Exchange Rate Test
```java
@Test
void testExchangeRateLookup() {
    ExchangeRate rate = ExchangeRate.builder()
        .baseCurrency("USD")
        .targetCurrency("INR")
        .rate(new BigDecimal("83.50"))
        .validFrom(LocalDate.of(2025, 1, 1))
        .source("ECB")
        .build();
    exchangeRateRepository.save(rate);

    ExchangeRate found = exchangeRateRepository
        .findTopByBaseCurrencyAndTargetCurrencyAndValidFromLessThanEqualOrderByValidFromDesc(
            "USD", "INR", LocalDate.now());

    assertNotNull(found);
    assertEquals(0, new BigDecimal("83.50").compareTo(found.getRate()));
}
```

---

## 5. Common Failure Reasons in RoamingCdr

| ratingFailureReason   | Cause                                              | Fix                                              |
|-----------------------|----------------------------------------------------|--------------------------------------------------|
| NO_ACCOUNT            | Partner has no active account                      | Create account linked to partner                 |
| NO_RATE_PACKAGE       | Account → ProductPlan → RatePackageGroup chain broken | Check product plan associations               |
| NO_ZONE_FOUND         | visitedPlmn not in any zone's prefixPattern        | Add PLMN prefix to correct zone                  |
| NO_ZONE_RATE_MAPPING  | Zone found but no active ZoneRateMapping           | Create ZoneRateMapping for group+zone            |
| NO_MATCHING_RATE      | RatePackage found but no RateDetails for call time | Add RateDetails with correct startTime/endTime   |
| RATING_ERROR          | Unexpected exception                               | Check application logs                           |

---

## 6. Cache Invalidation

After updating zones or zone rate mappings via API, invalidate the trie cache:
```java
// Injected in your service
zoneLookupService.invalidateZoneCache(ratePackageGroupId);
```
This forces a DB rebuild for that group on the next rating request.

---

## 7. Key Entities Relationship

```
Partner (LOB=ROAMING)
  └── ClearingHouse
  └── Account
        └── ProductPlan
              └── ProductPlanAssociation
                    └── RatePackageGroup
                          └── ZoneRateMapping
                                ├── Zone (prefixPattern → visitedPlmn lookup)
                                └── RatePackage (type=ZONE, LOB=ROAMING)
                                      └── RateDetails (rate, startTime, endTime)

Agreement (LOB=ROAMING)
  ├── tapDirection (TAP_IN / TAP_OUT)
  ├── homePlmn
  └── visitorPlmn

TapOutConfig
  ├── type (GLOBAL / PARTNER_SPECIFIC)
  ├── ourTadig
  └── ClearingHouse

ExchangeRate
  ├── baseCurrency → targetCurrency
  ├── rate
  └── source (ECB / XE / MANUAL)
```
