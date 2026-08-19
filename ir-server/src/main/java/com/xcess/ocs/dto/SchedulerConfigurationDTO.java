package com.xcess.ocs.dto;

import com.xcess.ocs.entity.InterconnectType;
import com.xcess.ocs.entity.SchedulerConfiguration;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerConfigurationDTO {
    @NotNull(message = "Start timestamp is required")
    private LocalDateTime startTimestamp;

    @NotNull(message = "Interval type is required")
    private SchedulerConfiguration.IntervalType intervalType;

    @Positive(message = "Interval value must be positive")
    private int intervalValue;

    @NotNull(message = "Targeted timestamp is required")
    private LocalDateTime targetedTimestamp;

    @NotNull(message = "Targeted interval type is required")
    private SchedulerConfiguration.IntervalType targetedIntervalType;

    @Positive(message = "Targeted max interval value must be positive")
    private int targetedMaxIntervalValue;

    @NotNull(message = "InterconnectType is required")
    private InterconnectType interconnectType;

    private boolean isActive;

    public void validate() {
        // targetedTimestamp (CDR start) should be before or equal to startTimestamp (scheduler start)
        if (targetedTimestamp.isAfter(startTimestamp)) {
            throw new IllegalArgumentException(
                "Targeted timestamp (CDR start) must be before or equal to start timestamp (scheduler start)");
        }
    }
}
