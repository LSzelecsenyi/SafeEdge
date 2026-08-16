package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.util.List;

public record LeagueCoverageSnapshot(
		CanonicalCompetition competition,
		HistoricalQuoteSource quoteSource,
		List<Integer> expectedWarmupStartYears,
		List<Integer> expectedEvaluationStartYears,
		List<Integer> missingWarmupStartYears,
		List<Integer> missingEvaluationStartYears,
		List<LeagueSeasonCoverageRow> seasons,
		int warmupMatchCount,
		int evaluationMatchCount,
		int evaluationMatchesWithSelectedQuote) {

	public LeagueCoverageSnapshot {
		if (competition == null) {
			throw new IllegalArgumentException("competition is required");
		}
		if (quoteSource == null) {
			throw new IllegalArgumentException("quoteSource is required");
		}
		expectedWarmupStartYears = List.copyOf(expectedWarmupStartYears == null ? List.of() : expectedWarmupStartYears);
		expectedEvaluationStartYears =
				List.copyOf(expectedEvaluationStartYears == null ? List.of() : expectedEvaluationStartYears);
		missingWarmupStartYears = List.copyOf(missingWarmupStartYears == null ? List.of() : missingWarmupStartYears);
		missingEvaluationStartYears =
				List.copyOf(missingEvaluationStartYears == null ? List.of() : missingEvaluationStartYears);
		seasons = List.copyOf(seasons == null ? List.of() : seasons);
	}

	public boolean evaluationSeasonsComplete() {
		return missingEvaluationStartYears.isEmpty();
	}

	public boolean warmupSeasonsComplete() {
		return missingWarmupStartYears.isEmpty();
	}

	public int evaluationMatchesMissingSelectedQuote() {
		return Math.max(0, evaluationMatchCount - evaluationMatchesWithSelectedQuote);
	}
}
