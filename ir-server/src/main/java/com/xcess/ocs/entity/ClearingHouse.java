package com.xcess.ocs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clearing_houses")
@SQLDelete(sql = "UPDATE clearing_houses SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClearingHouse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ClearingHouseType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClearingHouseStatus status;

    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @OneToMany(mappedBy = "clearingHouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClearingHouseProtocol> protocols = new ArrayList<>();

    // ─── SFTP Configuration ───────────────────────────────────────────────────
    /** SFTP hostname or IP of the clearing house server */
    @Column(name = "sftp_host", length = 255)
    private String sftpHost;

    @Column(name = "sftp_port")
    private Integer sftpPort;

    @Column(name = "sftp_username", length = 100)
    private String sftpUsername;

    /** Stored encrypted in production; plain text here for simplicity */
    @Column(name = "sftp_password", length = 255)
    private String sftpPassword;

    /** Remote directory path where TAP OUT files are deposited */
    @Column(name = "sftp_remote_path", length = 500)
    private String sftpRemotePath;

    /** Remote directory path from which TAP IN files are pulled */
    @Column(name = "sftp_inbox_path", length = 500)
    private String sftpInboxPath;
}
