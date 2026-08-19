package com.xcess.ocs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "scheduler_configuration")
@SQLDelete(sql = "UPDATE scheduler_configuration SET is_deleted = true, deleted_at = NOW() WHERE config_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerConfiguration extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @NotNull(message = "Start timestamp is required")
    @Column(name = "start_timestamp", nullable = false)
    private LocalDateTime startTimestamp;

    @NotNull(message = "Interval type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false)
    private IntervalType intervalType;

    @Positive(message = "Interval value must be positive")
    @Column(name = "interval_value", nullable = false)
    private int intervalValue;

    @NotNull(message = "Targeted timestamp is required")
    @Column(name = "targeted_timestamp", nullable = false)
    private LocalDateTime targetedTimestamp;

    @NotNull(message = "Targeted interval type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "targeted_interval_type", nullable = false)
    private IntervalType targetedIntervalType;

    @Positive(message = "Targeted max interval value must be positive")
    @Column(name = "targeted_max_interval_value", nullable = false)
    private int targetedMaxIntervalValue;

    @Column(name = "is_active", columnDefinition = "boolean default false", nullable = false)
    private boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "interconnect_type", nullable = false)
    private InterconnectType interconnectType;

    public enum IntervalType {
        HOUR("HOUR"),
        DAY("DAY"),
        WEEK("WEEK");

        private final String value;

        IntervalType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SchedulerConfiguration.IntervalType fromString(String value) {
            for (SchedulerConfiguration.IntervalType type : SchedulerConfiguration.IntervalType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid IntervalType: " + value);
        }
    }
}
