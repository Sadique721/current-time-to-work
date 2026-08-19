package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.roaming.dto.RoamingRatedCdrDTO;
import com.xcess.ocs.roaming.entity.RoamingCdr;
import com.xcess.ocs.roaming.entity.RoamingRatingStatus;
import com.xcess.ocs.roaming.repository.RoamingCdrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roaming/cdrs")
@RequiredArgsConstructor
public class RoamingCdrController {

    private final RoamingCdrRepository roamingCdrRepository;

    @GetMapping
    public List<RoamingRatedCdrDTO> list(@RequestParam(required = false) Long tapFileId,
                                         @RequestParam(required = false) Long partnerId,
                                         @RequestParam(required = false) RoamingRatingStatus ratingStatus) {
        List<RoamingCdr> cdrs;
        if (tapFileId != null && ratingStatus != null) {
            cdrs = roamingCdrRepository.findByTapFileRecord_TapFileIdAndRatingStatus(tapFileId, ratingStatus);
        } else if (tapFileId != null) {
            cdrs = roamingCdrRepository.findByTapFileRecord_TapFileId(tapFileId);
        } else if (partnerId != null) {
            cdrs = roamingCdrRepository.findByTapFileRecord_Partner_PartnerId(partnerId);
        } else if (ratingStatus != null) {
            cdrs = roamingCdrRepository.findByRatingStatus(ratingStatus);
        } else {
            cdrs = roamingCdrRepository.findAll();
        }
        return cdrs.stream().map(this::toDTO).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoamingRatedCdrDTO> getById(@PathVariable Long id) {
        return roamingCdrRepository.findById(id)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    private RoamingRatedCdrDTO toDTO(RoamingCdr c) {
        RoamingRatedCdrDTO dto = new RoamingRatedCdrDTO();
        dto.setRoamingCdrId(c.getRoamingCdrId());
        if (c.getTapFileRecord() != null) {
            dto.setTapFileId(c.getTapFileRecord().getTapFileId());
            dto.setFileName(c.getTapFileRecord().getFileName());
            if (c.getTapFileRecord().getPartner() != null) {
                dto.setPartnerId(c.getTapFileRecord().getPartner().getPartnerId());
                dto.setPartnerCode(c.getTapFileRecord().getPartner().getPartnerCode());
            }
        }
        dto.setTapDirection(c.getTapDirection());
        dto.setCallType(c.getCallType());
        dto.setImsi(c.getImsi());
        dto.setMsisdn(c.getMsisdn());
        dto.setCallingNumber(c.getCallingNumber());
        dto.setCalledNumber(c.getCalledNumber());
        dto.setCallStartTime(c.getCallStartTime());
        dto.setCallDurationSec(c.getCallDurationSec());
        dto.setVisitedPlmn(c.getVisitedPlmn());
        dto.setHomePlmn(c.getHomePlmn());
        dto.setServiceType(c.getServiceType());
        dto.setTapCharge(c.getTapCharge());
        dto.setCurrency(c.getCurrency());
        dto.setZoneName(c.getZoneName());
        dto.setRatePackageId(c.getRatePackageId());
        dto.setRatePackageName(c.getRatePackageName());
        dto.setRateDetailId(c.getRateDetailId());
        dto.setAppliedRate(c.getAppliedRate());
        dto.setOurCharge(c.getOurCharge());
        dto.setRatingStatus(c.getRatingStatus());
        dto.setRatingFailureReason(c.getRatingFailureReason());
        return dto;
    }
}
