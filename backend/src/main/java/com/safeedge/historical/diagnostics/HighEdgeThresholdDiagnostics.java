package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.util.List;

public record HighEdgeThresholdDiagnostics(
		BigDecimal threshold,
		EdgeQualityGroupSummary summary,
		int homeCount,
		int awayCount,
		List<ConcentrationShare> byAhLine,
		List<ConcentrationShare> bySeason) {

	public HighEdgeThresholdDiagnostics {
		if (threshold == null) {
			throw new IllegalArgumentException("threshold is required");
		}
		if (summary == null) {
			throw new IllegalArgumentException("summary is required");
		}
		threshold = threshold.stripTrailingZeros();
		byAhLine = List.copyOf(byAhLine == null ? List.of() : byAhLine);
		bySeason = List.copyOf(bySeason == null ? List.of() : bySeason);
	}
}
