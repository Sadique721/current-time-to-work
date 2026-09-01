package com.xcess.ocs.service;

import java.time.LocalDate;
import java.util.Objects;

public record BillingCycleResult(LocalDate cycleStart, LocalDate cycleEnd, LocalDate nextCycleStart) {
    public BillingCycleResult {
        Objects.requireNonNull(cycleStart, "cycleStart cannot be null");
        Objects.requireNonNull(cycleEnd, "cycleEnd cannot be null");
        Objects.requireNonNull(nextCycleStart, "nextCycleStart cannot be null");
        if (cycleEnd.isBefore(cycleStart)) {
            throw new IllegalArgumentException("cycleEnd (" + cycleEnd + ") cannot be before cycleStart (" + cycleStart + ")");
        }
        if (!nextCycleStart.equals(cycleEnd.plusDays(1))) {
            throw new IllegalArgumentException("nextCycleStart (" + nextCycleStart + ") must equal cycleEnd + 1 day (" + cycleEnd.plusDays(1) + ")");
        }
    }
}
