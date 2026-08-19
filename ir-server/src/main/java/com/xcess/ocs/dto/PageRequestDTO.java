package com.xcess.ocs.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "15. Payload for Get Response",
        description = "Schema to hold payload for get response"
)
public class PageRequestDTO<T> {

    @Schema(
            description = "Current page number",
            example = "1"
    )
    @Positive(message = "Page number must be positive")
    private int page;

    @Schema(
            description = "Number of records per page",
            example = "5"
    )
    @Positive(message = "Page size must be positive")
    private int pageSize;

    @Schema(
            description = "Search criteria for filtering partners",
//            oneOf = {
//                    PartnerSearchDTO.class,
//                    CountrySearchDTO.class,
//                    PrefixSearchDTO.class
//            },
            nullable = true
    )
    private T searchCriteria;
}
