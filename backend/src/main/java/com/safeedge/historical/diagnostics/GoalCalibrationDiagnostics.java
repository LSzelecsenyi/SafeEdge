package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record GoalCalibrationDiagnostics(
		int predictionCount,
		BigDecimal averagePredictedHomeGoals,
		BigDecimal averageActualHomeGoals,
		BigDecimal averagePredictedAwayGoals,
		BigDecimal averageActualAwayGoals,
		BigDecimal averagePredictedTotalGoals,
		BigDecimal averageActualTotalGoals,
		BigDecimal averagePredictedHomeWinProbability,
		BigDecimal actualHomeWinFrequency,
		BigDecimal averagePredictedDrawProbability,
		BigDecimal actualDrawFrequency,
		BigDecimal averagePredictedAwayWinProbability,
		BigDecimal actualAwayWinFrequency) {

	public GoalCalibrationDiagnostics {
		if (predictionCount < 0) {
			throw new IllegalArgumentException("predictionCount must be >= 0");
		}
		averagePredictedHomeGoals = strip(averagePredictedHomeGoals);
		averageActualHomeGoals = strip(averageActualHomeGoals);
		averagePredictedAwayGoals = strip(averagePredictedAwayGoals);
		averageActualAwayGoals = strip(averageActualAwayGoals);
		averagePredictedTotalGoals = strip(averagePredictedTotalGoals);
		averageActualTotalGoals = strip(averageActualTotalGoals);
		averagePredictedHomeWinProbability = strip(averagePredictedHomeWinProbability);
		actualHomeWinFrequency = strip(actualHomeWinFrequency);
		averagePredictedDrawProbability = strip(averagePredictedDrawProbability);
		actualDrawFrequency = strip(actualDrawFrequency);
		averagePredictedAwayWinProbability = strip(averagePredictedAwayWinProbability);
		actualAwayWinFrequency = strip(actualAwayWinFrequency);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
