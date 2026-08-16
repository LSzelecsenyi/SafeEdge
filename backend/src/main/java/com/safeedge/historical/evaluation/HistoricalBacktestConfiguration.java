package com.safeedge.historical.evaluation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HistoricalBacktestProperties.class)
class HistoricalBacktestConfiguration {
}
