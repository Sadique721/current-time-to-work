package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.Partner;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregated summary for a single TAP OUT file.
 *
 * Created by RoamingSummaryGenerationService after TapOutScheduler has
 * finished generating the TAP ASN files and marked CDRs with
 * isTapOutGenerated=true.
 *
 * One row per TapFileRecord — all three service types (VOICE, SMS, USAGE)
 * are aggregated into a single row per file.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tap_out_rated_summary")
public class TapOutRatedSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    /** Date when this summary was generated */
    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    /** The TAP OUT file this summary covers */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_file_id", nullable = false)
    private TapFileRecord tapFileRecord;

    /** Partner (roaming/visited network) resolved from the TAP file */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", columnDefinition = "varchar(20)")
    private com.xcess.ocs.entity.ServiceType serviceType;

    /** Zone name for the grouped CDRs (e.g. "Europe") */
    @Column(name = "zone_name")
    private String zoneName;

    /** COUNT of VoiceRatedCdr records in this file */
    @Column(name = "total_calls")
    private Integer totalCalls;

    /** SUM(SmsRatedCdr.eventNos) for this file */
    @Column(name = "total_sms")
    private Integer totalSms;

    /** SUM(VoiceRatedCdr.durationSeconds) for this file */
    @Column(name = "total_duration_sec")
    private Long totalDurationSec;

    /** SUM(UsageRatedCdr.totalUsage) for this file */
    @Column(name = "total_usage_bytes", precision = 19, scale = 4)
    private BigDecimal totalUsageBytes;

    /** SUM(outgoingTotalCost) across all three CDR types for this file */
    @Column(name = "total_charge", precision = 15, scale = 6)
    private BigDecimal totalCharge;

    /** ISO 4217 currency code from partner.billingCurrency */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Set to true when this summary has been included in a TAP OUT invoice.
     * RoamingTapOutInvoiceService flips this flag after invoice generation.
     */
    @Column(name = "is_invoiced", nullable = false)
    private boolean isInvoiced = false;
}
