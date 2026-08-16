package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record PredictionQualitySnapshot(
		int predictionsAvailable,
		int logLossObservations,
		BigDecimal averageActualScoreLogLoss,
		BigDecimal predictedHomeGoals,
		BigDecimal actualHomeGoals,
		BigDecimal predictedAwayGoals,
		BigDecimal actualAwayGoals,
		BigDecimal predictedHomeWin,
		BigDecimal actualHomeWin,
		BigDecimal predictedDraw,
		BigDecimal actualDraw,
		BigDecimal predictedAwayWin,
		BigDecimal actualAwayWin,
		java.util.List<MarginCategoryCalibration> marginCategories) {

	public PredictionQualitySnapshot {
		if (predictionsAvailable < 0 || logLossObservations < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		averageActualScoreLogLoss = strip(averageActualScoreLogLoss);
		predictedHomeGoals = strip(predictedHomeGoals);
		actualHomeGoals = strip(actualHomeGoals);
		predictedAwayGoals = strip(predictedAwayGoals);
		actualAwayGoals = strip(actualAwayGoals);
		predictedHomeWin = strip(predictedHomeWin);
		actualHomeWin = strip(actualHomeWin);
		predictedDraw = strip(predictedDraw);
		actualDraw = strip(actualDraw);
		predictedAwayWin = strip(predictedAwayWin);
		actualAwayWin = strip(actualAwayWin);
		marginCategories = java.util.List.copyOf(marginCategories == null ? java.util.List.of() : marginCategories);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
