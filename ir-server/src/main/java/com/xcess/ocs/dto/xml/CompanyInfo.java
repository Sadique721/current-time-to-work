package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class CompanyInfo {

    private String name;
    private String nameSuffix;
    private String legalName;
    private String address;

}
