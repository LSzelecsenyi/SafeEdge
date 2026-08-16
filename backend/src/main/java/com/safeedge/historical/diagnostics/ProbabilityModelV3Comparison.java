package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.math.BigDecimal;

/**
 * Diagnostics-only v1 vs v2 vs v3 comparison for one development league.
 */
public record ProbabilityModelV3Comparison(
		CanonicalCompetition competition,
		ProbabilityModelV2LeagueMetrics v1,
		ProbabilityModelV2LeagueMetrics v2,
		ProbabilityModelV2LeagueMetrics v3,
		ProbabilityModelV3Extras v1Extra,
		ProbabilityModelV3Extras v2Extra,
		ProbabilityModelV3Extras v3Extra) {

	public ProbabilityModelV3Comparison {
		if (competition == null) {
			throw new IllegalArgumentException("competition is required");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(competition);
		if (v1 == null || v2 == null || v3 == null) {
			throw new IllegalArgumentException("v1, v2, and v3 metrics are required");
		}
		if (v1Extra == null || v2Extra == null || v3Extra == null) {
			throw new IllegalArgumentException("v1, v2, and v3 extras are required");
		}
	}

	public BigDecimal betterBaselineSpearman() {
		return max(v1.rankQuality().spearman(), v2.rankQuality().spearman());
	}

	public BigDecimal betterBaselineLogLoss() {
		return min(v1.scoreLogLoss(), v2.scoreLogLoss());
	}

	public BigDecimal betterBaselineAbsWinGap10() {
		return min(v1.highEdge10().absWinGap(), v2.highEdge10().absWinGap());
	}

	public BigDecimal betterBaselineAbsLossGap10() {
		return min(v1.highEdge10().absLossGap(), v2.highEdge10().absLossGap());
	}

	public BigDecimal spearmanDeltaVsBetterBaseline() {
		return subtract(v3.rankQuality().spearman(), betterBaselineSpearman());
	}

	public BigDecimal logLossDeltaVsBetterBaseline() {
		return subtract(v3.scoreLogLoss(), betterBaselineLogLoss());
	}

	public BigDecimal winGap10ShrinkVsBetterBaseline() {
		return subtract(betterBaselineAbsWinGap10(), v3.highEdge10().absWinGap());
	}

	public BigDecimal lossGap10ShrinkVsBetterBaseline() {
		return subtract(betterBaselineAbsLossGap10(), v3.highEdge10().absLossGap());
	}

	private static BigDecimal max(BigDecimal left, BigDecimal right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return left.compareTo(right) >= 0 ? left : right;
	}

	private static BigDecimal min(BigDecimal left, BigDecimal right) {
		if (left == null) {
			return right;
		}
		if (right == null) {
			return left;
		}
		return left.compareTo(right) <= 0 ? left : right;
	}

	private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
		if (left == null || right == null) {
			return null;
		}
		return left.subtract(right);
	}
}
