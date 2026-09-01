package com.xcess.ocs.service;

import com.xcess.ocs.entity.Agreement;
import com.xcess.ocs.entity.BillingType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Service
public class BillingCycleCalculatorService {

    public BillingCycleResult calculate(LocalDate cycleStart, Agreement agreement) {
        BillingType type = agreement.getBillingType();
        if (type == null) {
            type = BillingType.DAYS;
        }

        return switch (type) {
            case DAYS -> days(cycleStart, agreement.getBillingCyclePeriod());
            case WEEKLY -> weekly(cycleStart);
            case FORTNIGHTLY -> fortnightly(cycleStart, agreement.getBillingCycleStartDate());
            case MONTHLY -> monthly(cycleStart);
        };
    }

    private BillingCycleResult days(LocalDate cycleStart, Integer period) {
        if (period == null || period < 1) {
            throw new IllegalArgumentException("billingCyclePeriod is required and must be >= 1 for DAYS billing type");
        }
        LocalDate cycleEnd = cycleStart.plusDays(period - 1);
        return new BillingCycleResult(cycleStart, cycleEnd, cycleEnd.plusDays(1));
    }

    private BillingCycleResult weekly(LocalDate cycleStart) {
        LocalDate cycleEnd = cycleStart.plusDays(6);
        return new BillingCycleResult(cycleStart, cycleEnd, cycleEnd.plusDays(1));
    }

    private BillingCycleResult fortnightly(LocalDate cycleStart, LocalDate originalStart) {
        if (originalStart == null) {
            originalStart = cycleStart;
        }
        int anchorDay = originalStart.getDayOfMonth();
        int effectiveAnchor = Math.min(anchorDay, cycleStart.lengthOfMonth());

        if (cycleStart.getDayOfMonth() == effectiveAnchor) {
            // Leg 1: exactly 15 calendar days (cycleStart + 14)
            LocalDate cycleEnd = cycleStart.plusDays(14);
            return new BillingCycleResult(cycleStart, cycleEnd, cycleEnd.plusDays(1));
        } else {
            // Leg 2: run until next occurrence of anchor day
            LocalDate nextStart;
            int effectiveInCurrent = Math.min(anchorDay, cycleStart.lengthOfMonth());
            if (effectiveInCurrent > cycleStart.getDayOfMonth()) {
                nextStart = cycleStart.withDayOfMonth(effectiveInCurrent);
            } else {
                YearMonth nextMonth = YearMonth.from(cycleStart).plusMonths(1);
                nextStart = nextMonth.atDay(Math.min(anchorDay, nextMonth.lengthOfMonth()));
            }
            LocalDate cycleEnd = nextStart.minusDays(1);
            return new BillingCycleResult(cycleStart, cycleEnd, nextStart);
        }
    }

    private BillingCycleResult monthly(LocalDate cycleStart) {
        LocalDate nextStart = cycleStart.plusMonths(1);
        LocalDate cycleEnd = nextStart.minusDays(1);
        return new BillingCycleResult(cycleStart, cycleEnd, nextStart);
    }
}
