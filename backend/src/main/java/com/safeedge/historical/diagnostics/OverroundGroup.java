package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record OverroundGroup(
		String key, int eventCount, BigDecimal averageOverround, BigDecimal medianOverround, boolean lowSample) {

	public OverroundGroup {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("key is required");
		}
		if (eventCount < 0) {
			throw new IllegalArgumentException("eventCount must be >= 0");
		}
		averageOverround = averageOverround == null ? null : averageOverround.stripTrailingZeros();
		medianOverround = medianOverround == null ? null : medianOverround.stripTrailingZeros();
	}
}
