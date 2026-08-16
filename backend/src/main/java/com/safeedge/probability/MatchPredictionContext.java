package com.safeedge.probability;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.time.LocalDate;

/**
 * Point-in-time prediction target. Team names are exact source spellings.
 * Contains no final score and no odds.
 */
public record MatchPredictionContext(
		CanonicalCompetition competition, String homeTeam, String awayTeam, LocalDate matchDate) {

	public MatchPredictionContext {
		if (competition == null) {
			throw new ProbabilityModelException("competition is required");
		}
		homeTeam = requireName(homeTeam, "homeTeam");
		awayTeam = requireName(awayTeam, "awayTeam");
		if (homeTeam.equals(awayTeam)) {
			throw new ProbabilityModelException("homeTeam and awayTeam must differ");
		}
		if (matchDate == null) {
			throw new ProbabilityModelException("matchDate is required");
		}
	}

	private static String requireName(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ProbabilityModelException(field + " is required");
		}
		return value;
	}
}
