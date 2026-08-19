package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Exchange Rate",
        description = "Schema to hold exchange rate details"
)
public class ExchangeRateDTO {

    @Schema(description = "ID of the exchange rate record", example = "1")
    private Long id;

    @Schema(description = "Base currency (ISO 4217)", example = "INR")
    private String baseCurrency;

    @Schema(description = "Target currency (ISO 4217)", example = "USD")
    private String targetCurrency;

    @Schema(description = "Conversion rate", example = "0.011700")
    private BigDecimal rate;

    @Schema(description = "Date from which this rate is valid")
    private LocalDate validFrom;

    @Schema(description = "Source of the rate", example = "FRANKFURTER")
    private String source;

    @Schema(description = "Decimal precision of the target currency", example = "2")
    private Integer targetCurrencyPrecision;
}
