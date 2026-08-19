package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Agreement {

    private String agreementCode;
    private String description;
    private String billingCycleStart;
    private String billingCycleEnd;
    private String settlementType;
}
