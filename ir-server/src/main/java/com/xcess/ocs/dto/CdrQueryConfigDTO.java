package com.xcess.ocs.dto;

import com.xcess.ocs.entity.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CdrQueryConfigDTO {
    private Long id;
    private String queryName;
    private ServiceType serviceType;
    private String fetchQuery;
    private Boolean isActive;
    private Boolean isDelete;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
    private String createdBy;
    private String modifiedBy;
}
