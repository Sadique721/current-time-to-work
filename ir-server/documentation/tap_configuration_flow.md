# TAP Configuration Flow Overview

This document details the configuration architecture for Transferred Account Procedure (TAP) processing within the OCS Summary Engine. It explains how TAP fields are defined globally and how they are customized and grouped for specific Roaming Partners.

## Core Entities and Their Relationships

The TAP configuration relies on four highly relational entities designed to provide a flexible override mechanism without duplicating data:

1.  **`TapFieldMapping` (Master Field Dictionary)**
2.  **`TapProfile` (The Template)**
3.  **`TapProfileFieldMapping` (The Override Intersection)**
4.  **`TapProfileGroup` (The Partner Assignment)**

---

### 1. `TapFieldMapping` (Master Field Dictionary)
This table acts as the global, master registry for all possible GSMA TAP ASN.1 fields.
*   **Purpose**: Defines the rigid mapping between the binary ASN.1 file structure and the internal Java Domain Models (`RatedCdr` / `TapCdrDTO`).
*   **Key Attributes**:
    *   `asnPath`: The dot-notation path inside the GSMA TAP structure (e.g., `basicCallInformation.destination.dialledDigits`).
    *   `dataType`: How the data is encoded/decoded (e.g., `BCD_STRING`, `ASCII_STRING`).
    *   `outSourceColumn`: The property name on the `RatedCdr` entity to read from when generating an outgoing TAP file (TAP OUT).
    *   `inTargetColumn`: The property name on the `TapCdrDTO` to write to when decoding an incoming TAP file (TAP IN).
    *   `defaultValue` & `isMandatory`: The global, fallback default value and whether the field is strictly required by the standard.

### 2. `TapProfile` (The Template)
A TAP Profile acts as a container that selects a specific subset of fields from the master `TapFieldMapping` dictionary that should be active for a specific scenario.
*   **Purpose**: Different roaming partners (or regions) might require different fields in their TAP files. The profile defines this specific "view".
*   **Key Attributes**:
    *   `serviceType`: Profiles are typically scoped to a specific service type (e.g., `VOICE`, `SMS`, `USAGE`, `GPRS`).

### 3. `TapProfileFieldMapping` (The Override Intersection)
This is the most critical piece for configuration flexibility. It is an intersection table that links a `TapProfile` to a `TapFieldMapping`.
*   **Purpose**: It resolves the many-to-many relationship but, more importantly, allows **per-profile customization** of the master field without modifying the global dictionary.
*   **Key Attributes**:
    *   `customDefaultValue`: If a specific partner requires a different default value (e.g., `999999` instead of `000000`), it is set here. If left null, the system falls back to the master dictionary's `defaultValue`.
    *   `isMandatoryOverride`: Allows a profile to make an optional master field mandatory, or vice versa, for that specific profile.

### 4. `TapProfileGroup` (The Partner Assignment)
Because a partner usually engages in multiple services (Voice, SMS, Data), they require multiple TAP Profiles.
*   **Purpose**: Groups multiple `TapProfile` entities together into a cohesive bundle.
*   **Assignment**: A `TapProfileGroup` is directly assigned to a `Partner` entity (specifically, partners with a `ROAMING` line of business). 

---

## The Configuration Flow (End-to-End)

1.  **Global Definition**: The system administrator defines a universal dictionary of TAP fields in `TapFieldMapping`.
2.  **Profile Creation**: The administrator creates a `TapProfile` (e.g., "EU Standard Voice Profile").
3.  **Field Selection & Override**: The administrator adds fields to this profile by creating `TapProfileFieldMapping` records. They choose which master fields to include and provide any profile-specific overrides (like a custom default value).
4.  **Grouping**: Multiple profiles (e.g., "EU Standard Voice", "EU Standard SMS") are added to a `TapProfileGroup` named "EU Roaming Package".
5.  **Partner Assignment**: Finally, the "EU Roaming Package" `TapProfileGroup` is assigned to a specific `Partner` (e.g., Vodafone UK).

When the `TapOutScheduler` runs, it checks the Partner's assigned `TapProfileGroup`, extracts the relevant `TapProfile` for the call type being processed, and uses the customized `TapProfileFieldMapping`s to generate the highly specific TAP file.
