package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "6. Country Response",
        description = "Country Response DTO with success or failure message"
)
public class CountryResponseDTO {
    @Schema(description = "Boolean to show Success or Failure", example = "true")
    private boolean success;
    @Schema(description = "Success Message for assurance", example = "Country created successfully")
    private String message;
    @Schema(description = "Data of the country")
    private CountryDTO data;
}