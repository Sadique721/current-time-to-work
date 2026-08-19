package com.xcess.ocs.entity;

import com.xcess.ocs.roaming.entity.TapFileRecord;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sms_rated_cdr")
@SQLDelete(sql = "UPDATE sms_rated_cdr SET is_deleted = true, deleted_at = NOW() WHERE sms_rated_cdr_id = ?")
@Where(clause = "is_deleted = false")
public class SmsRatedCdr extends BaseEntity implements RateableCdr {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long smsRatedCdrId;

    @Column(name = "calling_number", nullable = false)
    private String callingNumber;

    @Column(name = "called_number", nullable = false)
    private String calledNumber;

    /** SMS event timestamp — stored as proper datetime for correct billing date bucketing. */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "incoming_account_id", nullable = false)
    private String incomingAccountId;

    @Column(name = "outgoing_account_id", nullable = false)
    private String outgoingAccountId;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    // ========================================================================
    // INCOMING RATING FIELDS
    // ========================================================================

    @Column(name = "incoming_applied_rate", precision = 10, scale = 4)
    private BigDecimal incomingAppliedRate;

    @Column(name = "incoming_rate_package_id")
    private Long incomingRatePackageId;

    @Column(name = "incoming_rate_package_name")
    private String incomingRatePackageName;

    @Column(name = "incoming_matched_source_prefix")
    private String incomingMatchedSourcePrefix;

    @Column(name = "incoming_matched_destination_prefix")
    private String incomingMatchedDestinationPrefix;

    @Column(name = "incoming_total_cost", precision = 10, scale = 4)
    private BigDecimal incomingTotalCost;

    @Column(name = "incoming_rate_detail_id")
    private Long incomingRateDetailId;

    @Column(name = "incoming_is_source_destination_match")
    private Boolean incomingIsSourceDestinationMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoming_rating_status")
    private RatingStatus incomingRatingStatus;

    @Column(name = "incoming_rating_failure_reason")
    private String incomingRatingFailureReason;

    // ========================================================================
    // OUTGOING RATING FIELDS
    // ========================================================================

    @Column(name = "outgoing_applied_rate", precision = 10, scale = 4)
    private BigDecimal outgoingAppliedRate;

    @Column(name = "outgoing_rate_package_id")
    private Long outgoingRatePackageId;

    @Column(name = "outgoing_rate_package_name")
    private String outgoingRatePackageName;

    @Column(name = "outgoing_matched_source_prefix")
    private String outgoingMatchedSourcePrefix;

    @Column(name = "outgoing_matched_destination_prefix")
    private String outgoingMatchedDestinationPrefix;

    @Column(name = "outgoing_total_cost", precision = 10, scale = 4)
    private BigDecimal outgoingTotalCost;

    @Column(name = "outgoing_rate_detail_id")
    private Long outgoingRateDetailId;

    @Column(name = "outgoing_is_source_destination_match")
    private Boolean outgoingIsSourceDestinationMatch;

    @Enumerated(EnumType.STRING)
    @Column(name = "outgoing_rating_status")
    private RatingStatus outgoingRatingStatus;

    @Column(name = "outgoing_rating_failure_reason")
    private String outgoingRatingFailureReason;

    // ========================================================================
    // SHARED FIELDS
    // ========================================================================

    @Column(name = "rated_at")
    private LocalDateTime ratedAt;

    // SMS has no duration — billing unit is message count (event_nos).
    // duration_seconds column removed to avoid misleading zero values in summary.

    @Column(name = "is_summarized")
    private Boolean isSummarized = false;

    @Column(name = "is_tap_out_generated", nullable = false)
    private Boolean isTapOutGenerated = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", nullable = false)
    private LineOfBusiness lineOfBusiness = LineOfBusiness.INTERCONNECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_file_record_id", referencedColumnName = "tap_file_id")
    private TapFileRecord tapFileRecord;

    // ========================================================================
    // ROAMING-SPECIFIC FIELDS
    // ========================================================================

    @Column(name = "home_plmn")
    private String homePlmn;

    @Column(name = "visited_plmn")
    private String visitedPlmn;

    @Column(name = "imsi", length = 15)
    private String imsi;

    @Column(name = "msisdn", length = 15)
    private String msisdn;

    @Column(name = "imei", length = 20)
    private String imei;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType;

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", columnDefinition = "VARCHAR(20)")
    private com.xcess.ocs.roaming.entity.CallType callType;

    /** Total number of SMS messages in this CDR event. */
    @Column(name = "event_nos")
    private Integer eventNos;

    // ========================================================================
    // HELPER METHODS FOR RATING
    // ========================================================================

    public void markIncomingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                                    Long rateDetailId, String sourcePrefix, String destinationPrefix,
                                    boolean isSourceDestMatch, BigDecimal totalCost) {
        this.incomingAppliedRate = rate;
        this.incomingRatePackageId = ratePackageId;
        this.incomingRatePackageName = ratePackageName;
        this.incomingRateDetailId = rateDetailId;
        this.incomingMatchedSourcePrefix = sourcePrefix;
        this.incomingMatchedDestinationPrefix = destinationPrefix;
        this.incomingIsSourceDestinationMatch = isSourceDestMatch;
        this.incomingTotalCost = totalCost;
        this.incomingRatingStatus = RatingStatus.RATED;
        this.incomingRatingFailureReason = null;
    }

    public void markIncomingAsUnrated(String reason) {
        this.incomingRatingStatus = RatingStatus.UNRATED;
        this.incomingRatingFailureReason = reason;
    }

    public void markIncomingAsFailed(String reason) {
        this.incomingRatingStatus = RatingStatus.FAILED;
        this.incomingRatingFailureReason = reason;
    }

    public void markOutgoingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                                    Long rateDetailId, String sourcePrefix, String destinationPrefix,
                                    boolean isSourceDestMatch, BigDecimal totalCost) {
        this.outgoingAppliedRate = rate;
        this.outgoingRatePackageId = ratePackageId;
        this.outgoingRatePackageName = ratePackageName;
        this.outgoingRateDetailId = rateDetailId;
        this.outgoingMatchedSourcePrefix = sourcePrefix;
        this.outgoingMatchedDestinationPrefix = destinationPrefix;
        this.outgoingIsSourceDestinationMatch = isSourceDestMatch;
        this.outgoingTotalCost = totalCost;
        this.outgoingRatingStatus = RatingStatus.RATED;
        this.outgoingRatingFailureReason = null;
    }

    public void markOutgoingAsUnrated(String reason) {
        this.outgoingRatingStatus = RatingStatus.UNRATED;
        this.outgoingRatingFailureReason = reason;
    }

    public void markOutgoingAsFailed(String reason) {
        this.outgoingRatingStatus = RatingStatus.FAILED;
        this.outgoingRatingFailureReason = reason;
    }

    public void markAsPending() {
        this.incomingRatingStatus = RatingStatus.PENDING;
        this.outgoingRatingStatus = RatingStatus.PENDING;
        this.ratedAt = null;
    }

    @Override
    public Integer getEventCountForRating() {
        return getEventNos();
    }

    @Override
    public BigDecimal getDurationForRating() {
        return BigDecimal.ZERO;
    }

    @Override
    public LineOfBusiness getLineOfBusiness() {
        return lineOfBusiness;
    }
}
