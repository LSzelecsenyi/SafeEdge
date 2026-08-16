package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record LowScoreCellCalibration(
		String scoreline, BigDecimal averagePredicted, BigDecimal actualFrequency, int actualCount) {

	public LowScoreCellCalibration {
		if (scoreline == null || scoreline.isBlank()) {
			throw new IllegalArgumentException("scoreline is required");
		}
		if (actualCount < 0) {
			throw new IllegalArgumentException("actualCount must be >= 0");
		}
		averagePredicted = strip(averagePredicted);
		actualFrequency = strip(actualFrequency);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
