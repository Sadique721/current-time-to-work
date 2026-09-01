package com.xcess.ocs.roaming.entity;

import com.xcess.ocs.entity.Partner;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tap_file_records")
public class TapFileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tap_file_id")
    private Long tapFileId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "sender_tadig", length = 6)
    private String senderTadig;

    @Column(name = "recipient_tadig", length = 6)
    private String recipientTadig;

    @Column(name = "file_sequence_no")
    private Integer fileSequenceNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private TapFileType fileType;

    @Column(name = "tap_version", length = 20)
    private String tapVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TapFileStatus status;

    @Column(name = "total_records")
    private BigInteger totalRecords;

    @Column(name = "total_charge")
    private BigInteger totalCharge;

    @Column(name = "tap_decimal_places")
    private BigInteger tapDecimalPlaces;

    @Column(name = "local_currency", length = 3)
    private String localCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
