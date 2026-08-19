package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestParameters {
    private String serviceType;
    private String parameterField;
    private String parameterValue;
}
