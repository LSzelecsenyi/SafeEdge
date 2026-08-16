package com.safeedge.probability;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.historical.features.HistoricalModelRow;
import com.safeedge.settlement.MatchScore;
import java.time.LocalDate;

/**
 * Training observation for the probability model. Identity, date, and score
 * only — not {@code PreMatchFeatures} and not odds.
 */
public record ProbabilityTrainingMatch(
		CanonicalCompetition competition,
		LocalDate matchDate,
		String homeTeam,
		String awayTeam,
		MatchScore score) {

	public ProbabilityTrainingMatch {
		if (competition == null) {
			throw new ProbabilityModelException("competition is required");
		}
		if (matchDate == null) {
			throw new ProbabilityModelException("matchDate is required");
		}
		homeTeam = requireName(homeTeam, "homeTeam");
		awayTeam = requireName(awayTeam, "awayTeam");
		if (score == null) {
			throw new ProbabilityModelException("score is required");
		}
	}

	public static ProbabilityTrainingMatch from(HistoricalMatchRecord record) {
		if (record == null) {
			throw new ProbabilityModelException("record is required");
		}
		return new ProbabilityTrainingMatch(
				record.competition(), record.matchDate(), record.homeTeam(), record.awayTeam(), record.score());
	}

	public static ProbabilityTrainingMatch from(HistoricalModelRow row) {
		if (row == null) {
			throw new ProbabilityModelException("row is required");
		}
		return new ProbabilityTrainingMatch(
				row.competition(), row.matchDate(), row.homeTeam(), row.awayTeam(), row.target());
	}

	private static String requireName(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ProbabilityModelException(field + " is required");
		}
		return value;
	}
}
