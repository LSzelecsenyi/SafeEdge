package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Compact v1 or v2 league diagnostics for Probability Model comparison.
 * Strength and rho fields are populated for v2 only.
 */
public record ProbabilityModelV2LeagueMetrics(
		int predictionsAvailable,
		int candidateCount,
		BigDecimal scoreLogLoss,
		int logLossObservations,
		GoalCalibrationDiagnostics goalCalibration,
		List<MarginCategoryCalibration> marginCategories,
		LowScoreCalibration lowScores,
		RankQualityStats rankQuality,
		List<DecileSnapshot> edgeDeciles,
		int decileRoiInversions,
		HighEdgeCalibrationSnapshot highEdge10,
		HighEdgeCalibrationSnapshot highEdge20,
		HighEdgeCalibrationSnapshot highEdge30,
		BigDecimal meanPredictedEdge,
		BigDecimal realizedUnitRoi,
		EdgeQuantiles predictedEdge,
		EdgeQuantiles predictedWin,
		EdgeQuantiles predictedLoss,
		EdgeQuantiles lambdaHome,
		EdgeQuantiles lambdaAway,
		List<StrengthQuantileReport> strengths,
		RhoSummary rho,
		List<StrategySecondarySnapshot> strategies) {

	public ProbabilityModelV2LeagueMetrics {
		if (predictionsAvailable < 0 || candidateCount < 0 || logLossObservations < 0 || decileRoiInversions < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		if (rankQuality == null) {
			throw new IllegalArgumentException("rankQuality is required");
		}
		scoreLogLoss = strip(scoreLogLoss);
		meanPredictedEdge = strip(meanPredictedEdge);
		realizedUnitRoi = strip(realizedUnitRoi);
		marginCategories = copy(marginCategories);
		edgeDeciles = copy(edgeDeciles);
		strengths = copy(strengths);
		strategies = copy(strategies);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}

	private static <T> List<T> copy(List<T> values) {
		return List.copyOf(values == null ? List.of() : values);
	}
}
