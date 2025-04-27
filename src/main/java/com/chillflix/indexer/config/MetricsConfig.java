package com.chillflix.indexer.config;

import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SystemMetricsAutoConfiguration.class)
public class MetricsConfig {
    // This empty configuration class imports SystemMetricsAutoConfiguration 
    // but allows you to exclude or customize specific metrics beans if needed
}