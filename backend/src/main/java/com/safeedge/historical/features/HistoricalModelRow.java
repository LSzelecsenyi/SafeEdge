package com.safeedge.historical.features;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.settlement.MatchScore;
import java.time.LocalDate;

/**
 * One modelling row: pre-match features plus the match score as a training
 * target only.
 */
public record HistoricalModelRow(
		HistoricalSource source,
		CanonicalCompetition competition,
		FootballSeason season,
		LocalDate matchDate,
		String homeTeam,
		String awayTeam,
		int sourceRowNumber,
		Long persistenceId,
		PreMatchFeatures features,
		MatchScore target) {

	public HistoricalModelRow {
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
		if (homeTeam == null || homeTeam.isBlank() || awayTeam == null || awayTeam.isBlank()) {
			throw new HistoricalDataException("team names are required");
		}
		if (features == null) {
			throw new HistoricalDataException("features are required");
		}
		if (target == null) {
			throw new HistoricalDataException("target is required");
		}
	}
}
