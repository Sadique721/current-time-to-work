package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.dto.ErrorResponseDTO;
import com.xcess.ocs.dto.PageRequestDTO;
import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.dto.TapFileRecordDTO;
import com.xcess.ocs.roaming.dto.TapFileSearchDTO;
import com.xcess.ocs.roaming.entity.TapFileRecord;
import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileType;
import com.xcess.ocs.roaming.repository.RoamingCdrRepository;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import com.xcess.ocs.roaming.dto.TapFileCdrDetailsResponseDTO;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.roaming.entity.RoamingCdr;
import com.xcess.ocs.roaming.service.RoamingInvoiceService;
import com.xcess.ocs.roaming.service.TapFileProcessingService;
import com.xcess.ocs.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * REST controller for TAP file operations: upload, listing, download, and invoice generation.
 *
 * <p>TAP files are GSMA-standard ASN.1 BER-encoded roaming data exchange files.
 * This controller handles both TAP IN (inbound from partner) and TAP OUT (outbound to partner)
 * file lifecycle management.
 *
 * <p>Base path: {@code /api/roaming/tap}
 */
@Slf4j
@RestController
@RequestMapping("api/roaming/tap")
@RequiredArgsConstructor
@Tag(name = "TAP File Management",
     description = "Upload, list, download, and trigger invoice generation for GSMA TAP roaming files. " +
                   "Supports both TAP IN (inbound from partner) and TAP OUT (outbound to partner) file types.")
public class TapFileController {

    private final TapFileRecordRepository tapFileRecordRepository;
    private final PartnerRepository partnerRepository;
    private final TapFileProcessingService processingService;
    private final RoamingInvoiceService invoiceService;
    private final RoamingCdrRepository roamingCdrRepository;
    private final com.xcess.ocs.repository.VoiceRatedCdrRepository voiceRatedCdrRepository;
    private final com.xcess.ocs.repository.SmsRatedCdrRepository smsRatedCdrRepository;
    private final com.xcess.ocs.repository.UsageRatedCdrRepository usageRatedCdrRepository;
    private final com.xcess.ocs.roaming.scheduler.TapOutScheduler tapOutScheduler;

    /**
     * Accepts a TAP file upload, stores it to disk, creates a {@code TapFileRecord},
     * and immediately triggers asynchronous processing.
     *
     * @param file      the multipart TAP file
     * @param partnerId the partner this file belongs to
     * @param fileType  TAP_IN or TAP_OUT
     * @return HTTP 202 Accepted with the accepted filename
     */
    @Operation(
        summary = "Upload a TAP file",
        description = "Accepts a GSMA TAP ASN.1 file upload for a specific partner. " +
                      "The file is stored to disk, a TapFileRecord is created with RECEIVED status, " +
                      "and processing is triggered immediately."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "File accepted and processing triggered"),
        @ApiResponse(responseCode = "400", description = "Partner not found or invalid file type",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @Parameter(description = "The TAP ASN.1 binary file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "ID of the partner this file belongs to", example = "99", required = true)
            @RequestParam("partnerId") Long partnerId,
            @Parameter(description = "File type: TAP_IN for inbound files, TAP_OUT for outbound files",
                       example = "TAP_IN", required = true)
            @RequestParam("fileType") TapFileType fileType) throws IOException {
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerId));

        if (com.xcess.ocs.entity.LineOfBusiness.INTERCONNECT.equals(partner.getLineOfBusiness())) {
            throw new IllegalArgumentException("TAP files are only supported for ROAMING line of business");
        }

        String fileName = file.getOriginalFilename();
        Path storagePath = Paths.get("tap-files", fileName);
        Files.createDirectories(storagePath.getParent());
        Files.write(storagePath, file.getBytes());

        TapFileRecord record = new TapFileRecord();
        record.setFileName(fileName);
        record.setFileType(fileType);
        record.setStatus(TapFileStatus.RECEIVED);
        record.setPartner(partner);
        record.setFilePath(storagePath.toString());
        record.setSenderTadig(fileName.length() >= 7 ? fileName.substring(2, 7) : null);
        record.setRecipientTadig(fileName.length() >= 12 ? fileName.substring(7, 12) : null);
        record.setFileSequenceNo(parseSequenceNo(fileName));
        tapFileRecordRepository.save(record);

