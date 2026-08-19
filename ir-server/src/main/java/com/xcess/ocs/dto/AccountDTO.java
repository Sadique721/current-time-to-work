package com.xcess.ocs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(name = "14. Account", description = "Account details of the customer or vendor")
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {
    @Schema(description = "Account ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long accountId;

    @Schema(description = "Account code (user-entered for INTERCONNECT partners) or Home PLMN (for ROAMING partners)", example = "ACC00 (INTERCONNECT) / 40401 (ROAMING PLMN)")
    @NotBlank(message = "Account code is required")
    @Size(min = 3, max = 30, message = "Account code must be between 4 and 30 characters")
    private String accountCode;


    @Schema(description = "ID of the partner", example = "1")
    @NotNull(message = "Partner ID is required")
    private Long partnerId;

    @Schema(description = "Name of the partner", example = "Acme Corp", accessMode = Schema.AccessMode.READ_ONLY)
    private String partnerName;

    @Schema(description = "Type of the account", example = "VENDOR")
    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "CUSTOMER|VENDOR", message = "Account type must be either CUSTOMER or VENDOR")
    private String accountType;

    @Schema(description = "ID of the product plan", example = "1")
    @NotNull(message = "Product plan ID is required")
    private Long productPlanId;

    @Schema(description = "Name of the product plan", example = "Premium Plan", accessMode = Schema.AccessMode.READ_ONLY)
    private String productPlanName;
}