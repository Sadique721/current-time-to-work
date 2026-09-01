# 📖 Antigravity Workspace Rules & Memory Guide

This document preserves the workspace memory, path rules, and developer constraints for the **Interconnect Roaming (IR)** repository. Antigravity loads this context automatically on startup to align operations with workspace rules.

---

## 📍 1. Workspace Locations & Environments

* **Root Directory:** `d:\KTPL\current\IR` (Git remote points to GitHub repository `current-time-to-work`, branch `main`)
  * **GitHub Push Policy:**
    * Strictly uses **only `main` branch**.
    * Can commit and push the **overall project** (all code files, frontend `ir-gui`, backend `ir-server`, documentation, build configurations, and scripts with full details).
    * Never create or push secondary branches (e.g. `amin`) on GitHub.
* **Backend Repository:** `d:\KTPL\current\IR\ir-server` (Git remote points to Bitbucket repository `ir-server`, branch `amin`)
  * **Bitbucket Push Policy:**
    * Strictly uses **only `amin` branch**.
    * **Pre-Push Reminder Requirement:** ALWAYS remind/notify the user before committing or pushing anything to Bitbucket.
    * **Selective Scoped Commits:** Only stage and commit the specific backend files/features that were worked on (e.g. refactored services, DTOs, controllers, changelogs, `documentation/Hardcode.md`).
    * **Strictly Exclude Local Files on Bitbucket:** Never stage or push local configurations (`application.properties`, `application-local.properties`, `build.gradle`, `settings.gradle`, `gradle.properties`, wrapper configs).
  * **Default Port:** `8080`
  * **Local Context Path:** `/rating-engine/v1`
  * **Local Database:** MySQL Port `3306` (username: `root`, password: `0721`, database: `xcessocs`)

---

## 🔒 2. Committing & Pushing Constraints (Bitbucket vs GitHub)

1. **GitHub (`current-time-to-work` -> `main`):**
   * Full project scope permitted.
   * Can include toolchain configurations, local testing scripts, and full workspace features.
2. **Bitbucket (`ir-server` -> `amin`):**
   * Strictly limited to production-safe codebase files.
   * **Do NOT commit or push local configs to Bitbucket:**
     * `src/main/resources/application.properties`
     * `src/main/resources/application-local.properties`
     * `build.gradle`
     * `settings.gradle`
     * `gradle.properties`
     * `gradle/wrapper/gradle-wrapper.properties`

---

## 🛡️ 3. Refactoring Guardrails

* **JPA Entity Classes:** Database entity files (e.g. `Invoice.java`, `ReRateRequest.java`, `ErrorRateRequest.java`) must remain **100% untouched** (no annotations or field updates) to prevent Liquibase schema conflicts.
* **Refactoring Strategy (Strategy A):** Use service-layer type safety (e.g., custom Enums like `SettlementType`, `NetPayableBy`, `RequestStatus`, and `ServiceType`) while preserving the REST payload string structures for Angular GUI compatibility.
* **Auditing:** Maintain [**`Hardcode.md`**](file:///d:/KTPL/current/IR/ir-server/documentation/Hardcode.md) in the backend repository's documentation directory to record all refactored codes and test cURL inputs/outputs.
