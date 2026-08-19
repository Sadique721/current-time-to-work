package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.ServiceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roaming_rated_summary")
public class RoamingRatedSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_file_id", nullable = false)
    private TapFileRecord tapFileRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "tap_direction", nullable = false)
    private TapDirection tapDirection;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Column(name = "destination_prefix", length = 20)
    private String destinationPrefix;

    /** COUNT of VOICE CDRs (number of calls) in this summary group */
    @Column(name = "total_calls")
    private Integer totalCalls;

    /** SUM(RoamingCdr.eventNos) for SMS CDRs — actual message count, not CDR count */
    @Column(name = "total_sms")
    private Integer totalSms;

    @Column(name = "total_duration_sec", nullable = false)
    private Long totalDurationSec;

    /** SUM(RoamingCdr.totalUsage) for USAGE-type CDRs in this group, stored as raw bytes. */
    @Column(name = "total_usage_bytes", precision = 19, scale = 4)
    private BigDecimal totalUsageBytes;

    @Column(name = "total_tap_charge", precision = 15, scale = 6)
    private BigDecimal totalTapCharge;

    @Column(name = "total_our_charge", precision = 15, scale = 6)
    private BigDecimal totalOurCharge;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "is_invoiced", nullable = false)
    private boolean isInvoiced = false;
}
