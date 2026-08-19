package com.xcess.ocs.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.SQLDelete;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_accounts_account_code", columnNames = {"account_code_or_hplmn", "product_plan_id", "deleted_at"})
})
@SQLDelete(sql = "UPDATE accounts SET is_deleted = true, deleted_at = NOW() WHERE account_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_code_or_hplmn", nullable = false)
    @NotEmpty(message = "Account code is required")
    // For INTERCONNECT: user-entered account identifier; For ROAMING: Home PLMN code
    private String accountCode;

    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft deleting Partner
    @JsonBackReference
    private Partner partner;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @ManyToOne
    @JoinColumn(name = "product_plan_id", nullable = false)
//    @NotFound(action = NotFoundAction.IGNORE)  // Prevents error if soft deleting Product Plan
    private ProductPlan productPlan;
}
