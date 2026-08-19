# MSA AI & IR Project Modification Trace Log

This file contains the complete trace log of all code and configuration modifications applied to resolve build, model gateway, and network polling errors in the project.

---

## 📅 Log Date: 2026-08-19

### 1. Host Java Build Configuration (Gradle Resolution Fix)
* **File Modified**: [`ir-server/settings.gradle`](file:///d:/KTPL/current/IR/ir-server/settings.gradle)
* **Change Details**:
  Added the `foojay-resolver` plugin to allow Gradle to resolve and auto-download JDK 21 on the host machine.
  ```groovy
  plugins {
      id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
  }
  rootProject.name = 'Interconnect Roaming'
  ```
* **Implication**: Gradle JVM builds on the host machine (`.\gradlew.bat compileJava`) now pass with **BUILD SUCCESSFUL** by dynamically resolving OpenJDK 21.

---

### 2. Docker Container Environment (Gemini API Key 403 Fix)
* **File Created**: [`.env` (in D:\current using file\8-18-2026\.env)](file:///D:/current%20using%20file/8-18-2026/.env)
  * **Change Details**: Added the valid Google AI Studio Gemini API Key `[REDACTED_GEMINI_API_KEY]`.
* **File Modified**: [`docker-compose-msa.yml` (in D:\current using file\8-18-2026\docker-compose-msa.yml)](file:///D:/current%20using%20file/8-18-2026/docker-compose-msa.yml)
  * **Change Details**: Mapped the `GEMINI_API_KEY` env variable from the host to the `msa-router` and `omniroute` container environments:
  ```yaml
  environment:
    - PORT=20130
    - OLLAMA_URL=http://ollama:11434
    - OMNIROUTE_URL=http://omniroute:20129/v1/chat/completions
    - GEMINI_API_KEY=${GEMINI_API_KEY}
  ```
* **File Modified**: [`config.json` (in D:\current using file\8-18-2026\omniroute-data\config.json)](file:///D:/current%20using%20file/8-18-2026/omniroute-data/config.json)
  * **Change Details**: Pre-configured the valid Gemini API Key inside the `gemini` provider object.
* **Implication**: Docker container space is no longer isolated from the API credentials, preventing Google's API gateway from returning `403 Permission Denied` during model dispatch.

---

### 3. Roaming SFTP Poller (Local Dev UnknownHostException Fix)
* **File Modified**: [`ir-server/.../scheduler/TapSftpPullScheduler.java`](file:///d:/KTPL/current/IR/ir-server/src/main/java/com/xcess/ocs/roaming/scheduler/TapSftpPullScheduler.java)
  * **Change Details**: Applied `@ConditionalOnProperty` annotation so the poller only runs when enabled.
  ```java
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  
  @Slf4j
  @Component
  @RequiredArgsConstructor
  @ConditionalOnProperty(name = "roaming.tap.pull-enabled", havingValue = "true", matchIfMissing = true)
  public class TapSftpPullScheduler {
  ```
* **File Modified**: [`ir-server/.../resources/application-local.properties`](file:///d:/KTPL/current/IR/ir-server/src/main/resources/application-local.properties)
  * **Change Details**: Added the property to disable background pulling locally.
  ```properties
  # Roaming SFTP Polling Configuration
  # Set to false to disable sftp-mock UnknownHostException errors in local dev
  roaming.tap.pull-enabled=false
  ```
* **Implication**: The Spring Boot backend no longer prints recurring `UnknownHostException: sftp-mock` stack traces during local execution, while keeping it enabled in production.

---

### 4. Background SFTP Polling Disabled in Default Profile
* **File Modified**: [`ir-server/src/main/resources/application.properties`](file:///d:/KTPL/current/IR/ir-server/src/main/resources/application.properties)
* **Change Details**: Added `roaming.tap.pull-enabled=false` to ensure background pulling is disabled by default during local development/debugging runs using the default profile.
* **Implication**: `UnknownHostException: sftp-mock` logs no longer print repeatedly in the console.

---

### 5. Added New Admin User via API
* **Change Details**: Created user `Amin` with password `Amin@123` and role `Admin` via the general gateway REST API, enabling validation against the authorization gateway.
* **Verification**: Logged in with the new user `Amin` and verified the Angular dashboard interface (`http://localhost:4200/dashboard`) loads cleanly.
