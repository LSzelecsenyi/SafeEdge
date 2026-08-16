package com.safeedge.historical.diagnostics;

public record LowScoreCalibration(
		int predictionCount,
		LowScoreCellCalibration score00,
		LowScoreCellCalibration score10,
		LowScoreCellCalibration score01,
		LowScoreCellCalibration score11) {

	public LowScoreCalibration {
		if (predictionCount < 0) {
			throw new IllegalArgumentException("predictionCount must be >= 0");
		}
		if (score00 == null || score10 == null || score01 == null || score11 == null) {
			throw new IllegalArgumentException("all four low-score cells are required");
		}
	}
}
