package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record OverroundSeasonRow(String seasonDisplay, int eventCount, BigDecimal averageOverround) {

	public OverroundSeasonRow {
		if (seasonDisplay == null || seasonDisplay.isBlank()) {
			throw new IllegalArgumentException("seasonDisplay is required");
		}
		if (eventCount < 0) {
			throw new IllegalArgumentException("eventCount must be >= 0");
		}
		averageOverround = averageOverround == null ? null : averageOverround.stripTrailingZeros();
	}
}
