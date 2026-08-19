package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "Account SearchDTO",
        description = "Schema to hold Account search details"
)
public class AccountSearchDTO {
    @Schema(
            description = "Search term for global search",
            example = "John",
            nullable = true
    )
    private String searchTerm;

    @Schema(
            description = "Account type to filter",
            example = "VENDOR",
            allowableValues = {"CUSTOMER", "VENDOR"},
            nullable = true
    )
    private String accountType;
}