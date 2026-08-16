package com.safeedge.historical.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Provider-independent historical match. Team names are source spellings, not
 * canonical entities. {@code kickoffUtc} is null unless a verified UTC instant
 * exists; football-data.co.uk v1 never invents one from a date-only row.
 */
public record HistoricalMatchDraft(
		HistoricalSource source,
		String sourceCompetitionCode,
		CanonicalCompetition canonicalCompetition,
		FootballSeason season,
		LocalDate matchDate,
		LocalTime sourceKickoffTime,
		Instant kickoffUtc,
		String sourceHomeTeamName,
		String sourceAwayTeamName,
		int homeGoals,
		int awayGoals,
		String sourceFile,
		int sourceRowNumber) {

	public HistoricalMatchDraft {
		if (source == null) {
			throw new HistoricalDataException("source is required");
		}
		sourceCompetitionCode = requireText(sourceCompetitionCode, "sourceCompetitionCode");
		if (canonicalCompetition == null) {
			throw new HistoricalDataException("canonicalCompetition is required");
		}
		if (season == null) {
			throw new HistoricalDataException("season is required");
		}
		if (matchDate == null) {
			throw new HistoricalDataException("matchDate is required");
		}
		sourceHomeTeamName = requireText(sourceHomeTeamName, "sourceHomeTeamName");
		sourceAwayTeamName = requireText(sourceAwayTeamName, "sourceAwayTeamName");
		if (homeGoals < 0 || awayGoals < 0) {
			throw new HistoricalDataException("goals must be >= 0");
		}
		sourceFile = requireText(sourceFile, "sourceFile");
		if (sourceRowNumber < 1) {
			throw new HistoricalDataException("sourceRowNumber must be >= 1");
		}
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new HistoricalDataException(name + " is required");
		}
		return value;
	}

}
