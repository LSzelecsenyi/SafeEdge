package com.safeedge.historical.footballdata.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "safeedge.providers.football-data")
public record FootballDataProperties(
		@DefaultValue("https://www.football-data.co.uk") String baseUrl,
		String manualLeague,
		String manualSeasonStart,
		String bulkStartSeason,
		String bulkEndSeason,
		String bulkLeagues) {
}
