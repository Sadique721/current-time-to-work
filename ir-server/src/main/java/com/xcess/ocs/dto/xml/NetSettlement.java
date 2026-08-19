package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class NetSettlement {

    private Double customerTotal;
    private Double vendorTotal;
    private Double netAmount;
    private String netPayableBy;

}
