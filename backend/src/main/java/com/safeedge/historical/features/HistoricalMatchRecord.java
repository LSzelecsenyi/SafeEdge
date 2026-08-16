package com.safeedge.historical.features;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.settlement.MatchScore;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Provider-independent historical match fact for feature generation. Team names
 * are exact source spellings, not canonical entities.
 */
public record HistoricalMatchRecord(
		HistoricalSource source,
		CanonicalCompetition competition,
		FootballSeason season,
		LocalDate matchDate,
		Instant kickoffUtc,
		String homeTeam,
		String awayTeam,
		MatchScore score,
		int sourceRowNumber,
		Long persistenceId) {

	public HistoricalMatchRecord {
		if (source == null) {
			throw new HistoricalDataException("source is required");
		}
		if (competition == null) {
			throw new HistoricalDataException("competition is required");
		}
		if (season == null) {
			throw new HistoricalDataException("season is required");
		}
		if (matchDate == null) {
			throw new HistoricalDataException("matchDate is required");
		}
		homeTeam = requireName(homeTeam, "homeTeam");
		awayTeam = requireName(awayTeam, "awayTeam");
		if (homeTeam.equals(awayTeam)) {
			throw new HistoricalDataException("homeTeam and awayTeam must differ");
		}
		if (score == null) {
			throw new HistoricalDataException("score is required");
		}
		if (sourceRowNumber < 1) {
			throw new HistoricalDataException("sourceRowNumber must be >= 1");
		}
	}

	private static String requireName(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new HistoricalDataException(field + " is required");
		}
		return value;
	}
}
