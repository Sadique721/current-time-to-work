package com.xcess.ocs.service;

import com.xcess.ocs.dto.search.CdrFilterRequest;
import com.xcess.ocs.entity.*;
import com.xcess.ocs.roaming.entity.CallType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CdrExportService {

    private final EntityManager em;

    public CdrExportService(EntityManager em) {
        this.em = em;
    }

    // ─── Dropdown APIs ────────────────────────────────────────────────────────

    public List<String> getDistinctZoneNames() {
        List<String> result = new ArrayList<>();
        result.addAll(distinct(VoiceRatedCdr.class, "zoneName"));
        result.addAll(distinct(SmsRatedCdr.class, "zoneName"));
        result.addAll(distinct(UsageRatedCdr.class, "zoneName"));
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    public List<String> getDistinctHomePlmn() {
        List<String> result = new ArrayList<>();
        result.addAll(distinct(VoiceRatedCdr.class, "homePlmn"));
        result.addAll(distinct(SmsRatedCdr.class, "homePlmn"));
        result.addAll(distinct(UsageRatedCdr.class, "homePlmn"));
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    public List<String> getDistinctVisitedPlmn() {
        List<String> result = new ArrayList<>();
        result.addAll(distinct(VoiceRatedCdr.class, "visitedPlmn"));
        result.addAll(distinct(SmsRatedCdr.class, "visitedPlmn"));
        result.addAll(distinct(UsageRatedCdr.class, "visitedPlmn"));
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    public List<String> getDistinctIncomingAccountIds() {
        List<String> result = new ArrayList<>();
        result.addAll(distinct(VoiceRatedCdr.class, "incomingAccountId"));
        result.addAll(distinct(SmsRatedCdr.class, "incomingAccountId"));
        result.addAll(distinct(UsageRatedCdr.class, "incomingAccountId"));
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    public List<String> getDistinctOutgoingAccountIds() {
        List<String> result = new ArrayList<>();
        result.addAll(distinct(VoiceRatedCdr.class, "outgoingAccountId"));
        result.addAll(distinct(SmsRatedCdr.class, "outgoingAccountId"));
        result.addAll(distinct(UsageRatedCdr.class, "outgoingAccountId"));
        result.sort(Comparator.naturalOrder());
        return result.stream().distinct().toList();
    }

    private <T> List<String> distinct(Class<T> entityClass, String field) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<String> q = cb.createQuery(String.class);
        Root<T> root = q.from(entityClass);
        q.select(root.get(field)).distinct(true)
                .where(cb.and(
                        cb.isNotNull(root.get(field)),
                        cb.notEqual(cb.trim(root.get(field)), ""),
                        cb.notEqual(cb.lower(root.get(field)), "null")
                ));
        return em.createQuery(q).getResultList();
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    public byte[] exportToExcel(CdrFilterRequest filter) throws IOException {
        if (filter.getServiceType() == null) {
            throw new IllegalArgumentException("serviceType is required: VOICE, SMS or USAGE");
        }
        return switch (filter.getServiceType()) {
            case VOICE -> exportVoice(filter);
            case SMS   -> exportSms(filter);
            case USAGE -> exportUsage(filter);
        };
    }

    // ─── VOICE ───────────────────────────────────────────────────────────────

    private byte[] exportVoice(CdrFilterRequest f) throws IOException {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<VoiceRatedCdr> q = cb.createQuery(VoiceRatedCdr.class);
        Root<VoiceRatedCdr> root = q.from(VoiceRatedCdr.class);

        List<Predicate> predicates = new ArrayList<>();
        if (f.getCallingOrSubscriber() != null) predicates.add(cb.like(root.get("callingNumber"), "%" + f.getCallingOrSubscriber() + "%"));
        if (f.getCalledOrApn()         != null) predicates.add(cb.like(root.get("calledNumber"), "%" + f.getCalledOrApn() + "%"));
        addCommonPredicates(cb, root, f, predicates);
        if (f.getFromTime() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), f.getFromTime()));
        if (f.getToTime()   != null) predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), f.getToTime()));
        if (f.getCallType() != null) predicates.add(cb.equal(root.get("callType"), f.getCallType()));

        q.where(predicates.toArray(new Predicate[0]));
        List<VoiceRatedCdr> rows = em.createQuery(q).getResultList();

        String[] headers = {"Calling Number", "Called Number", "Start Time", "End Time",
                "Incoming Account", "Outgoing Account", "Incoming Status", "Outgoing Status",
                "Home PLMN", "Visited PLMN", "Zone Name", "Line Of Business", "Service Type",
                "Call Type", "Incoming Cost", "Outgoing Cost", "Duration (sec)", "Rated At"};

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Voice CDR");
            writeHeader(sheet, wb, headers);
            int rowNum = 1;
            for (VoiceRatedCdr r : rows) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(str(r.getCallingNumber()));
                row.createCell(c++).setCellValue(str(r.getCalledNumber()));
                row.createCell(c++).setCellValue(str(r.getStartTime()));
                row.createCell(c++).setCellValue(str(r.getEndTime()));
                row.createCell(c++).setCellValue(str(r.getIncomingAccountId()));
                row.createCell(c++).setCellValue(str(r.getOutgoingAccountId()));
                row.createCell(c++).setCellValue(str(r.getIncomingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getOutgoingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getHomePlmn()));
                row.createCell(c++).setCellValue(str(r.getVisitedPlmn()));
                row.createCell(c++).setCellValue(str(r.getZoneName()));
                row.createCell(c++).setCellValue(str(r.getLineOfBusiness()));
                row.createCell(c++).setCellValue(str(r.getServiceType()));
                row.createCell(c++).setCellValue(str(r.getCallType()));
                row.createCell(c++).setCellValue(r.getIncomingTotalCost() != null ? r.getIncomingTotalCost().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getOutgoingTotalCost() != null ? r.getOutgoingTotalCost().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getDurationSeconds() != null ? r.getDurationSeconds().toString() : "N/A");
                row.createCell(c).setCellValue(str(r.getRatedAt()));
            }
            autoSize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ─── SMS ─────────────────────────────────────────────────────────────────

    private byte[] exportSms(CdrFilterRequest f) throws IOException {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SmsRatedCdr> q = cb.createQuery(SmsRatedCdr.class);
        Root<SmsRatedCdr> root = q.from(SmsRatedCdr.class);

        List<Predicate> predicates = new ArrayList<>();
        if (f.getCallingOrSubscriber() != null) predicates.add(cb.like(root.get("callingNumber"), "%" + f.getCallingOrSubscriber() + "%"));
        if (f.getCalledOrApn()         != null) predicates.add(cb.like(root.get("calledNumber"), "%" + f.getCalledOrApn() + "%"));
        addCommonPredicates(cb, root, f, predicates);
        // SMS has no start/end time — filter on createdDate from BaseEntity
        if (f.getFromTime() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), f.getFromTime()));
        if (f.getToTime()   != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), f.getToTime()));
        if (f.getCallType() != null) predicates.add(cb.equal(root.get("callType"), f.getCallType()));

        q.where(predicates.toArray(new Predicate[0]));
        List<SmsRatedCdr> rows = em.createQuery(q).getResultList();

        String[] headers = {"Calling Number", "Called Number", "Created Date",
                "Incoming Account", "Outgoing Account", "Incoming Status", "Outgoing Status",
                "Home PLMN", "Visited PLMN", "Zone Name", "Line Of Business", "Service Type",
                "Call Type", "Event Count", "Incoming Cost", "Outgoing Cost", "Rated At"};

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("SMS CDR");
            writeHeader(sheet, wb, headers);
            int rowNum = 1;
            for (SmsRatedCdr r : rows) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(str(r.getCallingNumber()));
                row.createCell(c++).setCellValue(str(r.getCalledNumber()));
                row.createCell(c++).setCellValue(str(r.getCreatedDate()));
                row.createCell(c++).setCellValue(str(r.getIncomingAccountId()));
                row.createCell(c++).setCellValue(str(r.getOutgoingAccountId()));
                row.createCell(c++).setCellValue(str(r.getIncomingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getOutgoingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getHomePlmn()));
                row.createCell(c++).setCellValue(str(r.getVisitedPlmn()));
                row.createCell(c++).setCellValue(str(r.getZoneName()));
                row.createCell(c++).setCellValue(str(r.getLineOfBusiness()));
                row.createCell(c++).setCellValue(str(r.getServiceType()));
                row.createCell(c++).setCellValue(str(r.getCallType()));
                row.createCell(c++).setCellValue(r.getEventNos() != null ? r.getEventNos().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getIncomingTotalCost() != null ? r.getIncomingTotalCost().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getOutgoingTotalCost() != null ? r.getOutgoingTotalCost().toString() : "N/A");
                row.createCell(c).setCellValue(str(r.getRatedAt()));
            }
            autoSize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ─── USAGE ───────────────────────────────────────────────────────────────

    private byte[] exportUsage(CdrFilterRequest f) throws IOException {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UsageRatedCdr> q = cb.createQuery(UsageRatedCdr.class);
        Root<UsageRatedCdr> root = q.from(UsageRatedCdr.class);

        List<Predicate> predicates = new ArrayList<>();
        if (f.getCallingOrSubscriber() != null) predicates.add(cb.like(root.get("subscriberIdentity"), "%" + f.getCallingOrSubscriber() + "%"));
        if (f.getCalledOrApn()         != null) predicates.add(cb.like(root.get("accessPointName"), "%" + f.getCalledOrApn() + "%"));
        addCommonPredicates(cb, root, f, predicates);
        if (f.getFromTime() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), f.getFromTime()));
        if (f.getToTime()   != null) predicates.add(cb.lessThanOrEqualTo(root.get("startTime"), f.getToTime()));

        q.where(predicates.toArray(new Predicate[0]));
        List<UsageRatedCdr> rows = em.createQuery(q).getResultList();

        String[] headers = {"Subscriber Identity", "Access Point Name", "Start Time", "End Time",
                "Incoming Account", "Outgoing Account", "Incoming Status", "Outgoing Status",
                "Home PLMN", "Visited PLMN", "Zone Name", "Line Of Business", "Service Type",
                "Total Usage", "Upload Usage", "Download Usage", "Measurement Unit",
                "Incoming Cost", "Outgoing Cost", "Rated At"};

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Usage CDR");
            writeHeader(sheet, wb, headers);
            int rowNum = 1;
            for (UsageRatedCdr r : rows) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;
                row.createCell(c++).setCellValue(str(r.getSubscriberIdentity()));
                row.createCell(c++).setCellValue(str(r.getAccessPointName()));
                row.createCell(c++).setCellValue(str(r.getStartTime()));
                row.createCell(c++).setCellValue(str(r.getEndTime()));
                row.createCell(c++).setCellValue(str(r.getIncomingAccountId()));
                row.createCell(c++).setCellValue(str(r.getOutgoingAccountId()));
                row.createCell(c++).setCellValue(str(r.getIncomingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getOutgoingRatingStatus()));
                row.createCell(c++).setCellValue(str(r.getHomePlmn()));
                row.createCell(c++).setCellValue(str(r.getVisitedPlmn()));
                row.createCell(c++).setCellValue(str(r.getZoneName()));
                row.createCell(c++).setCellValue(str(r.getLineOfBusiness()));
                row.createCell(c++).setCellValue(str(r.getServiceType()));
                row.createCell(c++).setCellValue(r.getTotalUsage() != null ? r.getTotalUsage().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getUploadUsage() != null ? r.getUploadUsage().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getDownloadUsage() != null ? r.getDownloadUsage().toString() : "N/A");
                row.createCell(c++).setCellValue(str(r.getMeasurementUnit()));
                row.createCell(c++).setCellValue(r.getIncomingTotalCost() != null ? r.getIncomingTotalCost().toString() : "N/A");
                row.createCell(c++).setCellValue(r.getOutgoingTotalCost() != null ? r.getOutgoingTotalCost().toString() : "N/A");
                row.createCell(c).setCellValue(str(r.getRatedAt()));
            }
            autoSize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // ─── Shared predicate builder ─────────────────────────────────────────────

    private <T> void addCommonPredicates(CriteriaBuilder cb, Root<T> root,
                                          CdrFilterRequest f, List<Predicate> predicates) {
        if (f.getIncomingAccountId()     != null) predicates.add(cb.equal(root.get("incomingAccountId"), f.getIncomingAccountId()));
        if (f.getOutgoingAccountId()     != null) predicates.add(cb.equal(root.get("outgoingAccountId"), f.getOutgoingAccountId()));
        if (f.getIncomingRatingStatus()  != null) predicates.add(cb.equal(root.get("incomingRatingStatus"), f.getIncomingRatingStatus()));
        if (f.getOutgoingRatingStatus()  != null) predicates.add(cb.equal(root.get("outgoingRatingStatus"), f.getOutgoingRatingStatus()));
        if (f.getHomePlmn()              != null) predicates.add(cb.equal(root.get("homePlmn"), f.getHomePlmn()));
        if (f.getVisitedPlmn()           != null) predicates.add(cb.equal(root.get("visitedPlmn"), f.getVisitedPlmn()));
        if (f.getZoneName()              != null) predicates.add(cb.equal(root.get("zoneName"), f.getZoneName()));
        if (f.getLineOfBusiness()        != null) predicates.add(cb.equal(root.get("lineOfBusiness"), f.getLineOfBusiness()));
        if (f.getServiceType()           != null) predicates.add(cb.equal(root.get("serviceType"), f.getServiceType()));
    }

    // ─── Excel helpers ────────────────────────────────────────────────────────

    private void writeHeader(Sheet sheet, Workbook wb, String[] headers) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) sheet.autoSizeColumn(i);
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    private String str(Object o) {
        if (o == null) return "N/A";
        String s = o.toString().trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return "N/A";
        return s;
    }
}
