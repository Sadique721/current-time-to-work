package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorRateRequestDTO {
    private Long id;
    private String requestId;
    private String requestName;
    private String requestParameters;
    private CdrQueryConfigDTO voiceQueryConfig;
    private CdrQueryConfigDTO smsQueryConfig;
    private CdrQueryConfigDTO usageQueryConfig;
    private String status;
    private Boolean enable;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime requestedAt;
    private Boolean isActive;
    private Boolean isDelete;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String createdBy;
    private String modifiedBy;
    private Long version;
}
