package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Account {

    private String serviceType;
    private String accountCode;
    private String accountType;
    private Double totalAmount;
}
