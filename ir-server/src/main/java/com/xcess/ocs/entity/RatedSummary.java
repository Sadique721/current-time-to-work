package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "rated_summary")
@SQLDelete(sql = "UPDATE rated_summary SET is_deleted = true, deleted_at = NOW() WHERE summary_id = ?")
@Where(clause = "is_deleted = false")
public class RatedSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "partner_name")
    private String partnerName;

    // ── Prefix-based rating dimensions (SOURCE_DESTINATION_BASED / DESTINATION_BASED) ──

    @Column(name = "source_prefix")
    private String sourcePrefix;

    @Column(name = "source_prefix_name")
    private String sourcePrefixName;

    @Column(name = "source_country_code")
    private String sourceCountryCode;

    @Column(name = "source_country_name")
    private String sourceCountryName;

    @Column(name = "destination_prefix")
    private String destinationPrefix;

    @Column(name = "destination_prefix_name")
    private String destinationPrefixName;

    @Column(name = "destination_country_code")
    private String destinationCountryCode;

    @Column(name = "destination_country_name")
    private String destinationCountryName;

    // ── Zone-based rating dimension (ZONE_DESTINATION_BASED) ──
    // Populated when rated via ZonePrefixTrie. NULL for prefix-based rating.

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    // ── Rating type — which rating path produced this summary row ──
    // Values: SOURCE_DESTINATION_BASED | DESTINATION_BASED | ZONE_DESTINATION_BASED

    @Column(name = "rating_type", length = 30)
    private String ratingType;

    // ── Common dimensions ──

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "direction", nullable = false)
    private String direction;

    @Column(name = "applied_rate", precision = 10, scale = 4)
    private BigDecimal appliedRate;

    // ── Aggregated metrics ──
    // total_calls: COUNT of VOICE CDRs grouped in this row. NULL for SMS/USAGE rows.
    @Column(name = "total_calls")
    private Long totalCalls;

    // total_sms: SUM of event_nos from SMS CDRs grouped in this row. NULL for VOICE/USAGE rows.
    @Column(name = "total_sms")
    private Long totalSms;

    // total_sessions: COUNT of USAGE CDRs grouped in this row. NULL for VOICE/SMS rows.
    @Column(name = "total_sessions")
    private Long totalSessions;

    @Column(name = "total_duration", precision = 10, scale = 2)
    private BigDecimal totalDuration;

    @Column(name = "total_charge", precision = 10, scale = 4, nullable = false)
    private BigDecimal totalCharge;

    @Column(name = "total_data_volume", precision = 19, scale = 4)
    private BigDecimal totalDataVolume;
}
