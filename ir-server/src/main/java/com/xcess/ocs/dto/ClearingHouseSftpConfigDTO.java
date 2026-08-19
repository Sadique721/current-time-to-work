package com.xcess.ocs.dto;

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
        name = "Clearing House SFTP Config",
        description = "Schema to update clearing house SFTP configuration"
)
public class ClearingHouseSftpConfigDTO {

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
