package com.xcess.ocs.dto;

import com.xcess.ocs.entity.TapSftpRouteType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "Partner SFTP Config",
        description = "Schema to update/retrieve partner SFTP configuration"
)
public class PartnerSftpConfigDTO {

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

    @Schema(description = "SFTP route type", example = "DIRECT")
    private TapSftpRouteType tapSftpRouteType;

    @Schema(description = "Clearing house ID", example = "1")
    private Long clearingHouseId;

    @Schema(description = "Clearing house name", accessMode = Schema.AccessMode.READ_ONLY)
    private String clearingHouseName;

    @Schema(description = "TAP profile group ID for ROAMING partners")
    private Long tapProfileGroupId;

    @Schema(description = "TAP profile group name", accessMode = Schema.AccessMode.READ_ONLY)
    private String tapProfileGroupName;

    @Schema(description = "TAP version for TAP OUT file generation", example = "TAP3.12")
    private String tapVersion;

}
