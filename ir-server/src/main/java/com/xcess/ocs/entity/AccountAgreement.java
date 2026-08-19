package com.xcess.ocs.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "account_agreements")
@SQLDelete(sql = "UPDATE account_agreements SET is_deleted = true, deleted_at = NOW() WHERE account_agreement_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AccountAgreementEntity", description = "JPA entity mapping for the account_agreements join table")
public class AccountAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_agreement_id")
    @Schema(description = "Association ID", accessMode = Schema.AccessMode.READ_ONLY, example = "1")
    private Long accountAgreementId;

    @ManyToOne
    @JoinColumn(name = "agreement_id", nullable = false)
    @Schema(description = "The associated agreement")
    private Agreement agreement;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    @Schema(description = "The associated account")
    private Account account;

    @Column(name = "invoice_format", nullable = false)
    @Schema(description = "Invoice format preference (e.g. PDF, XML)", example = "PDF")
    private String invoiceFormat;
}
