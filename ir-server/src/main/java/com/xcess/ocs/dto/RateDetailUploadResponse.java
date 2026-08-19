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
@Schema(name = "10. Rate Detail CSV Upload", description = "Schema to upload CSV having rate details")
public class RateDetailUploadResponse {
    @Schema(description = "Number of records processed", example = "10")
    private int recordsProcessed;

    @Schema(description = "Success message for assurance", example = "Successfully processed 10 records")
    private String message;
}