package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Diagnostics-only v3 optimizer and parameter-distribution summary.
 */
public record JointDixonColesOptimizerSummary(
		int snapshotCount,
		int fittingFailures,
		int convergedCount,
		BigDecimal meanIterations,
		BigDecimal medianIterations,
		int maxIterationsObserved,
		EdgeQuantiles attack,
		EdgeQuantiles defence,
		EdgeQuantiles homeAdvantage,
		RhoSummary rho,
		boolean parametersFinite,
		boolean medianHomeAdvantagePositive) {

	public JointDixonColesOptimizerSummary {
		if (snapshotCount < 0 || fittingFailures < 0 || convergedCount < 0 || maxIterationsObserved < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		meanIterations = strip(meanIterations);
		medianIterations = strip(medianIterations);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
