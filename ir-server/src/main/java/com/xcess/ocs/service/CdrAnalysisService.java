package com.xcess.ocs.service;

import com.xcess.ocs.dto.CdrAnalysisDTO;
import com.xcess.ocs.dto.CdrAnalysisSummaryDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.search.CdrFilterRequest;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.SmsRatedCdr;
import com.xcess.ocs.entity.UsageRatedCdr;
import com.xcess.ocs.entity.VoiceRatedCdr;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.mapper.CdrAnalysisMapper;
import com.xcess.ocs.util.PaginationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Enterprise Production-Grade Service for CDR Analysis management.
 * Leverages MapStruct for mapping and unified predicate builder for ultra-fast, DRY query execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdrAnalysisService {

    private final EntityManager em;
    private final CdrAnalysisMapper cdrAnalysisMapper;

    /**
     * Fetch paginated summary CDR analysis records with 5 compact fields:
     * id, serviceType, lineOfBusiness, incomingRatingStatus, outgoingRatingStatus.
     * Automatically excludes FAILED rating status and sorts by latest modifiedDate DESC.
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<CdrAnalysisSummaryDTO> getPaginatedCdrSummary(PageRequestDTO<CdrFilterRequest> pageRequest) {
        log.info("Processing CDR Analysis summary pagination request: page={}, pageSize={}", 
                pageRequest.getPage(), pageRequest.getPageSize());

        int pageNum = pageRequest.getPage() > 0 ? pageRequest.getPage() - 1 : 0;
        int pageSize = pageRequest.getPageSize() > 0 ? pageRequest.getPageSize() : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        CdrFilterRequest filter = pageRequest.getSearchCriteria() != null ? pageRequest.getSearchCriteria() : new CdrFilterRequest();

        // If serviceType is explicitly specified, query that single service
        if (filter.getServiceType() != null) {
            return switch (filter.getServiceType()) {
                case VOICE -> fetchVoiceSummary(filter, pageable);
                case SMS   -> fetchSmsSummary(filter, pageable);
                case USAGE -> fetchUsageSummary(filter, pageable);
            };
        }

        // If serviceType is NULL, fetch latest records from ALL 3 service types combined!
        return fetchAllCombinedSummary(filter, pageable);
    }

    /**
     * Fetch full detailed CDR record by ID and optional ServiceType when user clicks on a summary row.
     */
    @Transactional(readOnly = true)
    public CdrAnalysisDTO getCdrDetails(Long id, ServiceType serviceType) {
        log.info("Fetching full CDR details for id={}, serviceType={}", id, serviceType);

        if (serviceType == null) {
            VoiceRatedCdr v = em.find(VoiceRatedCdr.class, id);
            if (v != null) return cdrAnalysisMapper.mapVoiceToDTO(v);

            SmsRatedCdr s = em.find(SmsRatedCdr.class, id);
            if (s != null) return cdrAnalysisMapper.mapSmsToDTO(s);

            UsageRatedCdr u = em.find(UsageRatedCdr.class, id);
            if (u != null) return cdrAnalysisMapper.mapUsageToDTO(u);

            throw new ResourceNotFoundException("CDR record not found with ID: " + id);
        }

        return switch (serviceType) {
            case VOICE -> {
                VoiceRatedCdr v = em.find(VoiceRatedCdr.class, id);
                if (v == null) throw new ResourceNotFoundException("Voice CDR not found with ID: " + id);
                yield cdrAnalysisMapper.mapVoiceToDTO(v);
            }
            case SMS -> {
                SmsRatedCdr s = em.find(SmsRatedCdr.class, id);
                if (s == null) throw new ResourceNotFoundException("SMS CDR not found with ID: " + id);
                yield cdrAnalysisMapper.mapSmsToDTO(s);
            }
            case USAGE -> {
                UsageRatedCdr u = em.find(UsageRatedCdr.class, id);
                if (u == null) throw new ResourceNotFoundException("Usage CDR not found with ID: " + id);
                yield cdrAnalysisMapper.mapUsageToDTO(u);
            }
        };
    }

    // ─── COMBINED SUMMARY (EXCLUDING FAILED STATUS) ────────────────────────────

    private PageResponseDTO<CdrAnalysisSummaryDTO> fetchAllCombinedSummary(CdrFilterRequest f, Pageable pageable) {
        int maxFetch = pageable.getPageSize() * (pageable.getPageNumber() + 1);
        
        List<CdrAnalysisDTO> voiceList = fetchVoiceListForCombined(f, maxFetch);
        List<CdrAnalysisDTO> smsList   = fetchSmsListForCombined(f, maxFetch);
        List<CdrAnalysisDTO> usageList = fetchUsageListForCombined(f, maxFetch);

        List<CdrAnalysisDTO> combinedList = new ArrayList<>(voiceList.size() + smsList.size() + usageList.size());
        combinedList.addAll(voiceList);
        combinedList.addAll(smsList);
        combinedList.addAll(usageList);

        // Sort all records by latest modifiedDate DESCENDING
        combinedList.sort(Comparator.comparing(this::getSortTimestamp, Comparator.nullsLast(Comparator.reverseOrder())));

        long totalCount = countVoiceCdrs(f) + countSmsCdrs(f) + countUsageCdrs(f);

        int start = Math.min((int) pageable.getOffset(), combinedList.size());
        int end = Math.min(start + pageable.getPageSize(), combinedList.size());
        List<CdrAnalysisDTO> subList = combinedList.subList(start, end);

        // Map to 5-field summary DTOs via MapStruct mapper
        List<CdrAnalysisSummaryDTO> summaryList = subList.stream()
                .map(dto -> cdrAnalysisMapper.toSummaryDTO(
                        dto.getId(), dto.getServiceType(), dto.getLineOfBusiness(), 
                        dto.getIncomingRatingStatus(), dto.getOutgoingRatingStatus()))
                .toList();

        PageImpl<CdrAnalysisSummaryDTO> pageResult = new PageImpl<>(summaryList, pageable, totalCount);
        return PaginationUtils.buildGetResponseDTO(summaryList, pageResult);
    }

    private LocalDateTime getSortTimestamp(CdrAnalysisDTO dto) {
        if (dto.getModifiedDate() != null) return dto.getModifiedDate();
        if (dto.getRatedAt() != null) return dto.getRatedAt();
        if (dto.getCreatedDate() != null) return dto.getCreatedDate();
        return dto.getStartTime();
    }

    // ─── VOICE SUMMARY & QUERIES ───────────────────────────────────────────────

    private PageResponseDTO<CdrAnalysisSummaryDTO> fetchVoiceSummary(CdrFilterRequest f, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<VoiceRatedCdr> cq = cb.createQuery(VoiceRatedCdr.class);
        Root<VoiceRatedCdr> root = cq.from(VoiceRatedCdr.class);

        cq.where(buildVoicePredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("ratedCdrId")));

        TypedQuery<VoiceRatedCdr> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<VoiceRatedCdr> contentList = query.getResultList();

        long totalCount = countVoiceCdrs(f);

        List<CdrAnalysisSummaryDTO> summaryList = contentList.stream()
                .map(v -> cdrAnalysisMapper.toSummaryDTO(
                        v.getRatedCdrId(), ServiceType.VOICE, v.getLineOfBusiness(), 
                        v.getIncomingRatingStatus(), v.getOutgoingRatingStatus()))
                .toList();

        PageImpl<CdrAnalysisSummaryDTO> pageResult = new PageImpl<>(summaryList, pageable, totalCount);
        return PaginationUtils.buildGetResponseDTO(summaryList, pageResult);
    }

    private List<CdrAnalysisDTO> fetchVoiceListForCombined(CdrFilterRequest f, int maxResults) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<VoiceRatedCdr> cq = cb.createQuery(VoiceRatedCdr.class);
        Root<VoiceRatedCdr> root = cq.from(VoiceRatedCdr.class);

        cq.where(buildVoicePredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("ratedCdrId")));

        TypedQuery<VoiceRatedCdr> query = em.createQuery(cq);
        query.setMaxResults(maxResults);
        return query.getResultList().stream().map(cdrAnalysisMapper::mapVoiceToDTO).toList();
    }

    private long countVoiceCdrs(CdrFilterRequest f) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<VoiceRatedCdr> countRoot = countQuery.from(VoiceRatedCdr.class);
        countQuery.select(cb.count(countRoot)).where(buildVoicePredicates(cb, countRoot, f).toArray(new Predicate[0]));
        return em.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildVoicePredicates(CriteriaBuilder cb, Root<VoiceRatedCdr> root, CdrFilterRequest f) {
        List<Predicate> predicates = buildBasePredicates(cb, root, f, "callingNumber", "calledNumber", "startTime");
        addEqualIfPresent(predicates, cb, root.get("callType"), f.getCallType());
        return predicates;
    }

    // ─── SMS SUMMARY & QUERIES ─────────────────────────────────────────────────

    private PageResponseDTO<CdrAnalysisSummaryDTO> fetchSmsSummary(CdrFilterRequest f, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SmsRatedCdr> cq = cb.createQuery(SmsRatedCdr.class);
        Root<SmsRatedCdr> root = cq.from(SmsRatedCdr.class);

        cq.where(buildSmsPredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("smsRatedCdrId")));

        TypedQuery<SmsRatedCdr> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<SmsRatedCdr> contentList = query.getResultList();

        long totalCount = countSmsCdrs(f);

        List<CdrAnalysisSummaryDTO> summaryList = contentList.stream()
                .map(s -> cdrAnalysisMapper.toSummaryDTO(
                        s.getSmsRatedCdrId(), ServiceType.SMS, s.getLineOfBusiness(), 
                        s.getIncomingRatingStatus(), s.getOutgoingRatingStatus()))
                .toList();

        PageImpl<CdrAnalysisSummaryDTO> pageResult = new PageImpl<>(summaryList, pageable, totalCount);
        return PaginationUtils.buildGetResponseDTO(summaryList, pageResult);
    }

    private List<CdrAnalysisDTO> fetchSmsListForCombined(CdrFilterRequest f, int maxResults) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SmsRatedCdr> cq = cb.createQuery(SmsRatedCdr.class);
        Root<SmsRatedCdr> root = cq.from(SmsRatedCdr.class);

        cq.where(buildSmsPredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("smsRatedCdrId")));

        TypedQuery<SmsRatedCdr> query = em.createQuery(cq);
        query.setMaxResults(maxResults);
        return query.getResultList().stream().map(cdrAnalysisMapper::mapSmsToDTO).toList();
    }

    private long countSmsCdrs(CdrFilterRequest f) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SmsRatedCdr> countRoot = countQuery.from(SmsRatedCdr.class);
        countQuery.select(cb.count(countRoot)).where(buildSmsPredicates(cb, countRoot, f).toArray(new Predicate[0]));
        return em.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildSmsPredicates(CriteriaBuilder cb, Root<SmsRatedCdr> root, CdrFilterRequest f) {
        List<Predicate> predicates = buildBasePredicates(cb, root, f, "callingNumber", "calledNumber", "createdDate");
        addEqualIfPresent(predicates, cb, root.get("callType"), f.getCallType());
        return predicates;
    }

    // ─── USAGE SUMMARY & QUERIES ───────────────────────────────────────────────

    private PageResponseDTO<CdrAnalysisSummaryDTO> fetchUsageSummary(CdrFilterRequest f, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UsageRatedCdr> cq = cb.createQuery(UsageRatedCdr.class);
        Root<UsageRatedCdr> root = cq.from(UsageRatedCdr.class);

        cq.where(buildUsagePredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("ratedCdrId")));

        TypedQuery<UsageRatedCdr> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<UsageRatedCdr> contentList = query.getResultList();

        long totalCount = countUsageCdrs(f);

        List<CdrAnalysisSummaryDTO> summaryList = contentList.stream()
                .map(u -> cdrAnalysisMapper.toSummaryDTO(
                        u.getRatedCdrId(), ServiceType.USAGE, u.getLineOfBusiness(), 
                        u.getIncomingRatingStatus(), u.getOutgoingRatingStatus()))
                .toList();

        PageImpl<CdrAnalysisSummaryDTO> pageResult = new PageImpl<>(summaryList, pageable, totalCount);
        return PaginationUtils.buildGetResponseDTO(summaryList, pageResult);
    }

    private List<CdrAnalysisDTO> fetchUsageListForCombined(CdrFilterRequest f, int maxResults) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UsageRatedCdr> cq = cb.createQuery(UsageRatedCdr.class);
        Root<UsageRatedCdr> root = cq.from(UsageRatedCdr.class);

        cq.where(buildUsagePredicates(cb, root, f).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("modifiedDate")), cb.desc(root.get("ratedCdrId")));

        TypedQuery<UsageRatedCdr> query = em.createQuery(cq);
        query.setMaxResults(maxResults);
        return query.getResultList().stream().map(cdrAnalysisMapper::mapUsageToDTO).toList();
    }

    private long countUsageCdrs(CdrFilterRequest f) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<UsageRatedCdr> countRoot = countQuery.from(UsageRatedCdr.class);
        countQuery.select(cb.count(countRoot)).where(buildUsagePredicates(cb, countRoot, f).toArray(new Predicate[0]));
        return em.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildUsagePredicates(CriteriaBuilder cb, Root<UsageRatedCdr> root, CdrFilterRequest f) {
        return buildBasePredicates(cb, root, f, "subscriberIdentity", "accessPointName", "startTime");
    }

    // ─── UNIFIED BASE PREDICATE BUILDER (ZERO REPETITION) ─────────────────────

    private <T> List<Predicate> buildBasePredicates(CriteriaBuilder cb, Root<T> root, CdrFilterRequest f,
            String callingOrSubscriberField, String calledOrApnField, String timestampField) {
        List<Predicate> predicates = new ArrayList<>();

        // Exclude FAILED status
        predicates.add(cb.notEqual(root.get("incomingRatingStatus"), RatingStatus.FAILED));
        predicates.add(cb.notEqual(root.get("outgoingRatingStatus"), RatingStatus.FAILED));

        // Shared dynamic predicates
        addEqualIfPresent(predicates, cb, root.get("lineOfBusiness"), f.getLineOfBusiness());
        addEqualIfPresent(predicates, cb, root.get("incomingRatingStatus"), f.getIncomingRatingStatus());
        addEqualIfPresent(predicates, cb, root.get("outgoingRatingStatus"), f.getOutgoingRatingStatus());
        addLikeIfPresent(predicates, cb, root.get(callingOrSubscriberField), f.getCallingOrSubscriber());
        addLikeIfPresent(predicates, cb, root.get(calledOrApnField), f.getCalledOrApn());
        addEqualIfPresent(predicates, cb, root.get("incomingAccountId"), f.getIncomingAccountId());
        addEqualIfPresent(predicates, cb, root.get("outgoingAccountId"), f.getOutgoingAccountId());
        addEqualIfPresent(predicates, cb, root.get("homePlmn"), f.getHomePlmn());
        addEqualIfPresent(predicates, cb, root.get("visitedPlmn"), f.getVisitedPlmn());
        addEqualIfPresent(predicates, cb, root.get("zoneName"), f.getZoneName());
        addRangeIfPresent(predicates, cb, root.get(timestampField), f.getFromTime(), f.getToTime());

        return predicates;
    }

    private <T> void addEqualIfPresent(List<Predicate> list, CriteriaBuilder cb, Path<T> path, T value) {
        if (value != null) {
            list.add(cb.equal(path, value));
        }
    }

    private void addLikeIfPresent(List<Predicate> list, CriteriaBuilder cb, Path<String> path, String value) {
        if (value != null && !value.isBlank()) {
            list.add(cb.like(path, "%" + value.trim() + "%"));
        }
    }

    private void addRangeIfPresent(List<Predicate> list, CriteriaBuilder cb, Path<LocalDateTime> path, LocalDateTime from, LocalDateTime to) {
        if (from != null) {
            list.add(cb.greaterThanOrEqualTo(path, from));
        }
        if (to != null) {
            list.add(cb.lessThanOrEqualTo(path, to));
        }
    }
}
