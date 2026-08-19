package com.xcess.ocs.mapper;

import com.xcess.ocs.entity.Country;
import com.xcess.ocs.entity.RateDetails;
import com.xcess.ocs.dto.RateDetailDTO;
import com.xcess.ocs.entity.RatePackage;
import org.springframework.stereotype.Component;

@Component
public class RateDetailMapper {
        public static RateDetailDTO toDTO(RateDetails entity) {
                return entity == null ? null
                        : RateDetailDTO.builder()
                        .rateDetailsId(entity.getRateDetailsId())
                        .destinationPrefix(entity.getDestinationPrefix())
                        .destinationPrefixName(entity.getDestinationPrefixName())
                        .sourcePrefix(entity.getSourcePrefix())
                        .sourcePrefixName(entity.getSourcePrefixName())
                        .rate(entity.getRate())
                        .startTime(entity.getStartTime())
                        .endTime(entity.getEndTime())
                        .ratePackageId(entity.getRatePackage() != null ? entity.getRatePackage().getRatePackageId() : null)
                        .currentVersion(entity.getCurrentVersion())
                        .sourceCountryCode(entity.getSourceCountryCode())
                        .sourceCountryName(entity.getSourceCountryName())
                        .destinationCountryCode(entity.getDestinationCountryCode())
                        .sourceCountryId(entity.getSourceCountryId() != null ? entity.getSourceCountryId().getCountryId() : null)
                        .destinationCountryId(entity.getDestinationCountryId() != null ? entity.getDestinationCountryId().getCountryId() : null)
                        .destinationCountryName(entity.getDestinationCountryName())
                        .zoneName(entity.getZoneName())
                        .build();
        }

        public static RateDetails toEntity(RateDetailDTO dto, RatePackage ratePackage) {
                return dto == null ? null
                        : RateDetails.builder()
                        .rateDetailsId(dto.getRateDetailsId())
                        .destinationPrefix(dto.getDestinationPrefix())
                        .destinationPrefixName(dto.getDestinationPrefixName())
                        .sourcePrefix(dto.getSourcePrefix())
                        .sourcePrefixName(dto.getSourcePrefixName())
                        .rate(dto.getRate())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .ratePackage(ratePackage)
                        .currentVersion(dto.getCurrentVersion())
                        .sourceCountryCode(dto.getSourceCountryCode())
                        .sourceCountryId(dto.getSourceCountryId() != null ? new Country(dto.getSourceCountryId()) : null)
                        .destinationCountryId(dto.getDestinationCountryId() != null ? new Country(dto.getDestinationCountryId()) : null)
                        .sourceCountryName(dto.getSourceCountryName())
                        .destinationCountryCode(dto.getDestinationCountryCode())
                        .destinationCountryName(dto.getDestinationCountryName())
                        .zoneName(dto.getZoneName())
                        .build();
        }


}