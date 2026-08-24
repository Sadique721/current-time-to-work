# 📖 Antigravity Workspace Rules & Memory Guide

This document preserves the workspace memory, path rules, and developer constraints for the **Interconnect Roaming (IR)** repository. Antigravity loads this context automatically on startup to align operations with workspace rules.

---

## 📍 1. Workspace Locations & Environments

* **Root Directory:** `d:\KTPL\current\IR` (Git remote points to GitHub repository `current-time-to-work`)
* **Backend Repository:** `d:\KTPL\current\IR\ir-server` (Git remote points to Bitbucket repository `ir-server`, branch `amin`)
  * **Default Port:** `8080`
  * **Local Context Path:** `/rating-engine/v1`
  * **Local Database:** MySQL Port `3306` (username: `root`, password: `0721`, database: `xcessocs`)

---

## 🔒 2. Committing & Pushing Constraints (Strictly Local Files)

The following files are configured for local environment testing/development. **Do not commit, stage, or push them to remote repositories:**

1. **📁 Properties Configurations:**
   * `src/main/resources/application.properties`
   * `src/main/resources/application-local.properties`
2. **📁 Gradle Toolchain & Compiler Setups:**
   * `build.gradle`
   * `settings.gradle`
   * `gradle.properties`
   * `gradle/wrapper/gradle-wrapper.properties`

---

## 🛡️ 3. Refactoring Guardrails

* **JPA Entity Classes:** Database entity files (e.g. `Invoice.java`, `ReRateRequest.java`, `ErrorRateRequest.java`) must remain **100% untouched** (no annotations or field updates) to prevent Liquibase schema conflicts.
* **Refactoring Strategy (Strategy A):** Use service-layer type safety (e.g., custom Enums like `SettlementType`, `NetPayableBy`, `RequestStatus`, and `ServiceType`) while preserving the REST payload string structures for Angular GUI compatibility.
* **Auditing:** Maintain [**`Hardcode.md`**](file:///d:/KTPL/current/IR/ir-server/documentation/Hardcode.md) in the backend repository's documentation directory to record all refactored codes and test cURL inputs/outputs.
