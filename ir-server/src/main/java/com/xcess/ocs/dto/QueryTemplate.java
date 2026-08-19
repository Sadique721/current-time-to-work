package com.xcess.ocs.dto;

import lombok.Data;
import java.util.List;

@Data
public class QueryTemplate {
    private String table;
    private List<JoinClause> joins;
    private List<SelectColumn> selectColumns;
    private List<WhereCondition> whereConditions;
    private List<String> groupBy;
    private List<OrderBy> orderBy;
}
