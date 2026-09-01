package com.xcess.ocs.entity;

import java.time.DayOfWeek;

public enum WeeklyDay {
    SUN,
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT;

    public DayOfWeek toDayOfWeek() {
        return switch (this) {
            case SUN -> DayOfWeek.SUNDAY;
            case MON -> DayOfWeek.MONDAY;
            case TUE -> DayOfWeek.TUESDAY;
            case WED -> DayOfWeek.WEDNESDAY;
            case THU -> DayOfWeek.THURSDAY;
            case FRI -> DayOfWeek.FRIDAY;
            case SAT -> DayOfWeek.SATURDAY;
        };
    }
}
