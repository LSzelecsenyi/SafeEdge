package com.safeedge.historical.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * AH coverage for one canonical competition + season. {@code totalMatches} is the
 * denominator for every source rate. {@code bestQuoteSource} is the source with the
 * highest {@code coverageRate}; ties use {@link HistoricalQuoteSource} enum order.
 * Null when every source has zero quotes.
 */
public record HistoricalAhLeagueSeasonCoverage(
		CanonicalCompetition competition,
		FootballSeason season,
		int totalMatches,
		int matchesWithAnyQuote,
		BigDecimal anyQuoteCoverageRate,
		HistoricalQuoteSource bestQuoteSource,
		BigDecimal bestQuoteSourceCoverageRate,
		List<HistoricalAhQuoteSourceCoverage> sourceCoverages) {

	public HistoricalAhLeagueSeasonCoverage {
		if (competition == null) {
			throw new HistoricalDataException("competition is required");
		}
		if (season == null) {
			throw new HistoricalDataException("season is required");
		}
		if (anyQuoteCoverageRate == null || bestQuoteSourceCoverageRate == null) {
			throw new HistoricalDataException("coverage rates are required");
		}
		sourceCoverages = List.copyOf(sourceCoverages == null ? List.of() : sourceCoverages);
	}
}
