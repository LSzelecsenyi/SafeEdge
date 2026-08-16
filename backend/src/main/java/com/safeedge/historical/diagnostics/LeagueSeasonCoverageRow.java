package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record LeagueSeasonCoverageRow(
		int seasonStartYear,
		String seasonDisplay,
		int totalMatches,
		int matchesWithSelectedQuote,
		BigDecimal selectedQuoteCoverageRate,
		boolean warmupHistory,
		boolean evaluationWindow) {

	public LeagueSeasonCoverageRow {
		if (seasonDisplay == null || seasonDisplay.isBlank()) {
			throw new IllegalArgumentException("seasonDisplay is required");
		}
		if (totalMatches < 0 || matchesWithSelectedQuote < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		selectedQuoteCoverageRate =
				selectedQuoteCoverageRate == null ? null : selectedQuoteCoverageRate.stripTrailingZeros();
	}
}
