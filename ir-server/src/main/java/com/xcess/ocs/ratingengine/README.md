# Telecom Rating Engine

This package contains the rating engine implementation for the OCS (Online Charging System) platform. The rating engine provides efficient telecom rate lookups using a RadixTrie data structure.

## Package Structure

```
com.xcess.ocs.ratingengine/
├── service/                # Services for rate lookup and CDR rating
│   ├── AccountRateService.java           # Account-to-rate package mapping
│   ├── CdrRatingIntegrationService.java  # CDR processing and rating
│   ├── RateLookupService.java            # High-level rate lookup
│   ├── RateTrieService.java              # RadixTrie implementation for rate lookup
│   └── TrieInitializationService.java    # Service to initialize tries
├── util/                   # Utility classes for the rating engine
│   ├── RadixTrie.java                    # Generic RadixTrie implementation
│   └── RateTrie.java                     # Telecom-specific RadixTrie wrapper
└── README.md               # This file
```

## Migration Notice

The rating engine code has been relocated from various packages to this consolidated package structure. The original classes are now deprecated and delegate to these new implementations:

- `com.xcess.ocs.ratingengine.RateLookupService` → `com.xcess.ocs.ratingengine.service.RateLookupService`
- `com.xcess.ocs.ratingengine.RateTrieService` → `com.xcess.ocs.ratingengine.service.RateTrieService`
- `com.xcess.ocs.util.RadixTrie` → `com.xcess.ocs.ratingengine.util.RadixTrie`
- `com.xcess.ocs.util.RateTrie` → `com.xcess.ocs.ratingengine.util.RateTrie`
- `com.xcess.ocs.service.CdrRatingIntegrationService` → `com.xcess.ocs.ratingengine.service.CdrRatingIntegrationService`
- `com.xcess.ocs.service.AccountRateService` → `com.xcess.ocs.ratingengine.service.AccountRateService`

Please update your imports to use the new package structure. The old classes will be removed in a future release.

## Rating Process

The rating engine uses a two-stage lookup process:

1. **Account Lookup**: Determine the appropriate rate package for a CDR based on the account information.
2. **Rate Lookup**: Find the best matching rate within that package using the RadixTrie data structure.

### RadixTrie Implementation

The rating engine uses a RadixTrie (compressed prefix tree) for efficient O(k) prefix-based lookups, where k is the length of the prefix. This is particularly well-suited for telecom rate lookups where rates are defined based on phone number prefixes.

The implementation supports:

- Source-destination prefix combinations
- Destination-only prefix lookups
- Time-based rate validity

## Architecture

The rating engine is built around the RadixTrie data structure, which provides O(k) lookup complexity for prefix matching (where k is the length of the prefix). The main components are:

```
CDR Processing Flow
├── Account Lookup → AccountRateService
│   └── Traverse Account → ProductPlan → RatePackageGroup → RatePackage
├── Rate Lookup → RateLookupService, RateTrieService
│   └── RadixTrie-based prefix matching
└── Rating Application → CdrRatingIntegrationService
    └── Apply rates and calculate costs
```

## Key Components

### 1. Core Data Structure
- **RateTrie**: An optimized RadixTrie implementation that provides fast prefix-based lookups for telecom rates.

### 2. Services
- **RateTrieService**: Manages RadixTries for different rate packages, providing O(k) lookups for prefix matching.
- **RateLookupService**: Coordinates the lookup process, finding the best rate for a given source and destination.
- **AccountRateService**: Determines which rate package applies to an account at a specific time, with caching for performance.
- **CdrRatingIntegrationService**: Integrates CDR processing with the rating engine, handling the complete flow from CDR receipt to rating application.

### 3. Caching
- **CacheConfig**: Configures caching for account-to-rate-package lookups, improving performance for repeated account lookups.

## Key Features

1. **Efficient Prefix Matching**: O(k) lookup complexity for finding the longest matching prefix.
2. **Account-Based Rate Derivation**: No need to include rate package IDs in Kafka messages.
3. **Two-Phase Lookup**: First tries source+destination match, then falls back to destination-only match.
4. **Time-Based Rates**: Supports rates that vary based on time of day, day of week, etc.
5. **Performance Optimizations**: Caching, efficient data structures, and minimal database access.

## Usage

The main entry point for rating is the `CdrRatingIntegrationService.processAndRateCdr()` method, which:

1. Creates a `RatedCdr` entity from the Kafka DTO
2. Determines the appropriate rate package based on account information
3. Finds the best rate within that package based on source and destination prefixes
4. Applies the rate to the CDR and calculates total cost

## Performance Considerations

- **Trie Initialization**: RadixTries are built at application startup and on rate package updates.
- **Account Lookup Caching**: Account-to-rate-package mappings are cached to avoid repeated database traversals.
- **Memory Usage**: The RadixTrie structure is memory-efficient, compressing common prefixes.

## Example

For a call from account "A12345" with source number "1212555123" to destination "91987654321":

1. **Account Lookup**: Find which rate package applies to account "A12345"
   - Result: "International Standard Package" (ID: 123)

2. **Rate Lookup**: Find best rate in package 123 for source "1212555123" to destination "91987654321"
   - First try: Source prefix "1212" + Destination prefix "91" → $0.15 per minute
   - If no match: Try destination-only → Prefix "91" → $0.18 per minute

3. **Rating Application**: Apply rate to CDR
   - Set rate details, matched prefixes, rate package info
   - Calculate total cost based on call duration 