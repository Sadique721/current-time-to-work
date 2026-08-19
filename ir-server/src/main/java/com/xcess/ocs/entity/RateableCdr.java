package com.xcess.ocs.entity;

import com.xcess.ocs.roaming.entity.CallType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RateableCdr {

    String getCallingNumber();
    String getCalledNumber();

    /** Call/event start timestamp — stored as LocalDateTime on all CDR entities. */
    LocalDateTime getStartTime();

    /** Call/event end timestamp. */
    LocalDateTime getEndTime();

    String getIncomingAccountId();
    String getOutgoingAccountId();
    String getHomePlmn();
    String getVisitedPlmn();

    /**
     * Returns the PLMN used to look up the partner for wholesale rating.
     * For TAP OUT (foreign sub on our network), this is the Home PLMN.
     * For TAP IN (our sub on foreign network), this is the Visited PLMN.
     */
    default String getPartnerPlmnForRating() {
        return getHomePlmn();
    }

    ServiceType getServiceType();

    /** Returns the TAP call type (MOC, MTC, GPRS, SMS, etc.) for TAP OUT event routing. */
    CallType getCallType();

    /** Returns the outgoing total cost after rating, used for TAP batch charge calculation. */
    BigDecimal getOutgoingTotalCost();

    Integer getEventCountForRating();
    BigDecimal getDurationForRating();

    LineOfBusiness getLineOfBusiness();

    default BigDecimal getUsageAmountForRating() {
        return BigDecimal.ZERO;
    }

    default String getMeasurementUnitForRating() {
        return null;
    }

    String getZoneName();
    void setZoneName(String zoneName);

    void setRatedAt(LocalDateTime ratedAt);

    void markIncomingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                             Long rateDetailId, String sourcePrefix, String destinationPrefix,
                             boolean isSourceDestMatch, BigDecimal totalCost);

    void markIncomingAsUnrated(String reason);
    void markIncomingAsFailed(String reason);

    void markOutgoingAsRated(BigDecimal rate, Long ratePackageId, String ratePackageName,
                             Long rateDetailId, String sourcePrefix, String destinationPrefix,
                             boolean isSourceDestMatch, BigDecimal totalCost);

    void markOutgoingAsUnrated(String reason);
    void markOutgoingAsFailed(String reason);

    RatingStatus getIncomingRatingStatus();
    void setIncomingRatingStatus(RatingStatus status);
    
    String getIncomingRatingFailureReason();
    void setIncomingRatingFailureReason(String reason);

    RatingStatus getOutgoingRatingStatus();
    void setOutgoingRatingStatus(RatingStatus status);

    String getOutgoingRatingFailureReason();
    void setOutgoingRatingFailureReason(String reason);
}
