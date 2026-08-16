package com.safeedge.historical.domain;

public record HistoricalImportResult(
		HistoricalSource source,
		CanonicalCompetition league,
		FootballSeason season,
		String sourceFile,
		int rowsRead,
		int matchesInserted,
		int matchesUpdated,
		int quotesInserted,
		int quotesUpdated,
		int rowsRejected,
		int quotesSkippedIncomplete,
		int quotesSkippedInvalidOdds,
		int quotesSkippedInvalidLine) {
}
