package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ServiceType;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "Data Transfer Object for Source Configuration")
public class SourceConfigurationDTO {


    @Schema(description = "Unique identifier for the Source Configuration", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long sourceId;

    @NotNull(message = "Source name is required")
    @Schema(description = "Name of the CDR source", example = "Source1", required = true)
    private String sourceName;

    @NotNull(message = "Topic name is required")
    @Schema(description = "Kafka topic name for this source", example = "Topic1", required = true)
    private String topicName;

    @Schema(description = "Service type for this source", example = "VOICE", allowableValues = {"VOICE", "SMS", "USAGE"})
    private ServiceType serviceType = ServiceType.VOICE;

    @Pattern(regexp = "enabled|disabled", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Status must be 'enabled' or 'disabled'")
    @Schema(description = "Status of the source configuration (enabled/disabled)", example = "enabled", allowableValues = {"enabled", "disabled"})
    private String status;
}
