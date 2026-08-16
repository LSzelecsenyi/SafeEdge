package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record PositiveLineForensics(
		BigDecimal selectedLine,
		EdgeQualityGroupSummary all,
		EdgeQualityGroupSummary positiveEdgeOnly,
		EdgeQualityGroupSummary edgeAtLeast03,
		EdgeQualityGroupSummary edgeAtLeast10) {

	public PositiveLineForensics {
		if (selectedLine == null) {
			throw new IllegalArgumentException("selectedLine is required");
		}
		if (all == null || positiveEdgeOnly == null || edgeAtLeast03 == null || edgeAtLeast10 == null) {
			throw new IllegalArgumentException("subset summaries are required");
		}
		selectedLine = selectedLine.stripTrailingZeros();
	}
}
