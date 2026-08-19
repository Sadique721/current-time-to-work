# Exchange Rate Scheduler — Complete Developer Guide

> **Purpose:** This guide provides detailed technical documentation on the Exchange Rate Scheduler module within the OCS system. It explains how exchange rates are retrieved from an external API, filtered, stored in the database, and how the system gracefully handles failures using fallback mechanisms.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map & Properties](#2-component-map--properties)
3. [The Scheduler Execution Flow](#3-the-scheduler-execution-flow)
4. [API Integration & Filtering](#4-api-integration--filtering)
5. [Currency Precision Handling](#5-currency-precision-handling)
6. [Scenario Playbook — Positive & Negative Cases](#6-scenario-playbook--positive--negative-cases)
7. [Checklist for New Developers](#7-checklist-for-new-developers)

---

## 1. System Overview

Exchange rates are critical for the Invoice Generation process (converting Partner currency into the System Base currency). Because rates fluctuate daily, the system automates fetching these rates via a scheduled cron job (`ExchangeRateScheduler`). 

The scheduler pulls data from an external REST API (e.g., Frankfurter), filters out irrelevant currencies to save space, and persists the daily rates into the `exchange_rate` database table. If the API fails, a self-healing fallback mechanism kicks in to ensure invoicing is never completely blocked.

---

## 2. Component Map & Properties

### Component Map
```text
Spring Scheduler (@Scheduled)
       │
       ▼
ExchangeRateScheduler (@Component) ────► ExchangeRateSchedulerStatusService (Concurrency Lock / Audit)
       │
       ▼
ExchangeRateService (@Service)
       │
       ├── Reads Configs (Base Currencies, API URL)
       ├── API Call (RestTemplate to external provider)
       ├── Filter (Matches against active Country currencies)
       ├── Fallback (If API fails, clone yesterday's rates)
       │
       ▼
Database (exchange_rate table)
```

### Key Application Properties (`application.yml`)

| Property | Default Value | Description |
|---|---|---|
| `exchangeRateScheduler.cron` | `0 0 1 * * ?` | When the scheduler runs (e.g., 1:00 AM daily). |
| `exchangeRate.api.url` | `https://api.frankfurter.app/latest?from={baseCurrency}` | The external API endpoint. `{baseCurrency}` is dynamically injected. |
| `exchangeRate.base-currencies` | `INR,USD` | Comma-separated list of base currencies the system needs rates for. |
| `exchangeRate.api.timeout` | `10000` | RestTemplate connect/read timeout in milliseconds. |
| `exchangeRate.rate-precision` | `6` | Decimal precision for storing the exchange rate multiplier. |

---

## 3. The Scheduler Execution Flow

The `ExchangeRateScheduler` controls the lifecycle of the job.

1. **Concurrency Lock:** Calls `waitForSchedulerLock()`. If a previous job is hung in `RUNNING` status, it will wait up to 60 minutes. If the lock doesn't clear, it skips execution.
2. **Execution Acquisition:** Calls `acquireRunningStatus()` on the status service to record the start in the database.
3. **Execution:** Delegates to `ExchangeRateService.fetchAndSaveRates()`.
4. **Shutdown Safety:** If the application begins to shut down (`@PreDestroy`), an `interrupted` flag is set. The scheduler detects this and marks the job status as `INTERRUPTED`.
5. **Success/Failure:** Logs the total number of records saved and the execution time, marking the DB status as `SUCCESS` or `FAILED`.

---

## 4. API Integration & Filtering

Inside `ExchangeRateService.fetchAndSaveRates()`:

1. **Base Currency Iteration:** The service splits `exchangeRate.base-currencies` and loops through them (e.g., first INR, then USD).
2. **Fetch Active Currencies:** Queries the `Country` table to get a distinct set of `currency_code`s currently active in the system.
3. **API Call:** Uses Spring's `RestTemplate` to call the external API for the current base currency.
4. **Filtering (`filterISO4217`):** The API might return 30+ global currencies. The service filters this down to **only** the currencies actively configured in the `Country` table. This prevents database bloat.
5. **Storage:** Maps the response to `ExchangeRate` entities.
   - `validFrom` = Today's Date.
   - `source` = "FRANKFURTER".
   - Saves to the `exchange_rate` table.

---

## 5. Currency Precision Handling

When storing an exchange rate, the system also calculates and stores the `targetCurrencyPrecision` for the destination currency.

- The service uses Java's built-in `java.util.Currency.getInstance(currencyCode)` to determine the ISO 4217 standard fractional digits (e.g., USD = 2, JPY = 0, BHD = 3).
- If the currency has no minor unit (e.g., SDR/XDR returns `-1`), it safely falls back to `2` decimal places.
- This precision is later used by the Invoicing module to correctly round monetary totals in the target billing currency.

---

## 6. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard Daily Fetch (Positive)
**Context:** System configured for `INR` base currency. 5 distinct currencies active in the Country table (USD, EUR, GBP, AUD, JPY).
**Flow:**
1. Scheduler triggers at 1:00 AM.
2. API called: `https://api.../latest?from=INR`.
3. API returns 32 currencies.
4. `filterISO4217` strips out 27 irrelevant currencies, keeping only the 5 active ones.
5. 5 `ExchangeRate` rows are saved to the DB with `source="FRANKFURTER"`.
6. Status logged as `SUCCESS`.

### ❌ Scenario 2: External API is Down / Timeout (Negative -> Positive Recovery)
**Context:** The Frankfurter API server is unreachable, throwing a `SocketTimeoutException` after 10 seconds.
**Flow:**
1. RestTemplate throws an exception.
2. The `catch` block intercepts the failure and initiates the **Fallback Mechanism**.
3. `applyFallbackRatesForBaseCurrency()` queries the database for the *latest* available date for the base currency (e.g., yesterday's rates).
4. It clones yesterday's rates, sets `validFrom` = Today, and sets `source = "FALLBACK_PREVIOUS_DAY"`.
5. The fallback rates are saved.
**Handling:** The system self-heals. Invoicing for today will use yesterday's rates. No billing processes are blocked.

### ❌ Scenario 3: API is Down AND No Previous Data Exists (Catastrophic Negative)
**Context:** It is day 1 of a new system deployment. The API is down, and the `exchange_rate` table is empty.
**Flow:**
1. API fails.
2. Fallback mechanism triggers.
3. Database query for latest rates returns `null`.
4. `applyFallbackRatesForBaseCurrency()` returns 0 saved records.
5. Service throws `RuntimeException: "Failed to fetch rates... and no fallback rates found"`.
**Handling:** Scheduler marks execution as `FAILED`. Administrators must manually insert rates or fix network access before the billing cycle ends, otherwise invoicing for multi-currency agreements will fail.

### ❌ Scenario 4: Concurrent Execution (Negative)
**Context:** A DevOps engineer manually triggers the scheduler via REST API while the cron job is already running.
**Flow:**
1. Manual trigger calls `fetchExchangeRates()`.
2. `waitForSchedulerLock()` detects status is currently `RUNNING`.
3. Waits. Eventually times out.
4. Logs `"Scheduler skipped due to concurrent execution"`.
**Handling:** Prevents duplicate rows from being inserted into the database for the same day.

---

## 7. Checklist for New Developers

If you are modifying or debugging the Exchange Rate Scheduler:

- [ ] **Test API Accessibility:** Ensure the application server has outbound internet access to the configured `apiUrl`. If running behind a corporate proxy, `RestTemplate` must be configured with a `Proxy`.
- [ ] **Check Base Currencies:** If the Invoicing team complains that converting to USD fails, verify that `USD` is listed in the `exchangeRate.base-currencies` property.
- [ ] **Monitor Fallbacks:** Look for `FALLBACK_PREVIOUS_DAY` in the database. If this appears consistently over several days, the external API integration is permanently broken.
- [ ] **Date Dependency:** Exchange rates are strictly mapped to `validFrom` dates using `LocalDate`. Ensure server timezones are consistent (preferably UTC) to avoid off-by-one-day billing errors.
