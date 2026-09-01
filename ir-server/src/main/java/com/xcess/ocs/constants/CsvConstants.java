package com.xcess.ocs.constants;

/**
 * Standard constants for CSV and spreadsheet parsing, rate package types, and upload operations.
 */
public final class CsvConstants {

    private CsvConstants() {
        // Prevent instantiation
    }

    // CSV Header Column Names
    public static final String HEADER_ZONE_NAME = "zoneName";
    public static final String HEADER_RATE = "rate";
    public static final String HEADER_START_TIME = "startTime";
    public static final String HEADER_END_TIME = "endTime";
    public static final String HEADER_DESTINATION_PREFIX = "destinationPrefix";
    public static final String HEADER_DESTINATION_PREFIX_NAME = "destinationPrefixName";
    public static final String HEADER_SOURCE_PREFIX = "sourcePrefix";
    public static final String HEADER_SOURCE_PREFIX_NAME = "sourcePrefixName";

    // Operation Types
    public static final String OP_REPLACE = "replace";
    public static final String OP_APPEND = "append";

    // Rate Package Types (String representations for template dispatch & sample data)
    public static final String TYPE_DESTINATION_BASED = "DESTINATION_BASED";
    public static final String TYPE_SOURCE_DESTINATION_BASED = "SOURCE_DESTINATION_BASED";
    public static final String TYPE_ZONE_DESTINATION_BASED = "ZONE_DESTINATION_BASED";

    // Template File Names
    public static final String FILE_DESTINATION_BASED_TEMPLATE = "destination_based_template.csv";
    public static final String FILE_SOURCE_DESTINATION_BASED_TEMPLATE = "source_destination_based_template.csv";
    public static final String FILE_ZONE_DESTINATION_BASED_TEMPLATE = "zone_destination_based_template.csv";
}
