package com.xcess.ocs.service;

import com.xcess.ocs.dto.PageResponseDTO;
import com.xcess.ocs.dto.PulseDTO;
import com.xcess.ocs.dto.search.PulseSearchDTO;
import com.xcess.ocs.entity.Pulse;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.exception.DuplicateNameException;
import com.xcess.ocs.exception.ForeignReferenceException;
import com.xcess.ocs.mapper.PulseMapper;
import com.xcess.ocs.repository.PulseRepository;
import com.xcess.ocs.repository.RatePackageRepository;
import com.xcess.ocs.util.PaginationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PulseService {
    @Autowired
    private PulseRepository pulseRepository;

    @Autowired
    private RatePackageRepository ratePackageRepository;

    @Autowired
    private PulseMapper pulseMapper;

    public PulseDTO createPulse(PulseDTO pulseDTO) {
        log.debug("Creating new pulse with name: {}", pulseDTO.getPulseName());
        if (pulseRepository.existsByPulseNameAndIsDeletedFalse(pulseDTO.getPulseName())) {
            log.warn("Pulse name already exists: {}", pulseDTO.getPulseName());
            throw new DuplicateNameException("Pulse", pulseDTO.getPulseName());
        }

        Pulse pulse = PulseMapper.toEntity(pulseDTO);
        Pulse savedPulse = pulseRepository.save(pulse);
        log.debug("Successfully created pulse with ID: {}", savedPulse.getPulseId());
        return PulseMapper.toDTO(savedPulse);
    }

    public List<PulseDTO> getAllPulses() {
        log.debug("Fetching all pulses");
        List<Pulse> pulses = pulseRepository.findAll();
        List<PulseDTO> pulseDTOs = pulses.stream()
                .map(PulseMapper::toDTO)
                .toList();
        log.debug("Retrieved {} pulses", pulseDTOs.size());
        return pulseDTOs;
    }

    public PageResponseDTO<PulseDTO> getPulsesInPagesByPost(Pageable pageable) {
        log.debug("Fetching pulses in pages");
        Page<Pulse> pulses = pulseRepository.findAll(pageable);
        List<PulseDTO> pulseDTOs = pulses.getContent().stream()
                .map(PulseMapper::toDTO)
                .toList();

        log.debug("Retrieved {} pulses in a page", pulseDTOs.size());
        return PaginationUtils.buildGetResponseDTO(pulseDTOs, pulses);
    }

    public PageResponseDTO<PulseDTO> searchPulses(PulseSearchDTO searchDTO, Pageable pageable) {
        log.debug("Searching pulses with criteria: {}", searchDTO);

        Page<Pulse> pulsePage = pulseRepository.searchPulses(
                searchDTO.getSearchTerm(),
                searchDTO.getServiceType(),
                pageable
        );

        List<PulseDTO> pulses = pulsePage.getContent().stream()
                .map(PulseMapper::toDTO)
                .collect(Collectors.toList());

        log.debug("Found {} pulses matching criteria", pulses.size());
        return PaginationUtils.buildGetResponseDTO(pulses, pulsePage);
    }

    public PulseDTO getPulseById(Long id) {
        log.debug("Fetching pulse with ID: {}", id);
        Pulse pulse = pulseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pulse not found with ID: {}", id);
                    return new RuntimeException("Pulse not found");
                });
        return PulseMapper.toDTO(pulse);
    }

    public PulseDTO updatePulse(Long id, PulseDTO pulseDTO) {
        log.debug("Updating pulse with ID: {}", id);
        Pulse pulse = pulseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Pulse not found with ID: {}", id);
                    return new RuntimeException("Pulse not found");
                });

        if (!pulse.getPulseName().equalsIgnoreCase(pulseDTO.getPulseName()) &&
            pulseRepository.existsByPulseNameAndIsDeletedFalse(pulseDTO.getPulseName())) {
            log.warn("Pulse name already exists: {}", pulseDTO.getPulseName());
            throw new DuplicateNameException("Pulse", pulseDTO.getPulseName());
        }

        pulse.setPulseName(pulseDTO.getPulseName());
        pulse.setServiceType(pulseDTO.getServiceType());
        pulse.setUnit(pulseDTO.getUnit());
        pulse.setNoOfUnits(pulseDTO.getNoOfUnits());

        Pulse updatedPulse = pulseRepository.save(pulse);
        log.debug("Successfully updated pulse with ID: {}", id);
        return PulseMapper.toDTO(updatedPulse);
    }

    public void deletePulse(Long id) {
        log.debug("Deleting pulse with ID: {}", id);
        if (!pulseRepository.existsById(id)) {
            log.warn("Attempt to delete non-existent pulse with ID: {}", id);
            throw new RuntimeException("Pulse not found");
        }
        // First, check if there is any active rate package using this pulse
        boolean isReferenced = ratePackageRepository.existsByPulse_PulseIdAndIsDeletedFalse(id);
        if (isReferenced) {
            log.warn("Attempt to soft delete pulse with ID: {} that is referenced by an active rate package", id);
            throw new ForeignReferenceException("Pulse cannot be soft deleted because it is referenced by an active rate package.");
        }
        pulseRepository.deleteById(id);
        log.debug("Successfully deleted pulse with ID: {}", id);
    }

    public List<Map<String, Object>> getPulsesByServiceType(String serviceType){
        log.debug("Fetching pulses by service type");

        List<Pulse> pulses;
        if("voice".equalsIgnoreCase(serviceType)) {
            pulses = pulseRepository.findByServiceTypeAndIsDeletedFalse(ServiceType.VOICE);
            log.debug("Retrieved {} pulses for service type voice", pulses.size());
        } else if("sms".equalsIgnoreCase(serviceType)) {
            pulses = pulseRepository.findByServiceTypeAndIsDeletedFalse(ServiceType.SMS);
            log.debug("Retrieved {} pulses for service type sms", pulses.size());
        } else if("usage".equalsIgnoreCase(serviceType)){
            pulses = pulseRepository.findByServiceTypeAndIsDeletedFalse(ServiceType.USAGE);
            log.debug("Retrieved {} pulses for service type usage", pulses.size());
        } else{
            throw new IllegalArgumentException("Invalid Service Type");
        }

        return pulses.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getPulseId());
                    map.put("pulseName", p.getPulseName());
                    return map;
                }).collect(Collectors.toList());
    }
}
