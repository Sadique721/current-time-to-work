package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ClearingHouseProtocol.SupportedProtocol;
import com.xcess.ocs.entity.ClearingHouseStatus;
import com.xcess.ocs.entity.ClearingHouseType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClearingHouse", description = "Clearing house details for ROAMING partners")
public class ClearingHouseDTO {

    @Schema(description = "Clearing house ID", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Schema(description = "Unique name of the clearing house", example = "GSMA Clearing House")
    private String name;

    @NotNull(message = "Type is required")
    @Schema(description = "Type: DCH, FCH, BOTH", example = "DCH")
    private ClearingHouseType type;

    @NotNull(message = "Status is required")
    @Schema(description = "Status: ACTIVE, INACTIVE", example = "ACTIVE")
    private ClearingHouseStatus status;

    @NotBlank(message = "Default currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3-character ISO code")
    @Schema(description = "ISO 4217 default currency", example = "USD")
    private String defaultCurrency;

    @NotBlank(message = "Timezone is required")
    @Schema(description = "Timezone", example = "UTC")
    private String timezone;

    @NotEmpty(message = "At least one protocol is required")
    @Schema(description = "Supported protocols: SFTP, API, AS2")
    private List<SupportedProtocol> protocols;

    // ─── SFTP Configuration ───────────────────────────────────────────────────
    @Schema(description = "SFTP hostname or IP", example = "sftp.gsma.com")
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
