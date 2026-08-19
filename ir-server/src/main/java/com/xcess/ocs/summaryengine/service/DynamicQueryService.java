package com.xcess.ocs.summaryengine.service;

import com.xcess.ocs.dto.QueryTemplateConfig;
import com.xcess.ocs.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DynamicQueryService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private QueryTemplateConfig queryTemplateConfig;

    public List<Map<String, Object>> executeQuery(String templateName, Map<String, Object> parameters) {
        QueryTemplate template = queryTemplateConfig.getQueryTemplates().get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Query template not found: " + templateName);
        }

        String sql = buildQuery(template, parameters);
        log.info("\n========== EXECUTING QUERY ==========\n{}", sql);
        log.info("========== WITH PARAMETERS ==========\n{}", parameters);

        List<Map<String, Object>> results = namedParameterJdbcTemplate.queryForList(sql, parameters);
        log.info("========== QUERY RETURNED {} ROWS ==========\n", results.size());
        
        if (!results.isEmpty()) {
            log.info("Sample row: {}", results.get(0));
        }
        
        return results;
    }

    private String buildQuery(QueryTemplate template, Map<String, Object> parameters) {
        StringBuilder sql = new StringBuilder("SELECT ");

        sql.append(template.getSelectColumns().stream()
                .map(col -> col.getExpression() + " AS " + col.getAlias())
                .collect(Collectors.joining(", ")));

        sql.append(" FROM ").append(template.getTable());

        if (template.getJoins() != null && !template.getJoins().isEmpty()) {
            for (JoinClause join : template.getJoins()) {
                sql.append(" ").append(join.getType())
                   .append(" ").append(join.getTable())
                   .append(" ON ").append(join.getCondition());
            }
        }

        if (template.getWhereConditions() != null && !template.getWhereConditions().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(template.getWhereConditions().stream()
                    .filter(condition -> parameters.containsKey(condition.getParameterName()))
                    .map(condition -> condition.getColumn() + " " + condition.getOperator() + " :" + condition.getParameterName())
                    .collect(Collectors.joining(" AND ")));
        }

        if (template.getGroupBy() != null && !template.getGroupBy().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", template.getGroupBy()));
        }

        if (template.getOrderBy() != null && !template.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(template.getOrderBy().stream()
                    .map(order -> order.getColumn() + " " + order.getDirection())
                    .collect(Collectors.joining(", ")));
        }

        return sql.toString();
    }
}
