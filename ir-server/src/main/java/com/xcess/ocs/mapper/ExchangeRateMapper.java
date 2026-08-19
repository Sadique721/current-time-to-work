package com.xcess.ocs.mapper;

import com.xcess.ocs.dto.ExchangeRateDTO;
import com.xcess.ocs.entity.ExchangeRate;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateMapper {

    public ExchangeRateDTO toDto(ExchangeRate entity) {
        if (entity == null) {
            return null;
        }
        ExchangeRateDTO dto = new ExchangeRateDTO();
        dto.setId(entity.getId());
        dto.setBaseCurrency(entity.getBaseCurrency());
        dto.setTargetCurrency(entity.getTargetCurrency());
        dto.setRate(entity.getRate());
        dto.setValidFrom(entity.getValidFrom());
        dto.setSource(entity.getSource());
        dto.setTargetCurrencyPrecision(entity.getTargetCurrencyPrecision());
        return dto;
    }
}
