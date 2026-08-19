package com.xcess.ocs.roaming.service;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.config.TapFieldPathResolver;
import com.xcess.ocs.roaming.dto.TapCdrDTO;
import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.roaming.entity.TapDirection;
import com.xcess.ocs.roaming.entity.TapFieldMapping;
import com.xcess.ocs.roaming.entity.TapProfile;
import com.xcess.ocs.roaming.entity.TapProfileFieldMapping;
import com.xcess.ocs.roaming.entity.TapProfileGroup;
import com.xcess.ocs.tapmodels.tap.CallEventDetail;
import com.xcess.ocs.tapmodels.tap.CallEventDetailList;
import com.xcess.ocs.tapmodels.tap.DataInterChange;
import com.xcess.ocs.tapmodels.tap.TransferBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts {@link TapCdrDTO} records from a decoded GSMA {@link DataInterChange}
 * using the partner's assigned TAP profile group.
 * Profile is selected per-event by matching the event's service type to
 * the serviceType field on each TapProfile in the group.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TapCdrExtractorService {

    private final TapFieldPathResolver resolver;

    public List<TapCdrDTO> extract(DataInterChange dic, TapDirection direction,
                                   int tapDecimalPlaces, Partner partner) {
        List<TapCdrDTO> result = new ArrayList<>();
        TransferBatch batch = dic.getTransferBatch();
        if (batch == null) return result;
        CallEventDetailList detailList = batch.getCallEventDetails();
        if (detailList == null) return result;
        List<CallEventDetail> events = detailList.getCallEventDetail();
        if (events == null || events.isEmpty()) return result;

        TapProfileGroup group = partner.getTapProfileGroup();
        if (group == null || group.getTapProfiles().isEmpty()) {
            throw new IllegalStateException(
                    "No TAP profile group assigned to partner=" + partner.getPartnerCode());
        }

        for (CallEventDetail event : events) {
            TapCdrDTO dto = extractEvent(event, direction, tapDecimalPlaces, group);
            if (dto != null) result.add(dto);
        }
        log.info("Extracted {} CDRs (direction={}, group={})", result.size(), direction, group.getName());
        return result;
    }

    public List<TapCdrDTO> extract(DataInterChange dic, TapDirection direction, Partner partner) {
        return extract(dic, direction, 0, partner);
    }

    // ── per-event dispatch ───────────────────────────────────────────────────

    private TapCdrDTO extractEvent(CallEventDetail event, TapDirection direction,
                                   int dp, TapProfileGroup group) {
        Object eventObj;
        CallType callType;
        ServiceType serviceType;

        if (event.getMobileOriginatedCall() != null) {
            eventObj = event.getMobileOriginatedCall();
            callType = CallType.MO_VOICE;
            serviceType = ServiceType.VOICE;
        } else if (event.getMobileTerminatedCall() != null) {
            eventObj = event.getMobileTerminatedCall();
            callType = CallType.MT_VOICE;
            serviceType = ServiceType.VOICE;
        } else if (event.getGprsCall() != null) {
            eventObj = event.getGprsCall();
            callType = CallType.GPRS;
            serviceType = ServiceType.USAGE;
        } else if (event.getMobileSession() != null) {
            eventObj = event.getMobileSession();
            callType = CallType.MO_SMS;
            serviceType = ServiceType.SMS;
        } else {
            return null;
        }

        // Resolve profile from group by matching event's service type
        TapProfile profile = group.getTapProfiles().stream()
                .filter(p -> serviceType.name().equalsIgnoreCase(p.getServiceType()))
                .findFirst()
                .orElse(null);
        if (profile == null) {
            log.warn("No TAP profile found for serviceType={} in group={}, skipping event",
                    serviceType, group.getName());
            return null;
        }

        List<TapProfileFieldMapping> mappings = profile.getFieldMappings();

        TapCdrDTO dto = new TapCdrDTO();
        dto.setTapDirection(direction);
        dto.setCallType(callType);
        dto.setServiceType(serviceType);

        for (TapProfileFieldMapping profileMapping : mappings) {
            TapFieldMapping master = profileMapping.getTapFieldMapping();

            if (master.getCallType() != null && master.getCallType() != callType) {
                boolean bothSms = (callType == CallType.MO_SMS || callType == CallType.MT_SMS) 
                        && (master.getCallType() == CallType.MO_SMS || master.getCallType() == CallType.MT_SMS);
                if (!bothSms) continue;
            }

            String rawValue = resolver.resolve(eventObj, master.getAsnPath());

            if (rawValue == null) {
                rawValue = profileMapping.getCustomDefaultValue() != null
                        ? profileMapping.getCustomDefaultValue()
                        : master.getDefaultValue();
            }

            boolean isMandatory = profileMapping.getIsMandatoryOverride() != null
                    ? profileMapping.getIsMandatoryOverride()
                    : Boolean.TRUE.equals(master.getIsMandatory());
            if (rawValue == null && isMandatory) {
                throw new IllegalArgumentException(String.format(
                        "Mandatory TAP IN field missing: %s (path: %s, callType: %s)",
                        master.getFieldName(), master.getAsnPath(), callType));
            }

            if (rawValue != null && master.getInTargetColumn() != null) {
                Object decoded = resolver.decodeValue(rawValue, master.getDataType(), dp);
                resolver.setPropertyValue(dto, master.getInTargetColumn(), decoded);
            }
        }
        return dto;
    }
}
