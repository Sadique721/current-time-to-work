package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RateableCdr;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single decoded and rated CDR from a TAP file (TAP IN direction).
 *
 * Rating flow that populates this entity:
 *   TapCdrDTO (decoded from TAP file)
 *       → RoamingRatingService.rateCdrs()
 *           → ZoneLookupService.getZoneForNumber(ratePackageGroupId, visitedPlmn) → zoneName
 *           → ZoneRateMappingRepository.findActiveByGroupIdAndZoneId()            → RatePackage
 *           → calculateTotalCost(durationMinutes, rateDetails, ratePackage)       → ourCharge
 *       → RoamingCdr saved with rating results
 *
 * Key design change from previous version:
 *   BEFORE: iotRate (FK → roaming_iot_rates) — simple rate × duration / 60
 *   AFTER:  zoneName + ratePackageId + rateDetailId — full pulse-based billing
 *           via the same RatePackage infrastructure used by INTERCONNECT
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "roaming_cdrs")
public class RoamingCdr implements RateableCdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roaming_cdr_id")
    private Long roamingCdrId;

    // ── TAP file reference ────────────────────────────────────────────────────

    /** The TAP file this CDR was decoded from */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_file_id", nullable = false)
    private TapFileRecord tapFileRecord;

    /** TAP_IN = foreign subscriber on our network, TAP_OUT = our subscriber abroad */
    @Enumerated(EnumType.STRING)
    @Column(name = "tap_direction", nullable = false)
    private TapDirection tapDirection;

    // ── Call event fields (decoded from TAP ASN.1) ────────────────────────────

    /** GPRS, MO_VOICE, MT_VOICE, MO_SMS, MT_SMS */
    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", columnDefinition = "VARCHAR(20)")
    private CallType callType;

    /** International Mobile Subscriber Identity — BCD decoded from TAP */
    @Column(name = "imsi", length = 15)
    private String imsi;

    /**
     * Mobile Station ISDN Number — BCD decoded from TAP.
     * This is the subscriber's phone number, used as input to ZoneLookupService
     * for zone-based rating (visitedPlmn is the primary lookup key currently,
     * MSISDN lookup is a future enhancement).
     */
    @Column(name = "msisdn", length = 15)
    private String msisdn;

    @Column(name = "calling_number", length = 20)
    private String callingNumber;

    @Column(name = "called_number", length = 20)
    private String calledNumber;

    @Column(name = "call_start_time")
    private LocalDateTime callStartTime;

    @Column(name = "call_duration_sec")
    private Integer callDurationSec;

    @Column(name = "total_usage", precision = 20, scale = 6)
    private BigDecimal totalUsage;

    @Column(name = "event_nos")
    private Integer eventNos;

    /**
     * Visited PLMN code — the network the subscriber was roaming on.
     * e.g. "23801" (MCC=238, MNC=01)
     *
     * This is the PRIMARY input to ZoneLookupService.getZoneForNumber():
     *   visitedPlmn → ZonePrefixTrie → zoneName → ZoneRateMapping → RatePackage
     */
    @Column(name = "visited_plmn", length = 10)
    private String visitedPlmn;

    /** Home PLMN — derived from IMSI (MCC+MNC) */
    @Column(name = "home_plmn", length = 10)
    private String homePlmn;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", columnDefinition = "ENUM('VOICE','SMS','USAGE')")
    private ServiceType serviceType;

    // ── TAP charge (what the foreign operator claims) ─────────────────────────

    /**
     * The charge as stated in the TAP file by the foreign operator.
     * Scaled: rawCharge / 10^tapDecimalPlaces
     * Stored for comparison against ourCharge (dispute detection).
     */
    @Column(name = "tap_charge", precision = 15, scale = 6)
    private BigDecimal tapCharge;

    @Column(name = "currency", length = 3)
    private String currency;

    // ── Zone-based rating results (replaces old iotRate FK) ───────────────────

    /**
     * Zone name resolved from visitedPlmn via ZonePrefixTrie.
     * e.g. "ZONE_EU", "ZONE_AFRICA"
     * Null if zone lookup failed (ratingStatus = UNRATED).
     */
    @Column(name = "zone_name", length = 100)
    private String zoneName;

    /**
     * The RatePackage ID used for rating this CDR.
     * Must be a ZONE-type package with lineOfBusiness = ROAMING.
     * Replaces the old iot_rate_id FK — rating now uses the full RatePackage
     * infrastructure (pulse-based billing, price rounding) same as INTERCONNECT.
     */
    @Column(name = "rate_package_id")
    private Long ratePackageId;

    /** The RatePackage name — stored for reporting without needing a join */
    @Column(name = "rate_package_name", length = 255)
    private String ratePackageName;

    /**
     * The specific RateDetails row used for this CDR.
     * RateDetails holds the actual rate value, startTime, endTime for time-based rates.
     */
    @Column(name = "rate_detail_id")
    private Long rateDetailId;

    /**
     * The rate applied from RateDetails.rate.
     * Replaces old iot_applied_rate — now sourced from RatePackage/RateDetails
     * instead of RoamingIotRate.
     */
    @Column(name = "applied_rate", precision = 15, scale = 6)
    private BigDecimal appliedRate;

    /**
     * Our calculated charge for this CDR using pulse-based billing:
     *   billableUnits = CEIL(durationInPulseUnits / pulse.noOfUnits)
     *   ourCharge = appliedRate × billableUnits
     *
     * Compare with tapCharge to detect overcharging by the foreign operator.
     */
    @Column(name = "our_charge", precision = 15, scale = 6)
    private BigDecimal ourCharge;

    // ── Rating status ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "rating_status", nullable = false)
    private RoamingRatingStatus ratingStatus;

    /**
     * Failure reason codes (set when ratingStatus = UNRATED or FAILED):
     *   NO_ACCOUNT          — partner has no active account
     *   NO_RATE_PACKAGE     — no RatePackage found via account chain
     *   NO_ZONE_FOUND       — visitedPlmn did not match any zone in ZonePrefixTrie
     *   NO_ZONE_RATE_MAPPING — zone found but no active ZoneRateMapping for this group
     *   NO_MATCHING_RATE    — RatePackage found but no RateDetails matched call time
     *   RATING_ERROR        — unexpected exception during rating
     */
    @Column(name = "rating_failure_reason", columnDefinition = "TEXT")
    private String ratingFailureReason;

    /** True once this CDR has been included in a RoamingRatedSummary */
    @Column(name = "is_summarized")
    private boolean isSummarized;

    /** Populated if this CDR is disputed via a RAP record */
    @Column(name = "rap_file_sequence_no")
    private Integer rapFileSequenceNo;

    // --- RateableCdr implementation ---

    @Override
    public LocalDateTime getStartTime() {
        return callStartTime;
    }

    @Override
    public LocalDateTime getEndTime() {
        if (callStartTime != null && callDurationSec != null) {
            return callStartTime.plusSeconds(callDurationSec);
        }
        return null;
    }

    @Override
    public String getIncomingAccountId() {
        return null;
    }

    @Override
    public String getOutgoingAccountId() {
        return null;
    }

    @Override
    public BigDecimal getOutgoingTotalCost() {
        return ourCharge;
    }

    @Override
    public Integer getEventCountForRating() {
        return eventNos != null ? eventNos : 1;
    }

    @Override
    public BigDecimal getDurationForRating() {
        return callDurationSec != null ? BigDecimal.valueOf(callDurationSec) : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getUsageAmountForRating() {
        return totalUsage != null ? totalUsage : BigDecimal.ZERO;
    }

    @Override
    public String getMeasurementUnitForRating() {
        return "BYTES"; // Default measurement unit for TAP data volume
    }

    @Override
    public LineOfBusiness getLineOfBusiness() {
        return LineOfBusiness.ROAMING;
    }

    @Override
    public String getPartnerPlmnForRating() {
        // For TAP IN (our subscriber on foreign network), the wholesale partner we 
        // are verifying charges against is the Visited PLMN (the foreign network).
        return visitedPlmn;
    }

    @Override
    public void setRatedAt(LocalDateTime ratedAt) {
        // Not used for RoamingCdr
    }

    @Override
    public void markIncomingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                                    Long rateDetailId, String sourcePrefix, String destinationPrefix,
                                    boolean isSourceDestMatch, BigDecimal totalCost) {
        this.appliedRate = rate;
        this.ratePackageId = ratePackageId;
        this.ratePackageName = ratePackageName;
        this.rateDetailId = rateDetailId;
        // NOTE: zoneName is NOT set here from destinationPrefix.
        // zoneName is set explicitly by CdrRatingIntegrationService only when
        // zone-based rating (ZonePrefixTrie lookup) was the path actually taken.
        // If source/destination prefix based rating was used, zoneName stays null.
        this.ourCharge = totalCost;
        this.ratingStatus = RoamingRatingStatus.RATED;
    }

    @Override
    public void markIncomingAsUnrated(String reason) {
        this.ratingStatus = RoamingRatingStatus.UNRATED;
        this.ratingFailureReason = reason;
    }

    @Override
    public void markIncomingAsFailed(String reason) {
        this.ratingStatus = RoamingRatingStatus.FAILED;
        this.ratingFailureReason = reason;
    }

    @Override
    public void markOutgoingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                                    Long rateDetailId, String sourcePrefix, String destinationPrefix,
                                    boolean isSourceDestMatch, BigDecimal totalCost) {
        markIncomingAsRated(rate, ratePackageId, ratePackageName, rateDetailId, sourcePrefix, destinationPrefix, isSourceDestMatch, totalCost);
    }

    @Override
    public void markOutgoingAsUnrated(String reason) {
        markIncomingAsUnrated(reason);
    }

    @Override
    public void markOutgoingAsFailed(String reason) {
        markIncomingAsFailed(reason);
    }

    @Override
    public RatingStatus getIncomingRatingStatus() {
        return null;
    }

    @Override
    public void setIncomingRatingStatus(RatingStatus status) {

    }

    @Override
    public String getIncomingRatingFailureReason() {
        return "";
    }

    @Override
    public void setIncomingRatingFailureReason(String reason) {

    }

    @Override
    public RatingStatus getOutgoingRatingStatus() {
        return null;
    }

    @Override
    public void setOutgoingRatingStatus(RatingStatus status) {

    }

    @Override
    public String getOutgoingRatingFailureReason() {
        return "";
    }

    @Override
    public void setOutgoingRatingFailureReason(String reason) {

    }
}
