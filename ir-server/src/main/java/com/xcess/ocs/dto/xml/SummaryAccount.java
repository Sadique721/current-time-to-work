package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SummaryAccount {

    private String accountCode;
    private String accountType;
    private String trafficDirection;
    private Double total;
}
