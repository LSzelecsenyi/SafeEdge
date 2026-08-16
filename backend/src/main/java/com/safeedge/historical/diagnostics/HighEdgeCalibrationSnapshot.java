package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record HighEdgeCalibrationSnapshot(
		BigDecimal threshold,
		int n,
		BigDecimal averageEdge,
		BigDecimal unitStakeRoi,
		BigDecimal predictedWin,
		BigDecimal actualWin,
		BigDecimal predictedLoss,
		BigDecimal actualLoss) {

	public HighEdgeCalibrationSnapshot {
		if (threshold == null) {
			throw new IllegalArgumentException("threshold is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		threshold = threshold.stripTrailingZeros();
		averageEdge = strip(averageEdge);
		unitStakeRoi = strip(unitStakeRoi);
		predictedWin = strip(predictedWin);
		actualWin = strip(actualWin);
		predictedLoss = strip(predictedLoss);
		actualLoss = strip(actualLoss);
	}

	public BigDecimal winGap() {
		return subtract(predictedWin, actualWin);
	}

	public BigDecimal lossGap() {
		return subtract(predictedLoss, actualLoss);
	}

	public BigDecimal absWinGap() {
		return abs(winGap());
	}

	public BigDecimal absLossGap() {
		return abs(lossGap());
	}

	private static BigDecimal subtract(BigDecimal left, BigDecimal right) {
		if (left == null || right == null) {
			return null;
		}
		return left.subtract(right);
	}

	private static BigDecimal abs(BigDecimal value) {
		return value == null ? null : value.abs();
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
