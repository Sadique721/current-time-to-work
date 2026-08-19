package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Schema(
        name = "Exchange Rate SearchDTO",
        description = "Schema to hold exchange rate search details"
)
public class ExchangeRateSearchDTO {

    @Schema(
            description = "Search term for base currency, target currency, or source",
            example = "USD",
            nullable = true
    )
    private String searchTerm;

    @Schema(
            description = "Filter by exact valid-from date",
            example = "2025-01-15",
            nullable = true
    )
    private LocalDate validFrom;
}
