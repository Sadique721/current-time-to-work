package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.CountryDTO;
import com.xcess.ocs.entity.Country;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Country entity and CountryDTO.
 * Provides bidirectional mapping between database entity and API data transfer object.
 * 
 * @see Country
 * @see CountryDTO
 */
@Component
public class CountryMapper {

    /**
     * Converts Country entity to CountryDTO.
     * 
     * @param country the Country entity
     * @return CountryDTO or null if country is null
     */
    public CountryDTO toDto(Country country) {
        if (country == null) {
            return null;
        }

        CountryDTO countryDTO = new CountryDTO();
        countryDTO.setCountryId(country.getCountryId());
        countryDTO.setName(country.getName());
        countryDTO.setCountryCode(country.getCountryCode());
        countryDTO.setCurrencyCode(country.getCurrencyCode());
        countryDTO.setCurrencySymbol(country.getCurrencySymbol());
        countryDTO.setIsoCode(country.getIsoCode());
        return countryDTO;
    }

    /**
     * Converts CountryDTO to Country entity.
     * 
     * @param countryDTO the CountryDTO
     * @return Country entity or null if countryDTO is null
     */
    public Country toEntity(CountryDTO countryDTO) {
        if (countryDTO == null) {
            return null;
        }

        Country country = new Country();
        country.setCountryId(countryDTO.getCountryId());
        country.setName(countryDTO.getName());
        country.setCountryCode(countryDTO.getCountryCode());
        country.setCurrencyCode(countryDTO.getCurrencyCode());
        country.setCurrencySymbol(countryDTO.getCurrencySymbol());
        country.setIsoCode(countryDTO.getIsoCode());
        return country;
    }
}
