package com.xcess.ocs.dto;

import com.xcess.ocs.entity.PartnerType;
import com.xcess.ocs.entity.Status;
import com.xcess.ocs.entity.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(name = "3. Partner", description = "Schema to hold details of a partner")
public class PartnerDTO {

    @Schema(description = "ID of the partner", accessMode = Schema.AccessMode.READ_ONLY)
    private Long partnerId;

    @NotBlank(message = "Partner name is required")
    @Schema(description = "Name of the partner", example = "Telecom Partner Ltd")
    private String partnerName;

    @NotBlank(message = "Partner code is required")
    @Schema(description = "Unique code of the partner", example = "TP001")
    private String partnerCode;

    @NotNull(message = "Partner type is required")
    @Schema(description = "Type of the partner", example = "CUSTOMER")
    private PartnerType partnerType;

    @NotNull(message = "Status is required")
    @Schema(description = "Status of the partner", example = "ACTIVE")
    private Status status;

    @Schema(description = "Country", example = "USA")
    private String country;

    @NotBlank(message = "Contact person name is required")
    @Schema(description = "Contact person name", example = "John Doe")
    private String contactPersonName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "contact@partner.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Schema(description = "Phone number", example = "+1234567890")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    @Schema(description = "Address line 1", example = "123 Main Street")
    private String addressLine1;

    @NotBlank(message = "City is required")
    @Schema(description = "City", example = "New York")
    private String city;

    @NotBlank(message = "Postal code is required")
    @Schema(description = "Postal code", example = "10001")
    private String postalCode;

    @Schema(description = "Interconnect type (required when lineOfBusiness is INTERCONNECT)", example = "IP")
    private InterconnectType interconnectType;

    @Schema(description = "Point code for SS7", example = "1-234-5")
    private String pointCode;

    @Schema(description = "IP address for IP/SIP", example = "192.168.1.1")
    private String ipAddress;

    @Schema(description = "Routing prefix", example = "91")
    private String routingPrefix;

    @NotBlank(message = "Billing currency is required")
    @Schema(description = "Billing currency", example = "USD")
    private String billingCurrency;

    @NotNull(message = "Billing cycle is required")
    @Schema(description = "Billing cycle", example = "MONTHLY")
    private BillingCycle billingCycle;

    @NotBlank(message = "Payment terms is required")
    @Schema(description = "Payment terms", example = "30 Days")
    private String paymentTerms;

    @NotBlank(message = "Tax number is required")
    @Schema(description = "Tax number", example = "TAX123456")
    private String taxNumber;

    @NotBlank(message = "Bank account number is required")
    @Schema(description = "Bank account number", example = "1234567890")
    private String bankAccountNumber;

    @Schema(description = "SWIFT code", example = "ABCDUS33")
    private String swiftCode;

    @Schema(description = "GSMA TADIG code for roaming partners", example = "AUTPT")
    private String tadigCode;

    @NotNull(message = "Line of business is required")
    @Schema(description = "Line of business: INTERCONNECT or ROAMING", example = "INTERCONNECT")
    private LineOfBusiness lineOfBusiness;

    @Schema(description = "Clearing house ID for ROAMING partners")
    private Long clearingHouseId;

    @Schema(description = "Clearing house name", accessMode = Schema.AccessMode.READ_ONLY)
    private String clearingHouseName;

    @NotNull(message = "Organization ID is required")
    @Schema(description = "Organization ID")
    private Long organizationId;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private String organizationName;

    @Schema(description = "HPLMN code (required for ROAMING)", example = "23201")
    private String hplmn;

    // ─── TAP Settings ────────────────────────────────────────────────────────

    @Schema(description = "TAP version for TAP OUT file generation", example = "TAP3.12")
    private String tapVersion;

    @Schema(description = "TAP profile group ID — required for ROAMING line of business")
    private Long tapProfileGroupId;

    @Schema(description = "TAP profile group name", accessMode = Schema.AccessMode.READ_ONLY)
    private String tapProfileGroupName;

    @Schema(description = "Sftp route", example = "IP")
    private TapSftpRouteType tapSftpRouteType;

    @Schema(description = "SFTP hostname or IP", example = "sftp.partner.com")
    private String sftpHost;

    @Schema(description = "SFTP port", example = "22")
    private Integer sftpPort;

    @Schema(description = "SFTP username", example = "tap_user")
    private String sftpUsername;

    @Schema(description = "SFTP password")
    private String sftpPassword;

    @Schema(description = "Remote path for TAP OUT file deposit", example = "/outbox/tap")
    private String sftpRemotePath;

    @Schema(description = "Remote path to pull TAP IN files from", example = "/inbox/tap")
    private String sftpInboxPath;
}
