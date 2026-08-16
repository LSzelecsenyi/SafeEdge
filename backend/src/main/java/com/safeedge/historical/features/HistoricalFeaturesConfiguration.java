package com.safeedge.historical.features;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HistoricalFeaturesProperties.class)
class HistoricalFeaturesConfiguration {
}
