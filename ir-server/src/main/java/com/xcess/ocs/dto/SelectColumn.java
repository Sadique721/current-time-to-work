package com.xcess.ocs.dto;

import lombok.Data;

@Data
public class SelectColumn {
    private String name;
    private String expression;
    private String alias;
    private String type;
}
