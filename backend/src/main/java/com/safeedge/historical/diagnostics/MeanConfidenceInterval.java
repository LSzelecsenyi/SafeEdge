package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

public record MeanConfidenceInterval(
		int n, long bootstrapReplicates, long seed, BigDecimal mean, BigDecimal lower95, BigDecimal upper95) {

	public MeanConfidenceInterval {
		if (n < 0) {
			throw new IllegalArgumentException("n must be >= 0");
		}
		if (bootstrapReplicates < 0) {
			throw new IllegalArgumentException("bootstrapReplicates must be >= 0");
		}
		mean = strip(mean);
		lower95 = strip(lower95);
		upper95 = strip(upper95);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
