package com.xcess.ocs.dto;

import com.xcess.ocs.entity.RatePackageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.xcess.ocs.entity.Type;
import com.xcess.ocs.entity.ServiceType;
import com.xcess.ocs.entity.Rounding;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "7. Rate Package", description = "Details of a rate package")
public class RatePackageDTO {

    @Schema(description = "Rate package ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long ratePackageId;

    @Schema(description = "Name of the rate package", example = "Basic Package")
    @NotBlank(message = "Package name is required")
    @Size(max = 100, message = "Package name cannot exceed 100 characters")
    private String packageName;

    @Schema(description = "Description of the rate package", example = "This is a basic rate package")
    @NotBlank(message = "Package description is required")
    @Size(max = 255, message = "Package description cannot exceed 255 characters")
    private String packageDesc;

    @Schema(description = "Type of the rate package", example = "SELLING",
            allowableValues = {"SELLING", "BUYING"})
    @NotNull(message = "Package type is required")
    private Type type;

    @Schema(description = "Service type of the rate package", example = "VOICE",
            allowableValues = {"VOICE", "SMS", "USAGE"})
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Schema(description = "Type of rate package", example = "DESTINATION_BASED",
            allowableValues = {"DESTINATION_BASED", "SOURCE_DESTINATION_BASED", "ZONE"})
    private RatePackageType ratePackageType;

    @Schema(description = "Pulse ID for reference to Pulse", example = "1")
    @NotNull(message = "Pulse ID is required")
    private Long pulseId;

    @Schema(description = "Name of the pulse", example = "Basic Pulse", accessMode = Schema.AccessMode.READ_ONLY)
    private String pulseName;

    @Schema(description = "Rounding type of the rate package", example = "DEFAULT",
            allowableValues = {"UPPER", "LOWER", "DEFAULT"})
    @NotNull(message = "Rounding method is required")
    private Rounding rounding;

    @Schema(description = "Price rounding type of the rate package", example = "DEFAULT",
            allowableValues = {"UPPER", "LOWER", "DEFAULT"})
    @NotNull(message = "Price rounding method is required")
    private Rounding priceRounding;

    @Schema(description = "Rate details of the rate package")
    @Valid
    private List<RateDetailDTO> rate_details;

    @Schema(description = "Start time for the Rate Package (USAGE only)", type = "string", example = "2023-01-01 06:30:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "End time for the Rate Package (USAGE only)", type = "string", example = "2023-03-01 06:30:30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "Rate for the Rate Package (USAGE only)", example = "20")
    private Double rate;

    @Schema(description = "ISO 4217 currency code for this rate package", example = "USD")
    private String currency;


}
