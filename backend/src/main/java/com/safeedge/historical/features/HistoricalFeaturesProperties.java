package com.safeedge.historical.features;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safeedge.historical.features")
public record HistoricalFeaturesProperties(String competition, String fromSeason, String toSeason) {
}
