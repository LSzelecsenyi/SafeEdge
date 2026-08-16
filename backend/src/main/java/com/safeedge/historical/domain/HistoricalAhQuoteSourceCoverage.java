package com.safeedge.historical.domain;

import java.math.BigDecimal;

public record HistoricalAhQuoteSourceCoverage(
		HistoricalQuoteSource quoteSource,
		int totalMatches,
		int matchesWithQuote,
		BigDecimal coverageRate) {

	public HistoricalAhQuoteSourceCoverage {
		if (quoteSource == null) {
			throw new HistoricalDataException("quoteSource is required");
		}
		if (coverageRate == null) {
			throw new HistoricalDataException("coverageRate is required");
		}
	}
}
