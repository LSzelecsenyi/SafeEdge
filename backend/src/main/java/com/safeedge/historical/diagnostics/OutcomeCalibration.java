package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record OutcomeCalibration(
		BigDecimal averagePredictedProbability, BigDecimal actualFrequency, BigDecimal gap) {

	public OutcomeCalibration {
		averagePredictedProbability = strip(averagePredictedProbability);
		actualFrequency = strip(actualFrequency);
		gap = strip(gap);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
