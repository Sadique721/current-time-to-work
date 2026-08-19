# Invoicing Process — Complete Developer Guide

> **Purpose:** This guide provides a detailed technical overview of the complete Invoicing flow in the OCS system. It covers everything from setting up an agreement to executing the billing scheduler, generating the XML, calculating taxes, and storing the final PDF. It covers both **INTERCONNECT** and **ROAMING** line of businesses.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map](#2-component-map)
3. [The Agreement Setup](#3-the-agreement-setup)
4. [The Billing Scheduler (Cron Job)](#4-the-billing-scheduler-cron-job)
5. [Invoice Generation Logic (Interconnect)](#5-invoice-generation-logic-interconnect)
6. [Roaming Invoice Flow (TAP OUT)](#6-roaming-invoice-flow-tap-out)
7. [Tax Calculation & Currency Conversion](#7-tax-calculation--currency-conversion)
8. [PDF Generation & Storage](#8-pdf-generation--storage)
9. [Scenario Playbook — Positive & Negative Cases](#9-scenario-playbook--positive--negative-cases)
10. [Checklist for New Developers](#10-checklist-for-new-developers)

---

## 1. System Overview

In the OCS system, invoicing is the final financial step. It aggregates already calculated daily summaries (`RatedSummary`) for a specific billing period into a formalized bill (`Invoice`). 

The flow is entirely automated via a daily cron job (`BillingCycleScheduler`), which acts upon `Agreement` configurations. The result is a securely generated PDF and an XML payload stored in the database.

### Core Design Goals
- **Agreement-Driven:** The schedule, settlement type (INCOMING, OUTGOING, NET), and taxes are strictly tied to an `Agreement`.
- **Idempotency:** A single invoice for a specific period, agreement, and settlement type can only be generated once.
- **Fail-Safe Generation:** If an invoice generation fails (e.g., PDF generation error, DB error), the failure is logged in `failed_invoice` and the agreement's billing cycle is **not** advanced, ensuring it will retry the next day.
- **Multi-Currency & Tax Support:** Dynamically applies conversion rates and multi-layered tax calculations before finalizing totals.

---

## 2. Component Map

```text
Spring Scheduler (@Scheduled)
       │
       ▼
BillingCycleScheduler
       │
       ├── Reads Agreements
       ├── Validates Billing Cycle Date
       │
       ├── INTERCONNECT LOB ──► InvoiceGenerationService
       │                           ├── Fetches RatedSummary data
       │                           ├── Applies ExchangeRates
       │                           ├── TaxCalculationService (Multi-Tax)
       │                           ├── Generates XML (InvoiceXmlDTO)
       │                           └── PdfGenerationService (Generates PDF -> File System)
       │
       └── ROAMING LOB ───────► RoamingTapOutInvoiceService
                                   └── Generates TAP OUT specific invoices
```

---

## 3. The Agreement Setup

Before an invoice can be generated, an `Agreement` must exist. 

### Key Fields for Invoicing
| Field | Description |
|---|---|
| `lineOfBusiness` | `INTERCONNECT` or `ROAMING`. Dictates which generation service is used. |
| `billingCycleStartDate` | The anchor date for the billing cycle. |
| `nextBillingCycleStartDate` | The date the *current* active cycle starts. |
| `billingCyclePeriod` | The length of the cycle in days (e.g., 30). |
| `isIncomingSettlement` | Generates an INCOMING invoice (Customer pays Host). |
| `isOutgoingSettlement` | Generates an OUTGOING invoice (Host pays Vendor). |
| `isNetSettlement` | Generates a NET invoice (Calculates the difference). |
| `tapDirection` | For ROAMING only (`TAP_OUT`). |

*Note:* Accounts (`AccountAgreement`) must be mapped to the Agreement for the system to know which CDRs/Summaries belong to this invoice.

---

## 4. The Billing Scheduler (Cron Job)

The `BillingCycleScheduler` runs daily at midnight (`@Scheduled(cron = "${billingCycleCronScheduler}")`).

### Execution Flow
1. **Concurrency Lock:** It checks `BillingSchedulerStatusService`. If the previous day's run is still stuck in `RUNNING`, it waits up to 60 minutes. If it still doesn't finish, the current run is skipped to prevent duplicate concurrent generation.
2. **Fetch Agreements:** Iterates through all active agreements.
3. **Date Validation:** 
   - `cycleStart` = `nextBillingCycleStartDate` (or falls back to `billingCycleStartDate`).
   - `cycleEnd` = `cycleStart + billingCyclePeriod - 1`.
   - **Condition:** If `cycleEnd` is **before** `today`, the cycle is complete, and generation proceeds.
4. **Trigger Generation:** Depending on the `LineOfBusiness` and enabled settlement types, it triggers the respective service.
5. **Cycle Advancement:** **ONLY IF** generation succeeds for all configured types, `nextBillingCycleStartDate` is advanced. If any fail, it throws an exception, saves a record via `FailedInvoiceService`, and leaves the date alone for a retry tomorrow.

---

## 5. Invoice Generation Logic (Interconnect)

`InvoiceGenerationService.generateInvoiceForSettlementType()` handles standard interconnect invoices.

### Step-by-Step Flow:
1. **Idempotency Check:** Queries `invoiceRepository` to ensure an invoice for the `AgreementId`, `BillingStart`, `BillingEnd`, and `SettlementType` doesn't already exist.
2. **Fetch Data:** Retrieves all `RatedSummary` records for all accounts mapped to the agreement within the billing date range.
3. **Aggregation:**
   - Filters summaries by `INCOMING` and `OUTGOING` directions.
   - Converts the `totalCharge` from the base currency to the partner's `billingCurrency` (if applicable) using `ExchangeRateRepository` as of the `billingEnd` date.
4. **Settlement Logic:**
   - **INCOMING:** Total = sum(INCOMING). Payable by CUSTOMER.
   - **OUTGOING:** Total = sum(OUTGOING). Payable by HOST.
   - **NET:** Total = |sum(INCOMING) - sum(OUTGOING)|. Payable by whoever owes more.
5. **Tax Calculation:** Calls `TaxCalculationService` to apply tiered/multi-taxes. (See Section 7)
6. **XML Generation:** Maps all data into `InvoiceXmlDTO` and converts it to XML string format.
7. **Database Save 1:** Saves the `Invoice` entity with status `GENERATED` and the raw XML.
8. **PDF Generation:** Calls `PdfGenerationService.generatePdf(xmlContent)`.
9. **Database Save 2:** Updates the `Invoice` entity with the `pdfFilePath` and `pdfChecksum` and saves it again.

---

## 6. Roaming Invoice Flow (TAP OUT)

If the `Agreement` has `LineOfBusiness.ROAMING` and `TapDirection.TAP_OUT`, the scheduler delegates to `RoamingTapOutInvoiceService`.

- **Difference from Interconnect:** Roaming involves TAP (Transferred Account Procedure) files. The invoice generation logic specifically aggregates TAP OUT summaries (calls made by home subscribers in the visited network). 
- **Settlement Type:** Hardcoded to `ROAMING_TAP_OUT` to separate it from standard INCOMING/OUTGOING/NET.
- Uses a roaming-specific template for PDF generation.

---

## 7. Tax Calculation & Currency Conversion

### Currency Conversion
- `RatedSummary` totals are stored in the system's base currency.
- `convertToBillingCurrency()` checks the `ExchangeRateRepository` for a rate where `baseCurrency` matches the partner's required currency on the `billingEnd` date.
- If no rate is found, it falls back to the max available date. If still not found, it warns and uses the raw amount (1:1).

### Tax Calculation
- If the agreement is `isTaxExempt == true`, taxes are zero.
- Otherwise, it fetches ordered `AgreementTaxConfig` rows.
- `TaxCalculationService.calculateMultiTax()` processes these rules sequentially. Some taxes may apply to the base amount, while others may "accumulate" (e.g., Tax B is applied to [Base Amount + Tax A]).
- Generates `InvoiceTaxDetail` rows linked to the final `Invoice`.

---

## 8. PDF Generation & Storage

The system relies heavily on the `InvoiceXmlDTO` → XML format.

1. **XML as Truth:** The XML string saved in the `xml_content` column is the absolute source of truth for the PDF layout.
2. **Template Engine:** `PdfGenerationService` takes the raw XML and a `templatePath` (e.g., XSLT/XSL-FO templates configured on the Agreement).
3. **Generation:** Converts the XML + Template into a binary PDF.
4. **Storage:** The PDF is saved to the local file system (or network drive).
5. **Security Verification:** An MD5/SHA checksum is generated (`pdfChecksum`) and stored alongside the `pdfFilePath` in the database to detect tampering.

---

## 9. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard NET Settlement Success (Positive)
**Context:** A 30-day Interconnect Agreement ends on Jan 31.
**Flow:**
1. Scheduler runs at midnight on Feb 1.
2. Detects cycle is complete.
3. Retrieves summaries. Incoming = $500, Outgoing = $300.
4. Net = $200 (Payable by CUSTOMER).
5. Taxes applied ($20). Total = $220.
6. XML built -> PDF generated -> Saved to `/var/ocs/invoices/NET-202601-001.pdf`.
7. Agreement's `nextBillingCycleStartDate` is advanced to Mar 2.
**Result:** Clean generation, cycle advanced.

### ❌ Scenario 2: Previous Scheduler Run Stuck (Negative)
**Context:** Yesterday's scheduler got stuck in an infinite loop or heavy DB lock, leaving status `RUNNING`.
**Flow:**
1. Tonight's scheduler wakes up.
2. `waitForSchedulerLock()` sees `RUNNING`. It waits for 60 minutes.
3. Still `RUNNING`. Scheduler logs: `"Timeout waiting for previous scheduler... Skipping this execution."`
**Handling:** Prevents two threads from generating duplicate invoices simultaneously. The stuck thread must be investigated by DevOps.

### ❌ Scenario 3: PDF Generation Fails (Negative)
**Context:** The server runs out of disk space while writing the PDF.
**Flow:**
1. `PdfGenerationService` catches `IOException` and returns `success = false` with `errorReason`.
2. `InvoiceGenerationService` sets `pdfErrorReason` on the `Invoice` but still saves the XML and database row.
**Handling:** The invoice data is successfully stored. A secondary manual or automated process can use the saved XML to re-attempt PDF generation later without recalculating the financials.

### ❌ Scenario 4: Invoice Returned as Null (Negative)
**Context:** `InvoiceGenerationService` returns `null` because an invoice for this exact period already exists (idempotency check).
**Flow:**
1. Scheduler receives `null`.
2. Marks `success = false`.
3. Calls `failedInvoiceService.saveFailedInvoice()`.
4. **Crucial:** Agreement `nextBillingCycleStartDate` is **NOT** advanced.
**Handling:** The system protects itself from duplicates but flags it for manual review.

### ❌ Scenario 5: Missing Exchange Rate (Negative)
**Context:** Partner requires billing in EUR, but no exchange rate exists for the `billingEnd` date.
**Flow:**
1. `convertToBillingCurrency()` tries to find a rate for EUR.
2. Falls back to the latest available date. If none exists, catches exception/returns empty.
3. Logs a warning: `"Exchange rate lookup failed for currency EUR"`.
4. Returns the un-converted base amount.
**Handling:** The invoice generates to avoid halting the pipeline, but the currency value will be strictly 1:1. *Developers should monitor exchange rate sync jobs.*

---

## 10. Checklist for New Developers

If you are modifying or debugging the Invoicing module, verify the following:

- [ ] **Check the Cycle Dates:** If an agreement isn't generating, check `nextBillingCycleStartDate`. `cycleStart + billingCyclePeriod - 1` must be strictly *before* `LocalDate.now()`.
- [ ] **Transaction Boundaries:** `processAgreement()` uses `@Transactional(REQUIRES_NEW)`. If one agreement fails, it rolls back its own data, but other agreements in the loop will continue to process successfully.
- [ ] **XML Over Code Changes:** If the invoice layout/numbers look wrong on the PDF, check the `xml_content` in the database first. If the XML is right but the PDF is wrong, the issue is in the XSLT template, not the Java code.
- [ ] **Check Failed Invoices Table:** Always query the `failed_invoice` table if business reports missing invoices. The exact error reason is logged there.
- [ ] **Tax Config Order:** Tax configurations rely heavily on `applyOrder`. Ensure taxes that depend on sub-totals are sequenced correctly in the database.