        processingService.process(record);
        return ResponseEntity.accepted().body("TAP file accepted: " + fileName);
    }

    /**
     * Returns a paginated, filterable list of TAP file records.
     *
     * @param pageRequestDTO pagination parameters and optional search filters
     * @return paginated list of {@link TapFileRecordDTO}
     */
    @Operation(
        summary = "List TAP file records (paginated)",
        description = "Returns a paginated list of TAP file records with optional filtering by " +
                      "status, file type, sender/recipient TADIG code, or partner ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of TAP file records returned successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid pagination parameters",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Pagination and search criteria",
        content = @Content(
            mediaType = "application/json",
            examples = @ExampleObject(
                name = "Filter by partner and status",
                value = """
                    {
                      "page": 1,
                      "pageSize": 10,
                      "searchCriteria": {
                        "partnerId": 99,
                        "status": "RECEIVED",
                        "fileType": "TAP_IN"
                      }
                    }"""
            )
        )
    )
    @PostMapping("/files/paginated")
    public ResponseEntity<PageResponseDTO<TapFileRecordDTO>> getFilesInPage(
            @Valid @RequestBody PageRequestDTO<TapFileSearchDTO> pageRequestDTO) {
        log.info("REST request to get TAP file records in page");

        PageRequest pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
        Specification<TapFileRecord> spec = buildSpec(pageRequestDTO.getSearchCriteria());
        Page<TapFileRecord> page = tapFileRecordRepository.findAll(spec, pageable);

        List<TapFileRecordDTO> content = page.getContent().stream().map(this::toDTO).toList();
        return ResponseEntity.ok(PaginationUtils.buildGetResponseDTO(content, page));
    }

    /**
     * Returns a single TAP file record by its primary key.
     *
     * @param id the TAP file record ID
     * @return the matching {@link TapFileRecordDTO}, or 404 if not found
     */
    @Operation(
        summary = "Get a TAP file record by ID",
        description = "Returns the full TAP file record including status, TADIG codes, charge totals, and error reason."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "TAP file record returned successfully"),
        @ApiResponse(responseCode = "404", description = "TAP file record not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/files/{id}")
    public ResponseEntity<TapFileRecordDTO> getById(
            @Parameter(description = "Unique ID of the TAP file record", example = "1", required = true)
            @PathVariable Long id) {
        return tapFileRecordRepository.findById(id)
                .map(r -> ResponseEntity.ok(toDTO(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns a specific TAP file's CDRs filtered by service type.
     *
     * @param id the TAP file record ID
     * @param serviceType the service type filter (e.g. VOICE, SMS, USAGE)
     * @return the TAP file details and its corresponding CDRs
     */
    @Operation(
        summary = "Get TAP file CDRs by service type",
        description = "Returns TAP file details and its associated CDRs filtered by service type."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "TAP file CDRs returned successfully"),
        @ApiResponse(responseCode = "404", description = "TAP file record not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/files/{id}/cdrs")
    public ResponseEntity<TapFileCdrDetailsResponseDTO> getTapFileCdrs(
            @Parameter(description = "Unique ID of the TAP file record", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(description = "Service type to filter CDRs", example = "VOICE", required = false)
            @RequestParam(required = false) ServiceType serviceType) {
        
        TapFileRecord record = tapFileRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }

        TapFileCdrDetailsResponseDTO responseDTO = new TapFileCdrDetailsResponseDTO();
        responseDTO.setSenderTadig(record.getSenderTadig());
        responseDTO.setRecipientTadig(record.getRecipientTadig());
        responseDTO.setTapDecimalPlaces(record.getTapDecimalPlaces());
        responseDTO.setFileSequenceNo(record.getFileSequenceNo());

        List<TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO> cdrDetails = new java.util.ArrayList<>();

        if (record.getFileType() == TapFileType.TAP_IN) {
            List<RoamingCdr> cdrs;
            if (serviceType != null) {
                cdrs = roamingCdrRepository.findByTapFileRecord_TapFileIdAndServiceType(id, serviceType);
            } else {
                cdrs = roamingCdrRepository.findByTapFileRecord_TapFileId(id);
            }

            cdrDetails.addAll(cdrs.stream().map(cdr -> {
                TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO dto = new TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO();
                dto.setCallType(cdr.getCallType());
                dto.setCallingNumber(cdr.getCallingNumber());
                dto.setCalledNumber(cdr.getCalledNumber());
                dto.setCallStartTime(cdr.getCallStartTime());
                dto.setCallDurationSec(cdr.getCallDurationSec());
                dto.setHomePlmn(cdr.getHomePlmn());
                dto.setVisitedPlmn(cdr.getVisitedPlmn());
                dto.setCurrency(cdr.getCurrency());
                dto.setRatePackageName(cdr.getRatePackageName());
                dto.setAppliedRate(cdr.getAppliedRate());
                dto.setTotalUsage(cdr.getTotalUsage());
                dto.setEventNos(cdr.getEventNos());
                return dto;
            }).toList());
        } else {
            // TAP_OUT
            if (serviceType == null || serviceType == ServiceType.VOICE) {
                List<com.xcess.ocs.entity.VoiceRatedCdr> voiceCdrs = voiceRatedCdrRepository.findRatedByTapFileId(id);
                cdrDetails.addAll(voiceCdrs.stream().map(cdr -> {
                    TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO dto = new TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO();
                    dto.setCallType(cdr.getCallType());
                    dto.setCallingNumber(cdr.getCallingNumber());
                    dto.setCalledNumber(cdr.getCalledNumber());
                    // getStartTime() now returns LocalDateTime directly
                    dto.setCallStartTime(cdr.getStartTime());
                    dto.setCallDurationSec(cdr.getDurationSeconds() != null ? cdr.getDurationSeconds().intValue() : null);
                    dto.setHomePlmn(cdr.getHomePlmn());
                    dto.setVisitedPlmn(cdr.getVisitedPlmn());
                    dto.setCurrency(record.getPartner() != null ? record.getPartner().getBillingCurrency() : null);
                    dto.setRatePackageName(cdr.getOutgoingRatePackageName());
                    dto.setAppliedRate(cdr.getOutgoingAppliedRate());
                    dto.setTotalUsage(null);
                    dto.setEventNos(cdr.getMessageCount());
                    return dto;
                }).toList());
            }

            if (serviceType == null || serviceType == ServiceType.SMS) {
                List<com.xcess.ocs.entity.SmsRatedCdr> smsCdrs = smsRatedCdrRepository.findRatedByTapFileId(id);
                cdrDetails.addAll(smsCdrs.stream().map(cdr -> {
                    TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO dto = new TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO();
                    dto.setCallType(cdr.getCallType());
                    dto.setCallingNumber(cdr.getCallingNumber());
                    dto.setCalledNumber(cdr.getCalledNumber());
                    // getStartTime() now returns LocalDateTime directly
                    dto.setCallStartTime(cdr.getStartTime());
                    dto.setCallDurationSec(0);
                    dto.setHomePlmn(cdr.getHomePlmn());
                    dto.setVisitedPlmn(cdr.getVisitedPlmn());
                    dto.setCurrency(record.getPartner() != null ? record.getPartner().getBillingCurrency() : null);
                    dto.setRatePackageName(cdr.getOutgoingRatePackageName());
                    dto.setAppliedRate(cdr.getOutgoingAppliedRate());
                    dto.setTotalUsage(null);
                    dto.setEventNos(cdr.getEventNos());
                    return dto;
                }).toList());
            }

            if (serviceType == null || serviceType == ServiceType.USAGE) {
                List<com.xcess.ocs.entity.UsageRatedCdr> usageCdrs = usageRatedCdrRepository.findRatedByTapFileId(id);
                cdrDetails.addAll(usageCdrs.stream().map(cdr -> {
                    TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO dto = new TapFileCdrDetailsResponseDTO.RoamingCdrDetailDTO();
                    dto.setCallType(null); // USAGE doesn't map cleanly to CallType usually
                    dto.setCallingNumber(cdr.getCallingNumber());
                    dto.setCalledNumber(cdr.getCalledNumber());
                    // getStartTime() now returns LocalDateTime directly
                    dto.setCallStartTime(cdr.getStartTime());
                    dto.setCallDurationSec(0);
                    dto.setHomePlmn(cdr.getHomePlmn());
                    dto.setVisitedPlmn(cdr.getVisitedPlmn());
                    dto.setCurrency(record.getPartner() != null ? record.getPartner().getBillingCurrency() : null);
                    dto.setRatePackageName(cdr.getOutgoingRatePackageName());
                    dto.setAppliedRate(cdr.getOutgoingAppliedRate());
                    dto.setTotalUsage(cdr.getTotalUsage());
                    dto.setEventNos(0);
                    return dto;
                }).toList());
            }
        }

        responseDTO.setCdrs(cdrDetails);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Streams the raw TAP ASN.1 binary file as an octet-stream download.
     *
     * @param id the TAP file record ID
     * @return the binary file as an attachment, or 404 if the record or file is missing
     */
    @Operation(
        summary = "Download a TAP file",
        description = "Streams the raw GSMA TAP ASN.1 binary file as an octet-stream attachment."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File streamed successfully",
                     content = @Content(mediaType = "application/octet-stream")),
        @ApiResponse(responseCode = "404", description = "File record not found or file missing from disk",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Unique ID of the TAP file record", example = "1", required = true)
            @PathVariable Long id) {
        log.info("Downloading TAP file for record: {}", id);
        TapFileRecord record = tapFileRecordRepository.findById(id).orElse(null);
        if (record == null || record.getFilePath() == null) return ResponseEntity.notFound().build();

        Resource resource = new FileSystemResource(record.getFilePath());
        if (!resource.exists()) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", record.getFileName());
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    /**
     * Triggers roaming invoice generation for a processed TAP file.
     *
     * @param id the TAP file record ID
     * @return HTTP 200 with confirmation message, or 404 if not found
     */
    @Operation(
        summary = "Generate invoice for a TAP file",
        description = "Triggers roaming invoice generation for a TAP file that has been decoded and rated. " +
                      "The file must be in RATED or DECODED status for invoice generation to succeed."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice generation triggered successfully"),
        @ApiResponse(responseCode = "404", description = "TAP file record not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/files/{id}/invoice")
    public ResponseEntity<String> generateInvoice(
            @Parameter(description = "Unique ID of the TAP file record", example = "1", required = true)
            @PathVariable Long id) {
        return tapFileRecordRepository.findById(id).map(record -> {
            invoiceService.generateInvoice(record);
            return ResponseEntity.ok("Invoice generation triggered for TAP file: " + record.getFileName());
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Triggers the daily TAP OUT scheduler manually.
     *
     * @return HTTP 200 with confirmation message
     */
    @Operation(
        summary = "Trigger TAP OUT Scheduler",
        description = "Manually triggers the daily TAP OUT scheduler which generates TAP OUT files for all eligible roaming partners."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "TAP OUT scheduler triggered successfully")
    })
    @PostMapping("/trigger-tap-out-scheduler")
    public ResponseEntity<String> triggerTapOutScheduler() {
        log.info("Manual trigger for TAP OUT scheduler received.");
        tapOutScheduler.generateDailyTapOutFiles();
        return ResponseEntity.ok("TAP OUT scheduler triggered successfully.");
    }

    private Specification<TapFileRecord> buildSpec(TapFileSearchDTO search) {
        return (root, query, cb) -> {
            if (search == null) return cb.conjunction();
            var predicates = new java.util.ArrayList<>();
            if (search.getStatus() != null)
                predicates.add(cb.equal(root.get("status"), search.getStatus()));
            if (search.getFileType() != null)
                predicates.add(cb.equal(root.get("fileType"), search.getFileType()));
            if (search.getSenderTadig() != null)
                predicates.add(cb.like(cb.lower(root.get("senderTadig")), "%" + search.getSenderTadig().toLowerCase() + "%"));
            if (search.getRecipientTadig() != null)
                predicates.add(cb.like(cb.lower(root.get("recipientTadig")), "%" + search.getRecipientTadig().toLowerCase() + "%"));
            if (search.getPartnerId() != null)
                predicates.add(cb.equal(root.get("partner").get("partnerId"), search.getPartnerId()));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private TapFileRecordDTO toDTO(TapFileRecord r) {
        TapFileRecordDTO dto = new TapFileRecordDTO();
        dto.setTapFileId(r.getTapFileId());
        dto.setFileName(r.getFileName());
        dto.setSenderTadig(r.getSenderTadig());
        dto.setRecipientTadig(r.getRecipientTadig());
        dto.setFileSequenceNo(r.getFileSequenceNo());
        dto.setFileType(r.getFileType());
        dto.setTapVersion(r.getTapVersion());
        dto.setStatus(r.getStatus());
        dto.setTotalRecords(r.getTotalRecords());
        dto.setTotalCharge(r.getTotalCharge());
        dto.setTapDecimalPlaces(r.getTapDecimalPlaces());
        dto.setLocalCurrency(r.getLocalCurrency());
        dto.setErrorReason(r.getErrorReason());
        dto.setProcessedAt(r.getProcessedAt());
        if (r.getPartner() != null) {
            dto.setPartnerId(r.getPartner().getPartnerId());
            dto.setPartnerName(r.getPartner().getPartnerName());
        }
        return dto;
    }

    private Integer parseSequenceNo(String fileName) {
        try { return Integer.parseInt(fileName.replaceAll("\\D", "").substring(0, 5)); }
        catch (Exception e) { return null; }
    }
}