package com.xcess.ocs.config;

import com.xcess.ocs.dto.QueryTemplateConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QueryTemplateConfig.class)
public class QueryConfig {
}
