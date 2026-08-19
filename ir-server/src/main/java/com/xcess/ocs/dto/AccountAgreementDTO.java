package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AccountAgreement", description = "Association between an agreement and an account with invoice format preference")
public class AccountAgreementDTO {

    @Schema(description = "Association ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long accountAgreementId;

    @NotNull(message = "Account ID is required")
    @Schema(description = "Account ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long accountId;

    @NotBlank(message = "Account code is required")
    @Schema(description = "Unique account code", example = "ACC001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountCode;

    @NotBlank(message = "Account type is required")
    @Schema(description = "Account type (e.g. CUSTOMER, VENDOR)", example = "CUSTOMER", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountType;

    @NotBlank(message = "Invoice format is required")
    @Schema(description = "Invoice format preference (e.g. PDF, XML)", example = "PDF", requiredMode = Schema.RequiredMode.REQUIRED)
    private String invoiceFormat;
}
