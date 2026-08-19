package com.xcess.ocs.controller;

import com.xcess.ocs.dto.search.CdrFilterRequest;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.RatingStatus;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.CallType;
import com.xcess.ocs.service.CdrExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/cdr")
@Tag(name = "CDR Export", description = "Filter and export CDR data as Excel")
public class CdrExportController {

    private final CdrExportService cdrExportService;

    public CdrExportController(CdrExportService cdrExportService) {
        this.cdrExportService = cdrExportService;
    }

    // ─── Main Export API ──────────────────────────────────────────────────────

    @PostMapping("/export")
    @Operation(summary = "Export filtered CDR data as Excel",
               description = "serviceType in request body determines the table: VOICE, SMS, or USAGE. " +
                             "SMS filters on createdDate instead of startTime. " +
                             "USAGE uses subscriberIdentity/accessPointName instead of callingNumber/calledNumber.")
    public ResponseEntity<byte[]> exportCdr(@RequestBody CdrFilterRequest filter) throws IOException {
        byte[] excelBytes = cdrExportService.exportToExcel(filter);
        String prefix = filter.getServiceType() != null ? filter.getServiceType().name().toLowerCase() : "cdr";
        String filename = prefix + "_cdr_export_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    // ─── Dropdown APIs ────────────────────────────────────────────────────────

    @GetMapping("/dropdown/zone-names")
    @Operation(summary = "Get distinct zone names across all CDR tables")
    public ResponseEntity<List<String>> getZoneNames() {
        return ResponseEntity.ok(cdrExportService.getDistinctZoneNames());
    }

    @GetMapping("/dropdown/home-plmn")
    @Operation(summary = "Get distinct home PLMN values across all CDR tables")
    public ResponseEntity<List<String>> getHomePlmn() {
        return ResponseEntity.ok(cdrExportService.getDistinctHomePlmn());
    }

    @GetMapping("/dropdown/visited-plmn")
    @Operation(summary = "Get distinct visited PLMN values across all CDR tables")
    public ResponseEntity<List<String>> getVisitedPlmn() {
        return ResponseEntity.ok(cdrExportService.getDistinctVisitedPlmn());
    }

    @GetMapping("/dropdown/incoming-account-ids")
    @Operation(summary = "Get distinct incoming account IDs across all CDR tables")
    public ResponseEntity<List<String>> getIncomingAccountIds() {
        return ResponseEntity.ok(cdrExportService.getDistinctIncomingAccountIds());
    }

    @GetMapping("/dropdown/outgoing-account-ids")
    @Operation(summary = "Get distinct outgoing account IDs across all CDR tables")
    public ResponseEntity<List<String>> getOutgoingAccountIds() {
        return ResponseEntity.ok(cdrExportService.getDistinctOutgoingAccountIds());
    }

    @GetMapping("/dropdown/rating-statuses")
    @Operation(summary = "Get all possible rating status values")
    public ResponseEntity<List<RatingStatus>> getRatingStatuses() {
        return ResponseEntity.ok(Arrays.asList(RatingStatus.values()));
    }

    @GetMapping("/dropdown/service-types")
    @Operation(summary = "Get all possible service type values")
    public ResponseEntity<List<ServiceType>> getServiceTypes() {
        return ResponseEntity.ok(Arrays.asList(ServiceType.values()));
    }

    @GetMapping("/dropdown/call-types")
    @Operation(summary = "Get all possible call type values")
    public ResponseEntity<List<CallType>> getCallTypes() {
        return ResponseEntity.ok(Arrays.asList(CallType.values()));
    }

    @GetMapping("/dropdown/line-of-business")
    @Operation(summary = "Get all possible line of business values")
    public ResponseEntity<List<LineOfBusiness>> getLineOfBusinessValues() {
        return ResponseEntity.ok(Arrays.asList(LineOfBusiness.values()));
    }
}
