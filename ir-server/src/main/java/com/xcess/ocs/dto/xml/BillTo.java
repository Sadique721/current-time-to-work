package com.xcess.ocs.dto.xml;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class BillTo {

    @XmlElement(name = "companyName")
    private String companyName;

    @XmlElement(name = "accountCode")
    private String accountCode;
}
