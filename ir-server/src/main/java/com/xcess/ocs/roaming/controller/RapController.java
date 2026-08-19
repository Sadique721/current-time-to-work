package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.roaming.entity.RapRecord;
import com.xcess.ocs.roaming.entity.RapStatus;
import com.xcess.ocs.roaming.repository.RapRecordRepository;
import com.xcess.ocs.roaming.repository.TapFileRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/roaming/rap")
@RequiredArgsConstructor
public class RapController {

    private final RapRecordRepository rapRecordRepository;
    private final TapFileRecordRepository tapFileRecordRepository;

    @GetMapping
    public List<RapRecord> list(@RequestParam(required = false) RapStatus status) {
        if (status != null) return rapRecordRepository.findByStatus(status);
        return rapRecordRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RapRecord> getById(@PathVariable Long id) {
        return rapRecordRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Raise a RAP dispute (RAP_IN received or RAP_OUT we initiate) */
    @PostMapping
    public ResponseEntity<RapRecord> create(@RequestBody RapRecord rap) {
        if (rap.getTapFileRecord() != null && rap.getTapFileRecord().getTapFileId() != null) {
            tapFileRecordRepository.findById(rap.getTapFileRecord().getTapFileId())
                    .ifPresent(rap::setTapFileRecord);
        }
        return ResponseEntity.ok(rapRecordRepository.save(rap));
    }

    /** Update RAP status (ACKNOWLEDGED, RESOLVED, REJECTED) */
    @PatchMapping("/{id}/status")
    public ResponseEntity<RapRecord> updateStatus(@PathVariable Long id,
                                                   @RequestBody RapStatusUpdateRequest req) {
        return rapRecordRepository.findById(id).map(rap -> {
            rap.setStatus(req.status());
            if (req.resolutionNotes() != null) rap.setResolutionNotes(req.resolutionNotes());
            if (req.status() == RapStatus.RESOLVED || req.status() == RapStatus.REJECTED) {
                rap.setResolvedAt(LocalDateTime.now());
            }
            return ResponseEntity.ok(rapRecordRepository.save(rap));
        }).orElse(ResponseEntity.notFound().build());
    }

    record RapStatusUpdateRequest(RapStatus status, String resolutionNotes) {}
}
