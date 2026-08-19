package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Data Transfer Object for Source CDR Configuration")
public class SourceCdrConfigurationDTO {


    @Schema(description = "Unique identifier for the Source CDR Configuration", example = "1")
    private Long id;

    @NotNull
    @Schema(description = "ID of the associated Source Configuration", accessMode = Schema.AccessMode.READ_ONLY)
    private Long sourceId;

    @NotNull
    @Schema(description = "Name of the field in the CDR", accessMode = Schema.AccessMode.READ_ONLY)
    private String fieldName;

    @NotNull
    @Min(1)
    @Schema(description = "Sequence number for field processing order (starts at 1)", example = "1", minimum = "1")
    private Integer sequence;

}
