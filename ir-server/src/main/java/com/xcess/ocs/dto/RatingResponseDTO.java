package com.xcess.ocs.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Rating Response", description = "Details of the applicable rate found")
public class RatingResponseDTO {

    @Schema(description = "Matched destination prefix", example = "1415")
    private String destinationPrefix;

    @Schema(description = "Matched source prefix (if applicable)", example = "1800")
    private String sourcePrefix;

    @Schema(description = "Applicable rate value", example = "0.05")
    private Double rate;

    @Schema(description = "Start time of the matched rate period", type = "string", example = "2024-01-01T00:00:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDate;

    @Schema(description = "End time of the matched rate period", type = "string", example = "2024-12-31T23:59:59")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDate;

    @Schema(description = "Status message", example = "Rate found successfully.")
    private String message;
}
