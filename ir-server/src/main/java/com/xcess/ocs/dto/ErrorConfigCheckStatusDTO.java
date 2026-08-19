package com.xcess.ocs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorConfigCheckStatusDTO {
    private Long id;
    private String errorRatedRecordId;
    private Boolean isConfigReady;
    private String errorMessage;
    private String callingNumber;
    private String calledNumber;
    private String incomingAccountId;
    private String outgoingAccountId;
    private List<String> incomingFailureData;
    private List<String> incomingSuccessData;
    private List<String> outgoingFailureData;
    private List<String> outgoingSuccessData;
    private String serviceType;
    private String lineOfBusiness;
}
