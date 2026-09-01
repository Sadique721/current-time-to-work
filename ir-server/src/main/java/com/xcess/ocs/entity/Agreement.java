package com.xcess.ocs.entity;

import com.xcess.ocs.entity.*;
import com.xcess.ocs.roaming.entity.TapDirection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "agreements", uniqueConstraints = {
    @UniqueConstraint(name = "uk_agreements_agreement_code", columnNames = {"agreement_code", "deleted_at"})
})
@SQLDelete(sql = "UPDATE agreements SET is_deleted = true, deleted_at = NOW() WHERE agreement_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AgreementEntity", description = "JPA entity mapping for the agreements table")
public class Agreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    @Schema(description = "Unique agreement ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long agreementId;

    @Column(name = "agreement_code", nullable = false)
    @Schema(description = "Unique agreement code", example = "AGR0101")
    private String agreementCode;

    @Column(name = "billing_cycle_start_date", nullable = false)
    @Schema(description = "Start date of the billing cycle", example = "2026-04-01")
    private LocalDate billingCycleStartDate;

    @Column(name = "next_billing_cycle_start_date")
    @Schema(description = "Next billing cycle start date (auto-calculated)", example = "2026-05-01", nullable = true)
    private LocalDate nextBillingCycleStartDate;

    @Column(name = "billing_cycle_period")
    @Schema(description = "Billing cycle period in days (required for DAYS billing type)", example = "30", nullable = true)
    private Integer billingCyclePeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_type", nullable = false)
    @Schema(description = "Billing type: DAYS, WEEKLY, FORTNIGHTLY, or MONTHLY", example = "DAYS")
    private BillingType billingType = BillingType.DAYS;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekly_day")
    @Schema(description = "Day of week for WEEKLY billing type", example = "MON", nullable = true)
    private WeeklyDay weeklyDay;

    @Column(name = "is_incoming_settlement", nullable = false)
    @Schema(description = "Whether incoming settlement is enabled", example = "true")
    private Boolean isIncomingSettlement = false;

    @Column(name = "is_outgoing_settlement", nullable = false)
    @Schema(description = "Whether outgoing settlement is enabled", example = "true")
    private Boolean isOutgoingSettlement = false;

    @Column(name = "is_net_settlement", nullable = false)
    @Schema(description = "Whether net settlement is enabled", example = "false")
    private Boolean isNetSettlement = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incoming_settlement_template_id")
    @Schema(description = "Template configuration for incoming settlement")
    private TemplateConfiguration incomingSettlementTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outgoing_settlement_template_id")
    @Schema(description = "Template configuration for outgoing settlement")
    private TemplateConfiguration outgoingSettlementTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "net_settlement_template_id")
    @Schema(description = "Template configuration for net settlement")
    private TemplateConfiguration netSettlementTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", nullable = false)
    @Schema(description = "Billing flow: INTERCONNECT (Kafka CDR) or ROAMING (TAP file)", example = "INTERCONNECT", allowableValues = {"INTERCONNECT", "ROAMING"})
    private LineOfBusiness lineOfBusiness = LineOfBusiness.INTERCONNECT;


    @Enumerated(EnumType.STRING)
    @Column(name = "tap_direction")
    @Schema(description = "TAP direction (required for ROAMING)", example = "TAP_IN", nullable = true, allowableValues = {"TAP_IN", "TAP_OUT"})
    private TapDirection tapDirection;

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Schema(description = "Account-agreement associations")
    private Set<AccountAgreement> accountAgreements = new HashSet<>();

    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Schema(description = "Tax configurations applied to this agreement")
    private List<AgreementTaxConfig> agreementTaxConfigs = new ArrayList<>();

    @Column(name = "is_tax_exempt")
    @Schema(description = "Whether the agreement is tax exempt", example = "false", defaultValue = "false")
    private Boolean isTaxExempt = false;
}
