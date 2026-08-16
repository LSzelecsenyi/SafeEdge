package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record ExactMarginCalibration(
		DiagnosticExactMarginBucket bucket,
		BigDecimal averagePredictedProbability,
		BigDecimal actualFrequency,
		int actualCount) {

	public ExactMarginCalibration {
		if (bucket == null) {
			throw new IllegalArgumentException("bucket is required");
		}
		if (actualCount < 0) {
			throw new IllegalArgumentException("actualCount must be >= 0");
		}
		averagePredictedProbability = strip(averagePredictedProbability);
		actualFrequency = strip(actualFrequency);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
