package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record RhoSummary(int n, BigDecimal min, BigDecimal median, BigDecimal max) {

	public RhoSummary {
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		min = strip(min);
		median = strip(median);
		max = strip(max);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
