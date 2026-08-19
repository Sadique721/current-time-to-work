package com.xcess.ocs.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;

@Data
@Component
@ConfigurationProperties
public class QueryTemplateConfig {
    private Map<String, QueryTemplate> queryTemplates;
}
