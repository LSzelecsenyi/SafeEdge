package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record DecileSnapshot(String key, int n, BigDecimal averageEdge, BigDecimal unitStakeRoi) {

	public DecileSnapshot {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("key is required");
		}
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		averageEdge = strip(averageEdge);
		unitStakeRoi = strip(unitStakeRoi);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
