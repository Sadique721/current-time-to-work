package com.xcess.ocs.roaming.dto;

import com.xcess.ocs.roaming.entity.TapFileStatus;
import com.xcess.ocs.roaming.entity.TapFileType;
import lombok.Data;

@Data
public class TapFileSearchDTO {
    private String senderTadig;
    private String recipientTadig;
    private TapFileStatus status;
    private TapFileType fileType;
    private Long partnerId;
}
