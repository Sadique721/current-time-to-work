package com.xcess.ocs.service;

import com.xcess.ocs.dto.RateCardRequestDTO;
import com.xcess.ocs.dto.RateCardResponseDTO;
import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.entity.RatePackage;
import com.xcess.ocs.entity.RatePackageType;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.exception.ResourceNotFoundException;
import com.xcess.ocs.repository.RatePackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateCardService {
    private final RatePackageRepository ratePackageRepository;

    public RateCardResponseDTO getRateCard(RateCardRequestDTO requestDTO) {
        // Log the request details
        log.info("Fetching RateCard for RatePackageId: {}, ServiceType: {}, SourceCountryCode: {}, DestinationCountryCode: {}",
                requestDTO.getRatePackageId(), requestDTO.getServiceType(),
                requestDTO.getSourceCountryCode(), requestDTO.getDestinationCountryCode());

        Long ratePackageId = requestDTO.getRatePackageId();
        if (ratePackageId == null) {
            throw new IllegalArgumentException("RatePackageId must be provided");
        }

        ServiceType serviceType = requestDTO.getServiceType();
        String sourceCode = requestDTO.getSourceCountryCode();
        String destinationCode = requestDTO.getDestinationCountryCode();

        if (serviceType == null) {
            throw new IllegalArgumentException("ServiceType must be provided");
        }

        if ((sourceCode == null || sourceCode.isBlank()) &&
                (destinationCode == null || destinationCode.isBlank())) {
            throw new IllegalArgumentException("At least one of sourceCountryCode or destinationCountryCode must be provided.");
        }

        // Fetch rate package directly by ID
        RatePackage ratePackage = ratePackageRepository.findById(ratePackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Rate Package not found with ID: " + ratePackageId));

        //  Filter the rate details
        List<RateDetails> filteredDetails = ratePackage.getRateDetails().stream()
                .filter(detail -> {
                    boolean matchesSource = (sourceCode == null || sourceCode.isBlank())
                            || sourceCode.equalsIgnoreCase(detail.getSourceCountryCode());
                    boolean matchesDestination = (destinationCode == null || destinationCode.isBlank())
                            || destinationCode.equalsIgnoreCase(detail.getDestinationCountryCode());

                    if (sourceCode != null && !sourceCode.isBlank() &&
                            (destinationCode == null || destinationCode.isBlank())) {
                        // Only source provided → match source only
                        return matchesSource;
                    } else if (sourceCode != null && !sourceCode.isBlank() &&
                            destinationCode != null && !destinationCode.isBlank()) {
                        // Both provided → match both
                        return matchesSource && matchesDestination;
                    } else if ((sourceCode == null || sourceCode.isBlank()) &&
                            destinationCode != null && !destinationCode.isBlank()) {
                        //  Only destination provided → match destination only
                        return matchesDestination;
                    } else {
                        return false; // Shouldn't reach here due to earlier validation
                    }
                })
                .toList();

        // Create and return the response DTO
        return RateCardResponseDTO.fromRatePackageWithFilteredDetails(ratePackage, filteredDetails);
    }

}
