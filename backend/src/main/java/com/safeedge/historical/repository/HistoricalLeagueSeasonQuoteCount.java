package com.safeedge.historical.repository;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;

public record HistoricalLeagueSeasonQuoteCount(
		CanonicalCompetition competition,
		Integer seasonStartYear,
		Integer seasonEndYear,
		HistoricalQuoteSource quoteSource,
		long matchCount) {
}
