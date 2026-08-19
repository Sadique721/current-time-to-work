package com.xcess.ocs.dto;

import lombok.Data;

@Data
public class JoinClause {
    private String type;
    private String table;
    private String condition;
}
