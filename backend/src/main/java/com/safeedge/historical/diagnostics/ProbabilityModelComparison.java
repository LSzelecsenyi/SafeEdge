package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;

/**
 * Diagnostics-only v1 vs v2 comparison for one development league. Not consumed
 * by CandidateEngine or BacktestEngine.
 */
public record ProbabilityModelComparison(
		CanonicalCompetition competition,
		ProbabilityModelV2LeagueMetrics v1,
		ProbabilityModelV2LeagueMetrics v2,
		EdgeQuantiles lambdaHomeDelta,
		EdgeQuantiles lambdaAwayDelta) {

	public ProbabilityModelComparison {
		if (competition == null) {
			throw new IllegalArgumentException("competition is required");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(competition);
		if (v1 == null || v2 == null) {
			throw new IllegalArgumentException("v1 and v2 metrics are required");
		}
	}

	public BigDecimal spearmanDelta() {
		return subtract(v2.rankQuality().spearman(), v1.rankQuality().spearman());
	}

	public BigDecimal pearsonDelta() {
		return subtract(v2.rankQuality().pearson(), v1.rankQuality().pearson());
	}

	public BigDecimal logLossDelta() {
		return subtract(v2.scoreLogLoss(), v1.scoreLogLoss());
	}

	private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
		if (left == null || right == null) {
			return null;
		}
		return left.subtract(right);
	}
}
