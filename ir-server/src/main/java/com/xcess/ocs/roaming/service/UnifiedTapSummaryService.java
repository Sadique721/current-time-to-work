package com.xcess.ocs.roaming.service;

import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.roaming.dto.RoamingRatedSummarySearchDTO;
import com.xcess.ocs.roaming.dto.RoamingRatedSummaryViewDTO;
import com.xcess.ocs.roaming.entity.RoamingRatedSummary;
import com.xcess.ocs.roaming.entity.TapOutRatedSummary;
import com.xcess.ocs.roaming.repository.RoamingRatedSummaryRepository;
import com.xcess.ocs.roaming.repository.TapOutRatedSummaryRepository;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnifiedTapSummaryService {

    private final RoamingRatedSummaryRepository inRepository;
    private final TapOutRatedSummaryRepository outRepository;

    public PageResponseDTO<RoamingRatedSummaryViewDTO> getUnifiedSummariesInPage(
            PageRequestDTO<RoamingRatedSummarySearchDTO> pageRequestDTO) {

        PageRequest pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        
        // 1. Fetch from TAP IN
        Specification<RoamingRatedSummary> inSpec = buildInSpec(pageRequestDTO.getSearchCriteria());
        List<RoamingRatedSummary> inList = inRepository.findAll(inSpec);
        
        // 2. Fetch from TAP OUT
        Specification<TapOutRatedSummary> outSpec = buildOutSpec(pageRequestDTO.getSearchCriteria());
        List<TapOutRatedSummary> outList = outRepository.findAll(outSpec);
        
        // 3. Map to DTOs
        List<RoamingRatedSummaryViewDTO> combined = new ArrayList<>();
        combined.addAll(inList.stream().map(this::toDTO).collect(Collectors.toList()));
        combined.addAll(outList.stream().map(this::toDTO).collect(Collectors.toList()));
        
        // 4. Sort (e.g. by summaryDate DESC, then tapFileId DESC)
        combined.sort(Comparator.comparing(RoamingRatedSummaryViewDTO::getSummaryDate)
                .thenComparing((RoamingRatedSummaryViewDTO dto) -> dto.getTapFileId() == null ? 0L : dto.getTapFileId())
                .reversed());
                
        // 5. Paginate in memory
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), combined.size());
        List<RoamingRatedSummaryViewDTO> pageContent = new ArrayList<>();
        if (start < combined.size()) {
            pageContent = combined.subList(start, end);
        }
        
        Page<RoamingRatedSummaryViewDTO> page = new PageImpl<>(pageContent, pageable, combined.size());
        return PaginationUtils.buildGetResponseDTO(pageContent, page);
    }

    private Specification<RoamingRatedSummary> buildInSpec(RoamingRatedSummarySearchDTO search) {
        return (root, query, cb) -> {
            if (search == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> tapFileJoin = root.join("tapFileRecord");
            Join<Object, Object> partnerJoin = tapFileJoin.join("partner");

            if (search.getSummaryDate() != null) {
                predicates.add(cb.equal(root.get("summaryDate"), search.getSummaryDate()));
            }
            if (search.getFileName() != null) {
                predicates.add(cb.like(cb.lower(tapFileJoin.get("fileName")), "%" + search.getFileName().toLowerCase() + "%"));
            }
            // filter by tapDirection if provided
            if (search.getTapDirection() != null && search.getTapDirection() != com.xcess.ocs.roaming.entity.TapDirection.TAP_IN) {
                predicates.add(cb.disjunction()); // Will return no results if searching for OUT
            }
            if (search.getTapVersion() != null) {
                predicates.add(cb.equal(tapFileJoin.get("tapVersion"), search.getTapVersion()));
            }
            if (search.getPartnerName() != null) {
                predicates.add(cb.like(cb.lower(partnerJoin.get("partnerName")), "%" + search.getPartnerName().toLowerCase() + "%"));
            }
            if (search.getPartnerId() != null) {
                predicates.add(cb.equal(partnerJoin.get("partnerId"), search.getPartnerId()));
            }
            if (search.getServiceType() != null) {
                predicates.add(cb.equal(root.get("serviceType"), search.getServiceType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<TapOutRatedSummary> buildOutSpec(RoamingRatedSummarySearchDTO search) {
        return (root, query, cb) -> {
            if (search == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            Join<Object, Object> tapFileJoin = root.join("tapFileRecord");
            Join<Object, Object> partnerJoin = tapFileJoin.join("partner");

            if (search.getSummaryDate() != null) {
                predicates.add(cb.equal(root.get("summaryDate"), search.getSummaryDate()));
            }
            if (search.getFileName() != null) {
                predicates.add(cb.like(cb.lower(tapFileJoin.get("fileName")), "%" + search.getFileName().toLowerCase() + "%"));
            }
            // filter by tapDirection if provided
            if (search.getTapDirection() != null && search.getTapDirection() != com.xcess.ocs.roaming.entity.TapDirection.TAP_OUT) {
                predicates.add(cb.disjunction()); // Will return no results if searching for IN
            }
            if (search.getTapVersion() != null) {
                predicates.add(cb.equal(tapFileJoin.get("tapVersion"), search.getTapVersion()));
            }
            if (search.getPartnerName() != null) {
                predicates.add(cb.like(cb.lower(partnerJoin.get("partnerName")), "%" + search.getPartnerName().toLowerCase() + "%"));
            }
            if (search.getPartnerId() != null) {
                predicates.add(cb.equal(partnerJoin.get("partnerId"), search.getPartnerId()));
            }
            if (search.getServiceType() != null) {
                predicates.add(cb.equal(root.get("serviceType"), search.getServiceType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private RoamingRatedSummaryViewDTO toDTO(RoamingRatedSummary s) {
        RoamingRatedSummaryViewDTO dto = new RoamingRatedSummaryViewDTO();
        dto.setSummaryId(s.getSummaryId());
        dto.setSummaryDate(s.getSummaryDate());
        dto.setTapFileName(s.getTapFileRecord() != null ? s.getTapFileRecord().getFileName() : null);
        dto.setTapFileId(s.getTapFileRecord() != null ? s.getTapFileRecord().getTapFileId() : null);
        dto.setTapDirection("IN"); // TAP IN
        dto.setTapVersion(s.getTapFileRecord() != null ? s.getTapFileRecord().getTapVersion() : null);
        dto.setPartnerName(s.getTapFileRecord() != null && s.getTapFileRecord().getPartner() != null
                ? s.getTapFileRecord().getPartner().getPartnerName() : null);
        dto.setServiceType(s.getServiceType() != null ? s.getServiceType().name() : null);
        dto.setZoneName(s.getDestinationPrefix());
        dto.setTotalCalls(s.getTotalCalls());
        dto.setTotalSms(s.getTotalSms());
        dto.setTotalDurationSec(s.getTotalDurationSec());
        dto.setTotalUsageBytes(s.getTotalUsageBytes());
        dto.setTotalTapCharge(s.getTotalTapCharge());
        dto.setOurCharge(s.getTotalOurCharge());
        return dto;
    }

    private RoamingRatedSummaryViewDTO toDTO(TapOutRatedSummary s) {
        RoamingRatedSummaryViewDTO dto = new RoamingRatedSummaryViewDTO();
        // Prepend an identifier to avoid ID collision if needed, but summaryId is mostly for keying
        dto.setSummaryId(s.getSummaryId());
        dto.setSummaryDate(s.getSummaryDate());
        dto.setTapFileName(s.getTapFileRecord() != null ? s.getTapFileRecord().getFileName() : null);
        dto.setTapFileId(s.getTapFileRecord() != null ? s.getTapFileRecord().getTapFileId() : null);
        dto.setTapDirection("OUT"); // TAP OUT
        dto.setTapVersion(s.getTapFileRecord() != null ? s.getTapFileRecord().getTapVersion() : null);
        dto.setPartnerName(s.getTapFileRecord() != null && s.getTapFileRecord().getPartner() != null
                ? s.getTapFileRecord().getPartner().getPartnerName() : null);
        dto.setServiceType(s.getServiceType() != null ? s.getServiceType().name() : null);
        dto.setZoneName(s.getZoneName());
        dto.setTotalCalls(s.getTotalCalls());
        dto.setTotalSms(s.getTotalSms());
        dto.setTotalDurationSec(s.getTotalDurationSec());
        dto.setTotalUsageBytes(s.getTotalUsageBytes());
        dto.setTotalTapCharge(s.getTotalCharge());
        dto.setOurCharge(s.getTotalCharge()); // Assuming totalCharge = ourCharge for TAP OUT since we charge the partner
        return dto;
    }
}
