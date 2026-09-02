USE xcessocs;

-- 1. Insert 60 Voice Failed Records
INSERT INTO voice_rated_cdr (
    created_date, is_deleted, modified_date, called_number, calling_number,
    start_time, end_time, incoming_account_id, outgoing_account_id,
    incoming_rating_status, outgoing_rating_status,
    incoming_rating_failure_reason, outgoing_rating_failure_reason,
    source_id, line_of_business, service_type, is_tap_out_generated, is_summarized,
    home_plmn, visited_plmn, duration_seconds
)
SELECT 
    NOW() - INTERVAL (n * 10) MINUTE,
    0,
    NOW() - INTERVAL (n * 10) MINUTE,
    CONCAT('9198765', LPAD(n, 4, '0')),
    CONCAT('9187654', LPAD(n, 4, '0')),
    NOW() - INTERVAL (n * 10 + 5) MINUTE,
    NOW() - INTERVAL (n * 10) MINUTE,
    ELT((n % 4) + 1, 'ACC-C-TELIA', 'ACC-C-VODAFONE', 'ACC-C-ORANGE', 'ACC-C-AIRTEL'),
    ELT((n % 4) + 1, 'ACC-V-TATA', 'ACC-V-BHARTI', 'ACC-V-MTN', 'ACC-V-VERIZON'),
    'FAILED',
    'FAILED',
    ELT((n % 5) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_END_TIME', 'INVALID_TIMESTAMP', 'NO_RATE_PACKAGE', 'NO_MATCHING_RATE'),
    ELT((n % 5) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_END_TIME', 'INVALID_TIMESTAMP', 'NO_RATE_PACKAGE', 'NO_MATCHING_RATE'),
    1,
    IF(n % 2 = 0, 'INTERCONNECT', 'ROAMING'),
    'VOICE',
    0,
    0,
    IF(n % 2 = 1, '40445', NULL),
    IF(n % 2 = 1, '40420', NULL),
    (n * 15) % 300 + 30
FROM (
    SELECT a.N + b.N * 10 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) b
    WHERE (a.N + b.N * 10 + 1) <= 60
) numbers;

-- 2. Insert 50 SMS Failed Records
INSERT INTO sms_rated_cdr (
    created_date, is_deleted, modified_date, called_number, calling_number,
    start_time, end_time, event_nos, incoming_account_id, outgoing_account_id,
    incoming_rating_status, outgoing_rating_status,
    incoming_rating_failure_reason, outgoing_rating_failure_reason,
    source_id, line_of_business, service_type, is_tap_out_generated, is_summarized,
    home_plmn, visited_plmn
)
SELECT 
    NOW() - INTERVAL (n * 12) MINUTE,
    0,
    NOW() - INTERVAL (n * 12) MINUTE,
    CONCAT('9191234', LPAD(n, 4, '0')),
    CONCAT('9181234', LPAD(n, 4, '0')),
    NOW() - INTERVAL (n * 12 + 2) MINUTE,
    NOW() - INTERVAL (n * 12) MINUTE,
    1,
    ELT((n % 4) + 1, 'ACC-C-TELIA', 'ACC-C-VODAFONE', 'ACC-C-ORANGE', 'ACC-C-AIRTEL'),
    ELT((n % 4) + 1, 'ACC-V-TATA', 'ACC-V-BHARTI', 'ACC-V-MTN', 'ACC-V-VERIZON'),
    'FAILED',
    'FAILED',
    ELT((n % 5) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_CALLED_NUMBER', 'INVALID_TIMESTAMP', 'NO_RATE_PACKAGE', 'INVALID_MESSAGE_COUNT'),
    ELT((n % 5) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_CALLED_NUMBER', 'INVALID_TIMESTAMP', 'NO_RATE_PACKAGE', 'INVALID_MESSAGE_COUNT'),
    1,
    IF(n % 2 = 0, 'INTERCONNECT', 'ROAMING'),
    'SMS',
    0,
    0,
    IF(n % 2 = 1, '40445', NULL),
    IF(n % 2 = 1, '310410', NULL)
FROM (
    SELECT a.N + b.N * 10 + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
    CROSS JOIN (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b
    WHERE (a.N + b.N * 10 + 1) <= 50
) numbers;

-- 3. Insert 10 Usage Failed Records
INSERT INTO usage_rated_cdr (
    created_date, is_deleted, modified_date, subscriber_identity, access_point_name,
    start_time, end_time, total_usage, measurement_unit,
    incoming_account_id, outgoing_account_id,
    incoming_rating_status, outgoing_rating_status,
    incoming_rating_failure_reason, outgoing_rating_failure_reason,
    source_id, line_of_business, service_type, is_tap_out_generated, is_summarized,
    home_plmn, visited_plmn, total_usage_bytes
)
SELECT 
    NOW() - INTERVAL (n * 15) MINUTE,
    0,
    NOW() - INTERVAL (n * 15) MINUTE,
    CONCAT('IMSI40445', LPAD(n, 6, '0')),
    ELT((n % 3) + 1, 'internet.roam', 'ims.lte', 'data.apn'),
    NOW() - INTERVAL (n * 15 + 10) MINUTE,
    NOW() - INTERVAL (n * 15) MINUTE,
    n * 1024.5000,
    'MB',
    ELT((n % 3) + 1, 'ACC-C-TELIA', 'ACC-C-VODAFONE', 'ACC-C-ORANGE'),
    ELT((n % 3) + 1, 'ACC-V-TATA', 'ACC-V-BHARTI', 'ACC-V-MTN'),
    'FAILED',
    'FAILED',
    ELT((n % 3) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_ACCESS_POINT_NAME', 'NO_RATE_PACKAGE'),
    ELT((n % 3) + 1, 'NULL_ACCOUNT_ID_AND_PLMN', 'MISSING_ACCESS_POINT_NAME', 'NO_RATE_PACKAGE'),
    1,
    IF(n % 2 = 0, 'INTERCONNECT', 'ROAMING'),
    'USAGE',
    0,
    0,
    IF(n % 2 = 1, '40445', NULL),
    IF(n % 2 = 1, '310410', NULL),
    n * 1024 * 1024
FROM (
    SELECT a.N + 1 AS n
    FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a
) numbers;
