package com.safeedge.historical.evaluation;

import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Point-in-time available prediction captured during the same walk-forward pass
 * that built {@link HistoricalWalkForwardDataset}. Includes matches that had no
 * selected AH quote. Does not mutate the dataset.
 */
public record HistoricalPredictionSnapshot(
		String eventId,
		FootballSeason season,
		LocalDate matchDate,
		BigDecimal homeExpectedGoals,
		BigDecimal awayExpectedGoals,
		ScoreProbabilityDistribution scoreDistribution,
		MatchScore actualScore) {

	public HistoricalPredictionSnapshot {
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("eventId is required");
		}
		if (season == null) {
			throw new IllegalArgumentException("season is required");
		}
		if (matchDate == null) {
			throw new IllegalArgumentException("matchDate is required");
		}
		if (homeExpectedGoals == null) {
			throw new IllegalArgumentException("homeExpectedGoals is required");
		}
		if (awayExpectedGoals == null) {
			throw new IllegalArgumentException("awayExpectedGoals is required");
		}
		if (scoreDistribution == null) {
			throw new IllegalArgumentException("scoreDistribution is required");
		}
		if (actualScore == null) {
			throw new IllegalArgumentException("actualScore is required");
		}
		homeExpectedGoals = homeExpectedGoals.stripTrailingZeros();
		awayExpectedGoals = awayExpectedGoals.stripTrailingZeros();
	}
}
