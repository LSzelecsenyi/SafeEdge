package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record SeasonStabilityRow(
		String seasonDisplay,
		int candidateCount,
		BigDecimal averageEdge,
		BigDecimal unitStakeRoi,
		int positiveEdgeCount,
		BigDecimal positiveEdgeRoi,
		int edgeAtLeast03Count,
		BigDecimal edgeAtLeast03Roi,
		int edgeAtLeast10Count,
		BigDecimal edgeAtLeast10Roi) {

	public SeasonStabilityRow {
		if (seasonDisplay == null || seasonDisplay.isBlank()) {
			throw new IllegalArgumentException("seasonDisplay is required");
		}
		averageEdge = strip(averageEdge);
		unitStakeRoi = strip(unitStakeRoi);
		positiveEdgeRoi = strip(positiveEdgeRoi);
		edgeAtLeast03Roi = strip(edgeAtLeast03Roi);
		edgeAtLeast10Roi = strip(edgeAtLeast10Roi);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
