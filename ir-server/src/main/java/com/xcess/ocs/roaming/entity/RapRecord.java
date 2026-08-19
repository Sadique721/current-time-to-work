package com.xcess.ocs.roaming.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rap_records")
public class RapRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rap_id")
    private Long rapId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_file_id", nullable = false)
    private TapFileRecord tapFileRecord;

    @Column(name = "rap_file_name", nullable = false)
    private String rapFileName;

    @Column(name = "rap_sequence_no")
    private Integer rapSequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rap_direction", nullable = false)
    private RapDirection rapDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RapStatus status;

    @Column(name = "disputed_amount", precision = 15, scale = 6)
    private BigDecimal disputedAmount;

    @Column(name = "reason_code", length = 20)
    private String reasonCode;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
