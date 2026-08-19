package com.xcess.ocs.entity;

import com.xcess.ocs.roaming.entity.TapProfileGroup;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import lombok.*;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "partners", uniqueConstraints = {
    @UniqueConstraint(name = "uk_partners_partner_code", columnNames = {"partner_code", "deleted_at"})
})
@SQLDelete(sql = "UPDATE partners SET is_deleted = true, deleted_at = NOW() WHERE partner_id = ?")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
public class Partner extends BaseEntity {

    /** Unique identifier for the partner */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "partner_id")
    private Long partnerId;

    /** Name of the partner (required, must be unique among non-deleted partners) */
    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    /** Unique code identifying the partner (required) */
    @Column(name = "partner_code", nullable = false)
    private String partnerCode;

    /** Type of partner: VENDOR, CUSTOMER, CARRIER, or BOTH */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", nullable = false)
    private PartnerType partnerType;

    /** Current status of the partner (ACTIVE, INACTIVE, etc.) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** Country where the partner is located */
    @Column(name = "country", length = 100)
    private String country;

    /** Name of the primary contact person at the partner organization */
    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    /** Email address of the partner */
    @Column(name = "email", nullable = false)
    private String email;

    /** Phone number of the partner */
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    /** Primary address line of the partner */
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    /** City where the partner is located */
    @Column(name = "city", nullable = false)
    private String city;

    /** Postal/ZIP code of the partner's location */
    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "interconnect_type")
    private InterconnectType interconnectType;

    @Column(name = "point_code")
    private String pointCode;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "routing_prefix")
    private String routingPrefix;

    @Column(name = "tadig_code", length = 6, unique = true)
    private String tadigCode;

    @Column(name = "hplmn", length = 20)
    private String hplmn;

    @Column(name = "billing_currency", nullable = false)
    private String billingCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_of_business", nullable = false)
    private LineOfBusiness lineOfBusiness = LineOfBusiness.INTERCONNECT;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(name = "payment_terms", nullable = false)
    private String paymentTerms;

    @Column(name = "tax_number", nullable = false)
    private String taxNumber;

    @Column(name = "bank_account_number", nullable = false)
    private String bankAccountNumber;

    @Column(name = "swift_code")
    private String swiftCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearing_house_id")
    private ClearingHouse clearingHouse;

    // ─── TAP Settings ────────────────────────────────────────────────────────

    @Column(name = "tap_version", length = 20)
    private String tapVersion;

    /**
     * TAP profile group assigned to this ROAMING partner.
     * Required when lineOfBusiness = ROAMING.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tap_profile_group_id")
    private TapProfileGroup tapProfileGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "sftp_route_type")
    private TapSftpRouteType sftpRouteType;

    @Column(name = "sftp_host", length = 255)
    private String sftpHost;

    @Column(name = "sftp_port")
    private Integer sftpPort;

    @Column(name = "sftp_username", length = 100)
    private String sftpUsername;

    @Column(name = "sftp_password", length = 255)
    private String sftpPassword;

    @Column(name = "sftp_remote_path", length = 500)
    private String sftpRemotePath;

    @Column(name = "sftp_inbox_path", length = 500)
    private String sftpInboxPath;
}
