package com.xcess.ocs.dto;

import lombok.Data;

@Data
public class WhereCondition {
    private String column;
    private String operator;
    private String parameterName;
}
