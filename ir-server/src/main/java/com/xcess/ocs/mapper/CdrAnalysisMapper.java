package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.CdrAnalysisDTO;
import com.xcess.ocs.dto.CdrAnalysisSummaryDTO;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.entity.VoiceRatedCdr;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct Mapper for converting Voice, SMS, and Usage CDR entities to unified CdrAnalysisDTO objects.
 * Uses Spring componentModel for dependency injection into CdrAnalysisService.
 */
@Mapper(componentModel = "spring")
public interface CdrAnalysisMapper {

    @Mapping(target = "id", source = "ratedCdrId")
    @Mapping(target = "serviceType", expression = "java(com.xcess.ocs.entity.ServiceType.VOICE)")
    @Mapping(target = "callingOrSubscriber", source = "callingNumber")
    @Mapping(target = "calledOrApn", source = "calledNumber")
    @Mapping(target = "durationOrUsage", source = "durationSeconds", qualifiedByName = "longToString")
    @Mapping(target = "callType", source = "callType", qualifiedByName = "callTypeToString")
    CdrAnalysisDTO mapVoiceToDTO(VoiceRatedCdr entity);

    @Mapping(target = "id", source = "smsRatedCdrId")
    @Mapping(target = "serviceType", expression = "java(com.xcess.ocs.entity.ServiceType.SMS)")
    @Mapping(target = "callingOrSubscriber", source = "callingNumber")
    @Mapping(target = "calledOrApn", source = "calledNumber")
    @Mapping(target = "durationOrUsage", source = "eventNos", qualifiedByName = "integerToString")
    @Mapping(target = "callType", source = "callType", qualifiedByName = "callTypeToString")
    CdrAnalysisDTO mapSmsToDTO(SmsRatedCdr entity);

    @Mapping(target = "id", source = "ratedCdrId")
    @Mapping(target = "serviceType", expression = "java(com.xcess.ocs.entity.ServiceType.USAGE)")
    @Mapping(target = "callingOrSubscriber", source = "subscriberIdentity")
    @Mapping(target = "calledOrApn", source = "accessPointName")
    @Mapping(target = "durationOrUsage", expression = "java(entity.getTotalUsage() != null ? entity.getTotalUsage().toString() + \" \" + (entity.getMeasurementUnit() != null ? entity.getMeasurementUnit() : \"BYTES\") : \"0 BYTES\")")
    @Mapping(target = "callType", ignore = true)
    CdrAnalysisDTO mapUsageToDTO(UsageRatedCdr entity);

    default CdrAnalysisSummaryDTO toSummaryDTO(Long id, ServiceType serviceType,
            LineOfBusiness lineOfBusiness, RatingStatus incomingRatingStatus, RatingStatus outgoingRatingStatus) {
        return new CdrAnalysisSummaryDTO(id, serviceType, lineOfBusiness, incomingRatingStatus, outgoingRatingStatus);
    }

    @Named("longToString")
    default String longToString(Long value) {
        return value != null ? value.toString() : "0";
    }

    @Named("integerToString")
    default String integerToString(Integer value) {
        return value != null ? value.toString() : "1";
    }

    @Named("callTypeToString")
    default String callTypeToString(com.xcess.ocs.roaming.entity.CallType callType) {
        return callType != null ? callType.name() : null;
    }
}
