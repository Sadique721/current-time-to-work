package com.xcess.ocs.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(
        name = "Source Configuration SearchDTO",
        description = "Schema to hold Source Configuration search details"
)
public class SourceConfigurationSearchDTO {
    @Schema(
            description = "Search term for topicName and sourceName",
            example = "topic1",
            nullable = true
    )
    private String searchTerm;

    @Schema(
            description = "Status to filter",
            example = "enabled",
            allowableValues = {"enabled", "disabled"},
            nullable = true
    )
    private String status;
}