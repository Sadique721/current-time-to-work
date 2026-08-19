package com.xcess.ocs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.RoundingMode;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ocs.rounding")
public class RoundingProperties {

    private int calculationPrecision = 6;
    private int displayPrecision = 2;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
}
