# TAP OUT Generation Flow Overview

This document details the end-to-end lifecycle of generating outgoing Transferred Account Procedure (TAP OUT) files for roaming partners within the OCS Summary Engine. It covers everything from the scheduled trigger to CDR extraction, binary ASN.1 encoding, and final SFTP delivery.

---

## 1. The Trigger: `TapOutScheduler`

The entire process is automated via a Spring Boot scheduler designed to run during off-peak hours.

*   **Schedule**: Runs daily at `1:00 AM` (configurable via `roaming.tap.out-cron` in `application.properties`).
*   **Time Window**: Processes all CDRs that were rated during the **previous day** (Yesterday `00:00:00` to `23:59:59`).
*   **Partner Selection**: The scheduler queries the database for eligible partners:
    *   Must have `lineOfBusiness = ROAMING`.
    *   Must have a `TADIG` code configured.
    *   Must have an assigned `TapProfileGroup` (critical for field mapping).
*   **Execution**: For each eligible partner, the scheduler invokes `TapOutFileGenerationService.generateForPartner()`.

---

## 2. CDR Extraction

Once triggered for a specific partner, the system extracts the relevant billing records.

*   **Extraction Criteria**: Queries are executed against `VoiceRatedCdrRepository`, `SmsRatedCdrRepository`, and `UsageRatedCdrRepository`.
*   **Filter**: Selects records where `homePlmn` matches the partner's HPLMN, the timestamp falls within the yesterday window, and the CDR has *not* already been marked as TAP OUT generated.
*   **Aggregation**: All extracted Voice, SMS, and Usage records are aggregated into a unified `List<RateableCdr>` for processing.

---

## 3. Event Mapping and ASN.1 Construction

This is the core mapping phase where generic `RateableCdr` records are transformed into strict GSMA TAP3 event objects.

1.  **Profile Resolution**: For each CDR, the system checks its `serviceType` (e.g., VOICE, SMS) and finds the corresponding `TapProfile` assigned to the partner.
2.  **Event Type Selection**: Based on the `CallType`, a specific TAP object is instantiated:
    *   `MO_VOICE` $\rightarrow$ `MobileOriginatedCall`
    *   `MT_VOICE` $\rightarrow$ `MobileTerminatedCall`
    *   `MO_SMS` / `MT_SMS` $\rightarrow$ `MobileSession`
    *   `GPRS` $\rightarrow$ `GprsCall`
3.  **Field Population**: The `TapFieldPathResolver` iterates through the `TapProfileFieldMapping`s for the profile:
    *   It extracts the value from the `RateableCdr` using the `outSourceColumn`.
    *   If null, it applies the partner-specific or global `defaultValue`.
    *   It strictly enforces `isMandatory` checks (failing the CDR if a required field is missing).
    *   Values are formatted (e.g., Decimal alignment, BCD encoding) and injected into the ASN.1 object via reflection based on the `asnPath`.

---

## 4. File Assembly (`DataInterChange`)

Once all individual call events are mapped, they are wrapped into a formal TAP structure.

*   **Sequence Management**: `TapOutSequenceRepository` fetches and increments the strict, sequential 5-digit file number for the specific partner.
*   **Batch Control Info**: Defines the Sender TADIG (Our network), Recipient TADIG (The partner), File Sequence Number, and TAP version specs (e.g., Release 12).
*   **Network & Accounting Info**: Sets the `TapDecimalPlaces` (usually 4), `LocalCurrency`, and UTC offset data.
*   **Audit Control Info**: Calculates the total number of records and the exact `TotalCharge` (sum of `outgoingTotalCost` from all CDRs).
*   The headers, trailers, and event list are packaged into a root `DataInterChange` object.

---

## 5. Binary Encoding & File Save

The logical `DataInterChange` object must be converted into a binary file compliant with Basic Encoding Rules (BER).

1.  **BER Encoding**: The `DataInterChange.encode()` method writes the object tree into a `ReverseByteArrayOutputStream` (from the `asn1bean` library).
2.  **File Naming**: The file name is generated using the strict GSMA standard: `TD{SenderTADIG}{RecipientTADIG}{SequenceNumber}` (e.g., `TDEUR01FRA0200015`).
3.  **Local Save**: The binary byte array is written to disk in the configured `outboxDir` (e.g., `/data/tap-files/outbox/`).

---

## 6. Persistence and Delivery

After the file is physically created, the system performs final administrative tasks.

1.  **Database Record**: A `TapFileRecord` is saved in the database with status `INVOICED`, logging the file path, total charge, and sequence number for audit purposes.
2.  **SFTP Push**: `TapSftpPushService.push()` is invoked to securely transfer the generated `.tap` file to the Roaming Partner's configured SFTP server.
3.  **CDR Flagging**: The original records in the `rated_cdr` tables are updated, linking their `tap_file_record_id` foreign key. This ensures these CDRs are never processed again in future scheduler runs.
