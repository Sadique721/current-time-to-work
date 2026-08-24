# 📋 Interconnect Roaming (IR) Project - Technical History & Refactoring Log

This document serves as a consolidated history of the **Interconnect Roaming (IR)** project setup, configuration, and recent refactoring actions. It details all operations performed on the repository at `d:\KTPL\current\IR`.

---

## 🚀 1. Repository Setup & Bitbucket Cloning

The project workspace was cloned from the organization's **Bitbucket** repositories to establish a unified local development environment.

### 📥 Cloning Commands & Branch Information:
* **Backend (`ir-server`)**:
  * **Repository URL**: `https://amin.sadique@bitbucket.org/keyannatechnology/ir-server.git`
  * **Target Branch**: `ocs-summary-engine_tap_merge-sms-error-processing`
  * **Command**:
    ```bash
    git clone --branch ocs-summary-engine_tap_merge-sms-error-processing https://amin.sadique@bitbucket.org/keyannatechnology/ir-server.git ir-server
    ```
* **Frontend (`ir-gui`)**:
  * **Repository URL**: `https://amin.sadique@bitbucket.org/keyannatechnology/ir-gui.git`
  * **Target Branch**: `error-rerating-processing`
  * **Command**:
    ```bash
    git clone --branch error-rerating-processing https://amin.sadique@bitbucket.org/keyannatechnology/ir-gui.git ir-gui
    ```

---

## 🛠️ 2. Type-Safe Enum Refactoring (Eliminating Hardcoded Strings)

### 🔴 The Problem:
Across several core backend files (`BillingCycleScheduler.java`, `InvoiceGenerationService.java`, etc.), settlement type values were hardcoded as plain string literals: `"INCOMING"`, `"OUTGOING"`, `"NET"`, `"ROAMING_TAP_OUT"`. This approach lacked type safety, was prone to typos, required case-sensitive string matching, and could cause crashes if trailing whitespace or nulls were read from the database.

### 🟢 The Solution (Strategy A - Decoupled Enums):
To ensure **zero database schema modifications**, **zero Liquibase changeset impacts**, and **zero Angular frontend REST payload contract breakage**, the system was refactored to use type-safe Java Enums locally while serializing/deserializing them as strings on the edges.

### 📁 Created Enum Files:

#### 1. [`SettlementType.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/constants/enums/SettlementType.java)
Defines type-safe constants with case-insensitive parsing, whitespace trimming, and a default safe fallback (`NET`):
```java
package com.xcess.ocs.constants.enums;

public enum SettlementType {
    NET, INCOMING, OUTGOING, ROAMING_TAP_OUT, ROAMING_TAP_IN;

    public String label() {
        return this.name();
    }

    public static SettlementType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NET;
        }
        try {
            return SettlementType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NET; // Default fallback for safety
        }
    }
}
```

#### 2. [`NetPayableBy.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/constants/enums/NetPayableBy.java)
```java
package com.xcess.ocs.constants.enums;

public enum NetPayableBy {
    CUSTOMER, HOST;

    public String label() {
        return this.name();
    }

    public static NetPayableBy fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CUSTOMER;
        }
        try {
            return NetPayableBy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOMER; // Default fallback for safety
        }
    }
}
```

### 📝 Modified Files & Details (Commit `731a327`):

* **[`InvoiceGenerationService.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/service/InvoiceGenerationService.java)**:
  * Converted raw string switch cases over `settlementType` to switch on the parsed `SettlementType` enum:
    ```java
    SettlementType settlementTypeEnum = SettlementType.fromString(settlementType);
    switch (settlementTypeEnum) {
        case INCOMING: ...
        case OUTGOING: ...
        case NET: ...
    }
    ```
* **[`InvoiceService.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/service/InvoiceService.java)**:
  * Replaced string comparisons like `"INCOMING".equals(settlementType)` with `SettlementType.INCOMING == settlementTypeEnum`.
