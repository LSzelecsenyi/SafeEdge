package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record HighEdgeCalibrationSlice(
		BigDecimal threshold,
		int n,
		BigDecimal averageEdge,
		BigDecimal unitStakeRoi,
		BigDecimal predictedWinProbability,
		BigDecimal actualWinFrequency,
		BigDecimal predictedLossProbability,
		BigDecimal actualLossFrequency) {

	public HighEdgeCalibrationSlice {
		if (threshold == null) {
			throw new IllegalArgumentException("threshold is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		threshold = threshold.stripTrailingZeros();
		averageEdge = strip(averageEdge);
		unitStakeRoi = strip(unitStakeRoi);
		predictedWinProbability = strip(predictedWinProbability);
		actualWinFrequency = strip(actualWinFrequency);
		predictedLossProbability = strip(predictedLossProbability);
		actualLossFrequency = strip(actualLossFrequency);
	}

	boolean winOverconfident() {
		if (predictedWinProbability == null || actualWinFrequency == null || n == 0) {
			return false;
		}
		return predictedWinProbability.compareTo(actualWinFrequency) > 0;
	}

	boolean lossUnderconfident() {
		if (predictedLossProbability == null || actualLossFrequency == null || n == 0) {
			return false;
		}
		return predictedLossProbability.compareTo(actualLossFrequency) < 0;
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
