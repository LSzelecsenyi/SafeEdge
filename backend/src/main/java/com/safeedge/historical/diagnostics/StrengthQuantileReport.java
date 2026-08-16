package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record StrengthQuantileReport(
		String name, EdgeQuantiles raw, EdgeQuantiles shrunk, BigDecimal rawMadFromOne, BigDecimal shrunkMadFromOne) {

	public StrengthQuantileReport {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		rawMadFromOne = strip(rawMadFromOne);
		shrunkMadFromOne = strip(shrunkMadFromOne);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
