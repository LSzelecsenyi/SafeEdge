package com.safeedge.historical.domain;

import java.time.Instant;
import java.util.List;

/**
 * Read-only AH availability audit over persisted historical matches and valid
 * {@code historical_ah_offer} rows. Only league-seasons with at least one
 * persisted match are included. Empty database → empty {@code leagueSeasons}.
 */
public record HistoricalAhCoverageReport(
		Instant generatedAt,
		HistoricalSource source,
		List<HistoricalAhLeagueSeasonCoverage> leagueSeasons) {

	public HistoricalAhCoverageReport {
		if (generatedAt == null) {
			throw new HistoricalDataException("generatedAt is required");
		}
		if (source == null) {
			throw new HistoricalDataException("source is required");
		}
		leagueSeasons = List.copyOf(leagueSeasons == null ? List.of() : leagueSeasons);
	}
}