* **[`BillingCycleScheduler.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/summaryengine/cron/BillingCycleScheduler.java)**:
  * Replaced hardcoded settlement type parameters in repository calls and generated model queries with `SettlementType.INCOMING.label()`, `SettlementType.OUTGOING.label()`, etc.
* **[`InvoiceController.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/controller/InvoiceController.java)**:
  * Handled manual invoice generation endpoint validation by parsing string request parameters via `SettlementType.fromString(settlementType)` to route processing logic correctly.
* **[`AppConstants.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/constants/AppConstants.java)**:
  * Cleaned up legacy constant declarations.

---

## ☕ 3. Java 21 & Gradle Build Configuration

* **The Issue**: Building the project on different systems resulted in build errors because Gradle could not find or dynamically resolve the specified JDK 21 compiler toolchain.
* **The Fix**: Added the `foojay-resolver` convention plugin to [`settings.gradle`](file:///d:/KTPL/current/IR/ir-server/settings.gradle):
  ```groovy
  plugins {
      id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
  }
  rootProject.name = 'Interconnect Roaming'
  ```
* **Result**: Enables the Gradle toolchain daemon to resolve and automatically download OpenJDK 21 onto the system on-demand, resolving local compile issues.

---

## 📭 4. Background SFTP Poller Error Isolation (`UnknownHostException`)

* **The Issue**: When running the server locally, a background poller schedule triggered every few seconds, attempting to contact the mock SFTP server (`sftp-mock`), producing constant `UnknownHostException: sftp-mock` stack trace logs in the terminal.
* **The Fix**:
  1. Annotated the poller component class [`TapSftpPullScheduler.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/roaming/scheduler/TapSftpPullScheduler.java) with `@ConditionalOnProperty`:
     ```java
     @ConditionalOnProperty(name = "roaming.tap.pull-enabled", havingValue = "true", matchIfMissing = true)
     ```
  2. Disabled background polling in local configs [`application-local.properties`](file:///d:/KTPL/current/IR/ir-server/src/main/resources/application-local.properties) and [`application.properties`](file:///d:/KTPL/current/IR/ir-server/src/main/resources/application.properties):
     ```properties
     # Disable background SFTP polling in dev profiles to avoid sftp-mock logs
     roaming.tap.pull-enabled=false
     ```
* **Result**: Keeps background pulling enabled in production but silences network exception logs during local runs.

---

## 🐋 5. Docker Infrastructure & Runtime Upgrades

We refined the local infrastructure setup in the Docker compose mesh for seamless containerized execution.

### 🗄️ MySQL & SFTP Mock Containers:
Updated [`docker-compose.yml`](file:///d:/KTPL/current/IR/docker-compose.yml) to spin up:
1. **MySQL 8.0 Engine** (`ir-mysql`):
   * Runs on host port `3307` mapped to container `3306`.
   * Automatically initializes the database tables by mounting and execution of [`xcessocs.sql`](file:///d:/KTPL/current/IR/xcessocs.sql) at startup.
2. **SFTP Mock Server** (`sftp-mock`):
   * Runs on port `2222` to replicate the remote carrier environment.

### 📦 Dockerfile Footprint Optimization:
Modified [`ir-server/Dockerfile`](file:///d:/KTPL/current/IR/ir-server/Dockerfile):
* Changed runtime base image from `eclipse-temurin:21-jdk-jammy` to **`eclipse-temurin:21-jre-alpine`**.
* **Result**: Dramatically reduced the final docker image size and minimized security vulnerabilities by running on Alpine JRE instead of Ubuntu JDK.

---

## 🔑 6. Local Admin Credentials Seeding
* Seeded a default administrator user through the gateway database for local dashboard interface verification:
  * **User**: `Amin`
  * **Password**: `Amin@123`
  * **Role**: `Admin`
* **Result**: Verified successful login against the Angular dashboard application running at `http://localhost:4200/dashboard`.
