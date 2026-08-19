package com.xcess.ocs.validator;

import com.xcess.ocs.exception.InvalidInputException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PrefixPatternValidator {

    private static final int MIN_PREFIX_LENGTH = 2;
    private static final int MAX_PREFIX_LENGTH = 15;
    private static final int MAX_PREFIX_COUNT  = 2000;

    /**
     * Validates and normalizes a raw comma-separated manual prefix string.
     *
     * Rules enforced:
     *   - Each token must be digits only (no +, no letters, no spaces within token)
     *   - Each token length must be 2–15 digits (covers PLMN 5-6 and E.164 up to 15)
     *   - No duplicates within the same input
     *   - Maximum 2000 prefixes per zone
     *   - Empty tokens (double comma, trailing comma) are silently dropped
     *
     * @param raw the raw input string from the admin
     * @return normalized string: deduplicated, sorted, joined with "," no spaces
     * @throws InvalidInputException if any validation rule is violated
     */
    public static String validateAndNormalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidInputException("Prefix pattern is required");
        }

        String[] tokens = raw.split(",");
        List<String> errors = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;

            if (!t.matches("\\d+")) {
                errors.add("'" + t + "' contains non-digit characters");
                continue;
            }
            if (t.length() < MIN_PREFIX_LENGTH) {
                errors.add("'" + t + "' is too short (minimum " + MIN_PREFIX_LENGTH + " digits)");
                continue;
            }
            if (t.length() > MAX_PREFIX_LENGTH) {
                errors.add("'" + t + "' is too long (maximum " + MAX_PREFIX_LENGTH + " digits)");
                continue;
            }
            if (!seen.add(t)) {
                errors.add("duplicate prefix '" + t + "'");
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidInputException("Invalid prefix pattern — " + String.join("; ", errors));
        }
        if (seen.isEmpty()) {
            throw new InvalidInputException("No valid prefixes found in the provided pattern");
        }
        if (seen.size() > MAX_PREFIX_COUNT) {
            throw new InvalidInputException(
                    "Too many prefixes: " + seen.size() + " provided, maximum allowed is " + MAX_PREFIX_COUNT);
        }

        return seen.stream().sorted().reduce((a, b) -> a + "," + b).orElseThrow();
    }
}
