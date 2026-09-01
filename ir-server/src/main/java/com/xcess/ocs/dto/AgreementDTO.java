package com.xcess.ocs.dto;

import com.xcess.ocs.entity.BillingType;
import com.xcess.ocs.entity.LineOfBusiness;
import com.xcess.ocs.entity.WeeklyDay;
import com.xcess.ocs.roaming.entity.TapDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Agreement", description = "Agreement details for billing and settlement configuration")
public class AgreementDTO {

    @Schema(description = "Agreement ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long agreementId;

    @NotBlank(message = "Agreement code is required")
    @Size(max = 50, message = "Agreement code must be at most 50 characters")
    @Schema(description = "Unique agreement code", example = "AGR0101", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    private String agreementCode;

    @NotNull(message = "Billing cycle start date is required")
    @Schema(description = "Start date of the billing cycle", example = "2026-04-01", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate billingCycleStartDate;

    @Schema(description = "Billing cycle period in days (required for DAYS billing type)", example = "30", nullable = true)
    private Integer billingCyclePeriod;

    @Schema(description = "Billing type: DAYS, WEEKLY, FORTNIGHTLY, or MONTHLY", example = "DAYS", defaultValue = "DAYS")
    private BillingType billingType;

    @Schema(description = "Day of week for WEEKLY billing type", example = "MON", nullable = true)
    private WeeklyDay weeklyDay;

    @NotNull(message = "isIncomingSettlement is required")
    @Schema(description = "Whether incoming settlement is enabled", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isIncomingSettlement;

    @NotNull(message = "isOutgoingSettlement is required")
    @Schema(description = "Whether outgoing settlement is enabled", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isOutgoingSettlement;

    @NotNull(message = "isNetSettlement is required")
    @Schema(description = "Whether net settlement is enabled", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isNetSettlement;

    @Min(value = 1, message = "Incoming settlement template ID must be positive")
    @Schema(description = "Template ID for incoming settlement (required when isIncomingSettlement is true)", example = "3", nullable = true)
    private Long incomingSettlementTemplateId;

    @Min(value = 1, message = "Outgoing settlement template ID must be positive")
    @Schema(description = "Template ID for outgoing settlement (required when isOutgoingSettlement is true)", example = "3", nullable = true)
    private Long outgoingSettlementTemplateId;

    @Min(value = 1, message = "Net settlement template ID must be positive")
    @Schema(description = "Template ID for net settlement (required when isNetSettlement is true)", example = "3", nullable = true)
    private Long netSettlementTemplateId;

    @Schema(description = "Name of the incoming settlement template", accessMode = Schema.AccessMode.READ_ONLY, example = "Incoming Settlement Template")
    private String incomingSettlementTemplateName;

    @Schema(description = "Name of the outgoing settlement template", accessMode = Schema.AccessMode.READ_ONLY, example = "Outgoing Settlement Template")
    private String outgoingSettlementTemplateName;

    @Schema(description = "Name of the net settlement template", accessMode = Schema.AccessMode.READ_ONLY, example = "Net Settlement Template")
    private String netSettlementTemplateName;

    @Schema(description = "Partner display name", accessMode = Schema.AccessMode.READ_ONLY, example = "Test Partner")
    private String partnerName;

    @NotNull(message = "Line of business is required")
    @Schema(description = "Billing flow: INTERCONNECT (Kafka CDR) or ROAMING (TAP file)", example = "INTERCONNECT", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"INTERCONNECT", "ROAMING"})
    private LineOfBusiness lineOfBusiness;


    @Schema(description = "TAP direction (required for ROAMING)", example = "TAP_IN", nullable = true, allowableValues = {"TAP_IN", "TAP_OUT"})
    private TapDirection tapDirection;

    @Valid
    @Schema(description = "List of account-agreement associations")
    private List<AccountAgreementDTO> accountAgreements = new ArrayList<>();

    @Schema(description = "Whether the agreement is tax exempt", example = "false", defaultValue = "false")
    private Boolean isTaxExempt;

    @Valid
    @Schema(description = "Ordered list of tax configurations for this agreement")
    private List<AgreementTaxConfigDTO> taxConfigs = new ArrayList<>();

    public void validate() {
        List<String> errors = new ArrayList<>();

        BillingType effectiveType = billingType != null ? billingType : BillingType.DAYS;
        switch (effectiveType) {
            case DAYS -> {
                if (billingCyclePeriod == null || billingCyclePeriod < 1) {
                    errors.add("billingCyclePeriod is required and must be >= 1 for DAYS billing type");
                }
                if (weeklyDay != null) {
                    errors.add("weeklyDay is not applicable for DAYS billing type");
                }
            }
            case WEEKLY -> {
                if (weeklyDay == null) {
                    errors.add("weeklyDay is required for WEEKLY billing type");
                }
                if (billingCyclePeriod != null) {
                    errors.add("billingCyclePeriod is not applicable for WEEKLY billing type");
                }
            }
            case FORTNIGHTLY, MONTHLY -> {
                if (billingCyclePeriod != null) {
                    errors.add("billingCyclePeriod is not applicable for " + effectiveType + " billing type");
                }
                if (weeklyDay != null) {
                    errors.add("weeklyDay is not applicable for " + effectiveType + " billing type");
                }
            }
        }

        boolean incomingEnabled = isIncomingSettlement != null && isIncomingSettlement;
        boolean outgoingEnabled = isOutgoingSettlement != null && isOutgoingSettlement;
        boolean netEnabled = isNetSettlement != null && isNetSettlement;

        if (!incomingEnabled && !outgoingEnabled && !netEnabled) {
            errors.add("At least one settlement type must be enabled (incoming, outgoing, or net)");
        }

        if (incomingEnabled && (incomingSettlementTemplateId == null || incomingSettlementTemplateId <= 0)) {
            errors.add("Incoming settlement template is required when isIncomingSettlement is enabled");
        }

        if (outgoingEnabled && (outgoingSettlementTemplateId == null || outgoingSettlementTemplateId <= 0)) {
            errors.add("Outgoing settlement template is required when isOutgoingSettlement is enabled");
        }

        if (netEnabled && (netSettlementTemplateId == null || netSettlementTemplateId <= 0)) {
            errors.add("Net settlement template is required when isNetSettlement is enabled");
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }
}
