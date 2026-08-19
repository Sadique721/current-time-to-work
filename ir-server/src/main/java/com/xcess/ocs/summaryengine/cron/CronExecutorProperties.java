package com.xcess.ocs.summaryengine.cron;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "cron.executor")
public class CronExecutorProperties {
    private Integer corePoolSize;
    private Integer maxPoolSize;
    private Long keepAliveTime;
    private Integer queueCapacity;
    private Duration minInterval;
    private Duration maxInterval;
    private Double backoffFactor;
    private Integer multiplierUnit;
}
