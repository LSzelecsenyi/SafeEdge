package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * High-edge settlement calibration for all five AH outcomes. Diagnostics only.
 */
public record HighEdgeFiveWaySnapshot(
		BigDecimal threshold,
		int n,
		BigDecimal averageEdge,
		BigDecimal unitStakeRoi,
		OutcomeCalibration win,
		OutcomeCalibration halfWin,
		OutcomeCalibration push,
		OutcomeCalibration halfLoss,
		OutcomeCalibration loss) {

	public HighEdgeFiveWaySnapshot {
		if (threshold == null) {
			throw new IllegalArgumentException("threshold is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		if (win == null || halfWin == null || push == null || halfLoss == null || loss == null) {
			throw new IllegalArgumentException("all five settlement calibrations are required");
		}
		threshold = threshold.stripTrailingZeros();
		averageEdge = strip(averageEdge);
		unitStakeRoi = strip(unitStakeRoi);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
