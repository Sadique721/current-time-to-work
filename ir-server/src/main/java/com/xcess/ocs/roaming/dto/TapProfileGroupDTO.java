package com.xcess.ocs.roaming.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "DTO for TAP profile group — used for both request and response")
public class TapProfileGroupDTO {

    @Schema(description = "Unique identifier (populated in response only)", example = "1")
    private Long id;

    @NotBlank(message = "TAP profile group name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    @Schema(description = "Unique name for this group", example = "EU_Roaming_Group",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Human-readable description", example = "Group for EU roaming partners")
    private String description;

    @Schema(description = "Whether this group is active", example = "true")
    private boolean isActive = true;

    @Schema(description = "IDs of TAP profiles in this group")
    private List<Long> tapProfileIds;

    @Schema(description = "TAP profile summaries (populated in response only)")
    private List<TapProfileDTO> tapProfiles;
}
