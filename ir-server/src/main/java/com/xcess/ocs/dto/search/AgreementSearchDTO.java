package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "AgreementSearchDTO", description = "Search criteria for filtering agreements")
public class AgreementSearchDTO {

    @Schema(description = "Search term to filter by agreement code", example = "AGR", nullable = true)
    private String searchTerm;
}
