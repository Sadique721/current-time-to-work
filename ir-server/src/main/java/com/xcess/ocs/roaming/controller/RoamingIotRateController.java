package com.xcess.ocs.roaming.controller;

import com.xcess.ocs.entity.Partner;
import com.xcess.ocs.repository.PartnerRepository;
import com.xcess.ocs.roaming.entity.RoamingIotRate;
import com.xcess.ocs.roaming.repository.RoamingIotRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roaming/iot-rates")
@RequiredArgsConstructor
public class RoamingIotRateController {

    private final RoamingIotRateRepository iotRateRepository;
    private final PartnerRepository partnerRepository;

    @GetMapping
    public List<RoamingIotRate> list(@RequestParam(required = false) Long partnerId) {
        if (partnerId != null) {
            return iotRateRepository.findAll().stream()
                    .filter(r -> !r.isDeleted() && r.getPartner().getPartnerId().equals(partnerId))
                    .toList();
        }
        return iotRateRepository.findAll().stream().filter(r -> !r.isDeleted()).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoamingIotRate> getById(@PathVariable Long id) {
        return iotRateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RoamingIotRate> create(@RequestBody RoamingIotRate rate) {
        Partner partner = partnerRepository.findById(rate.getPartner().getPartnerId())
                .orElseThrow(() -> new IllegalArgumentException("Partner not found"));
        rate.setPartner(partner);
        return ResponseEntity.ok(iotRateRepository.save(rate));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoamingIotRate> update(@PathVariable Long id, @RequestBody RoamingIotRate updated) {
        return iotRateRepository.findById(id).map(existing -> {
            existing.setDestinationPrefix(updated.getDestinationPrefix());
            existing.setServiceType(updated.getServiceType());
            existing.setRate(updated.getRate());
            existing.setCurrency(updated.getCurrency());
            existing.setEffectiveFrom(updated.getEffectiveFrom());
            existing.setEffectiveTo(updated.getEffectiveTo());
            return ResponseEntity.ok(iotRateRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return iotRateRepository.findById(id).map(r -> {
            r.setDeleted(true);
            iotRateRepository.save(r);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
