package com.safeedge.historical.evaluation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "safeedge.historical.backtest")
public record HistoricalBacktestProperties(
		String competition,
		String trainingFromSeason,
		String fromSeason,
		String toSeason,
		String quoteSource,
		String startingBankroll,
		String maxAcceptedBets) {
}
