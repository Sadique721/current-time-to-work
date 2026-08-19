# Tax Configuration & Calculation — Complete Developer Guide

> **Purpose:** This guide provides a detailed technical overview of how taxes are created, mapped to agreements, and calculated during the invoice generation process within the OCS system. It covers both single-tier and multi-tier (tax-on-tax) scenarios.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Component Map](#2-component-map)
3. [Tax Creation (`TaxConfig`)](#3-tax-creation-taxconfig)
4. [Agreement Tax Mapping (`AgreementTaxConfig`)](#4-agreement-tax-mapping-agreementtaxconfig)
5. [Tax Calculation Logic](#5-tax-calculation-logic)
6. [Scenario Playbook — Positive & Negative Cases](#6-scenario-playbook--positive--negative-cases)
7. [Checklist for New Developers](#7-checklist-for-new-developers)

---

## 1. System Overview

The Tax module in the OCS system is designed to be highly flexible. It supports:
- Flat percentage taxes.
- **Multi-layered taxes** (where one tax is applied on top of the base amount *plus* previously calculated taxes).
- Date-effective tax rules to ensure historic invoices are calculated correctly even if tax laws change.
- Complete exemption handling.

The core service responsible for crunching the numbers is the `TaxCalculationService`, which is invoked by the `InvoiceGenerationService` just before an invoice is finalized.

---

## 2. Component Map

```text
Database Tables
    │
    ├── tax_config (The master list of taxes)
    │
    └── agreement_tax_config (Mapping table linking a Tax to an Agreement with specific order)
           │
           ▼
InvoiceGenerationService
           │
           ├── Checks if Agreement.isTaxExempt == true
           ├── Fetches List<AgreementTaxConfig> sorted by applyOrder
           │
           ▼
TaxCalculationService (@Service)
           ├── calculateMultiTax(baseAmount, agreementTaxConfigs, invoiceDate)
           │      ├── Checks effective dates
           │      ├── Handles accumulateFromOrders (Tax-on-Tax)
           │      └── Applies precision rounding
           │
           └── Returns MultiTaxCalculationResult
                  └── List<InvoiceTaxLineItem> (Saved to invoice_tax_details table)
```

---

## 3. Tax Creation (`TaxConfig`)

Taxes are defined globally in the system via the `TaxConfig` entity.

### Key Fields:
| Field | Description |
|---|---|
| `taxName` | The human-readable name of the tax (e.g., "VAT", "GST"). |
| `taxType` | A categorization string. |
| `standardRate` | The percentage rate of the tax (e.g., `18.5` for 18.5%). |
| `effectiveFrom` | The date this tax rule becomes legally valid. |
| `effectiveTo` | The date this tax rule expires (optional). |
| `applyOn` | Usually denotes what it applies to (e.g., "BASE", "ACCUMULATED"). |

*Note:* If a government changes a tax rate from 10% to 12% on Jan 1st, you do **not** update the existing `TaxConfig`. You set `effectiveTo` on the 10% config to Dec 31st, and create a new 12% config with `effectiveFrom` Jan 1st.

---

## 4. Agreement Tax Mapping (`AgreementTaxConfig`)

A tax only applies to an invoice if it is mapped to the partner's `Agreement` via the `AgreementTaxConfig` entity. 

This mapping table introduces the concept of **Order** and **Accumulation**, which allows for complex tax-on-tax calculations.

### Key Fields:
| Field | Description |
|---|---|
| `applyOrder` | (Integer) The sequence in which the tax is calculated. 1 is first, 2 is second, etc. |
| `accumulateFromOrders` | (String, Comma-separated) Dictates if the tax should be applied on the base amount + the calculated tax amount from previous orders. |

**Example of `accumulateFromOrders`:**
- If `applyOrder = 1` (Base Tax 10%). `accumulateFromOrders` is empty. Applied strictly to the base invoice amount.
- If `applyOrder = 2` (Surcharge 5%). `accumulateFromOrders = "1"`. Applied to (Base Amount + The calculated tax amount from Order 1).

---

## 5. Tax Calculation Logic

When `InvoiceGenerationService` generates a NET or INCOMING invoice, it calls `TaxCalculationService.calculateMultiTax()`.

### The Algorithm:
1. **Exempt Check:** If the base amount is 0, or if `agreement.isTaxExempt` is true, the service immediately returns `calculateExempt()`, which yields 0 for all taxes.
2. **Sort:** It sorts the `AgreementTaxConfig` list ascending by `applyOrder`.
3. **Loop:** Iterates through each configured tax:
   - **Date Check:** Validates that `invoiceDate` falls between `effectiveFrom` and `effectiveTo`. If not, an exception is thrown.
   - **Base Determination:** 
     - If `accumulateFromOrders` is blank: `taxableBase = baseAmount`.
     - If not blank: It parses the string (e.g., "1,2"), looks up the previously calculated tax amounts for those orders in a local Map, and calculates `taxableBase = baseAmount + order1_tax + order2_tax`.
   - **Calculation:** `taxAmount = taxableBase * (standardRate / 100)`.
   - **Rounding:** Rounds the `taxAmount` using the `RoundingProperties` (typically Half-Up, 2 decimal places).
4. **Finalization:** Sums up all `taxAmount`s into a single `totalTax` and creates `InvoiceTaxLineItem`s.

---

## 6. Scenario Playbook — Positive & Negative Cases

### ✅ Scenario 1: Standard Single Tax (Positive)
**Context:** An invoice with a base amount of $1,000. Agreement has one tax mapped: "VAT 10%", Order=1, Accumulate=null.
**Flow:**
1. Sorted list: [Order 1].
2. Taxable Base = $1,000.
3. Tax Amount = 1,000 * 0.10 = $100.
4. Total Invoice Amount = $1,100.
**Result:** Clean single-tier calculation.

### ✅ Scenario 2: Multi-Tier Tax-on-Tax (Positive)
**Context:** An invoice base of $1,000. Agreement has two taxes:
- Tax A: 10%, Order=1, Accumulate=null.
- Tax B (Surcharge): 5%, Order=2, Accumulate="1".
**Flow:**
1. **Order 1 (Tax A):** Base = $1,000. Tax = $100. Saved to local map under key `1`.
2. **Order 2 (Tax B):** Base = $1,000 + Map.get(1) = $1,100.
3. Tax B Amount = $1,100 * 0.05 = $55.
4. Total Tax = $100 + $55 = $155. Total Invoice = $1,155.
**Result:** Complex multi-layer taxation calculated correctly.

### ❌ Scenario 3: Tax Config Not Effective (Negative)
**Context:** An invoice is generated for December 31st, 2025. The mapped tax "GST 18%" has an `effectiveFrom` of January 1st, 2026.
**Flow:**
1. `TaxCalculationService` evaluates `invoiceDate` (Dec 31) against `effectiveFrom` (Jan 1).
2. Throws `RuntimeException`: `"Tax config 'GST 18%' not effective on 2025-12-31"`.
3. The exception propagates up. `InvoiceGenerationService` catches it, logs a warning, and proceeds without applying tax.
**Handling:** The invoice generation does not crash, but it generates without the invalid tax. *Developers must ensure proper effective dates are set when migrating to new tax rates.*

### ❌ Scenario 4: Missing Referenced Order for Accumulation (Negative)
**Context:** An admin misconfigures the database. Tax B (Order=2) has `accumulateFromOrders="3"`, but Order 3 does not exist or hasn't been calculated yet.
**Flow:**
1. `TaxCalculationService` processes Order 2.
2. It attempts to parse `accumulateFromOrders="3"` and looks up key `3` in the local map.
3. Map returns `null`.
4. Throws `RuntimeException`: `"Order 3 referenced but not found"`.
**Handling:** Tax calculation fails. Invoice generates without tax, flagged with a warning in logs.

### ✅ Scenario 5: Tax Exempt Agreement (Positive Edge Case)
**Context:** A partner is a government entity and is exempt from all taxes. `Agreement.isTaxExempt = true`.
**Flow:**
1. `InvoiceGenerationService` checks the flag.
2. Calls `taxCalculationService.calculateExempt(baseAmount)`.
3. Immediately returns a result with `$0` tax and empty line items.
**Result:** Skips all database lookups and processing rules entirely, ensuring zero tax.

---

## 7. Checklist for New Developers

If you are modifying or debugging the Tax module, verify the following:

- [ ] **Never Modify Historic Standard Rates:** If a tax rate changes, create a new `TaxConfig` and end-date the old one using `effectiveTo`. Modifying the `standardRate` directly will corrupt any recalculations of historic invoices.
- [ ] **Check Apply Order:** When debugging multi-tier taxes, ensure the `applyOrder` logic makes sense. A tax with Order=1 cannot `accumulateFromOrders="2"`.
- [ ] **Verify Date:** Remember that `invoiceDate` is used to check effectiveness, not the current system execution date. This ensures retroactive billing calculations are accurate.
- [ ] **Rounding Configuration:** Taxes are highly sensitive to rounding. Always use `roundingProperties.getRoundingMode()` (usually HALF_UP) and `getCalculationPrecision()`, never raw `double` math. Use `BigDecimal` strictly.
