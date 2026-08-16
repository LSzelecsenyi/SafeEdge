package com.safeedge.historical.domain;

import java.time.Instant;
import java.util.List;

public record BulkHistoricalImportResult(
		Instant startedAt,
		Instant completedAt,
		int leagueSeasonPairsRequested,
		int leagueSeasonPairsSucceeded,
		int leagueSeasonPairsFailed,
		int rowsRead,
		int matchesInserted,
		int matchesUpdated,
		int quotesInserted,
		int quotesUpdated,
		int rowsRejected,
		int quotesSkippedIncomplete,
		int quotesSkippedInvalidOdds,
		int quotesSkippedInvalidLine,
		List<HistoricalImportFailure> failures) {

	public BulkHistoricalImportResult {
		if (startedAt == null || completedAt == null) {
			throw new HistoricalDataException("startedAt and completedAt are required");
		}
		failures = List.copyOf(failures == null ? List.of() : failures);
	}

	public boolean hasFailures() {
		return leagueSeasonPairsFailed > 0;
	}
}
