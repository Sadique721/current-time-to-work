package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileType;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
public class TapFileRecordDTO {
    private Long tapFileId;
    private String fileName;
    private String senderTadig;
    private String recipientTadig;
    private Integer fileSequenceNo;
    private TapFileType fileType;
    private String tapVersion;
    private TapFileStatus status;
    private BigInteger totalRecords;
    private BigInteger totalCharge;
    private BigInteger tapDecimalPlaces;
    private String localCurrency;
    private Long partnerId;
    private String partnerName;
    private String errorReason;
    private LocalDateTime processedAt;
}
