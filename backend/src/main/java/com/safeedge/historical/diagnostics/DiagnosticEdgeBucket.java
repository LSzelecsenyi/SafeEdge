package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Fixed candidate-edge buckets. Thresholds are diagnostic only and must not be
 * retuned from observed results.
 */
public enum DiagnosticEdgeBucket {
	NON_POSITIVE("edge <= 0"),
	OPEN_0_TO_02("0 < edge < 0.02"),
	FROM_02_TO_05("0.02 <= edge < 0.05"),
	FROM_05_TO_10("0.05 <= edge < 0.10"),
	FROM_10("edge >= 0.10");

	private static final BigDecimal TWO_PERCENT = new BigDecimal("0.02");
	private static final BigDecimal FIVE_PERCENT = new BigDecimal("0.05");
	private static final BigDecimal TEN_PERCENT = new BigDecimal("0.10");

	private final String label;

	DiagnosticEdgeBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static DiagnosticEdgeBucket of(BigDecimal edge) {
		if (edge == null) {
			throw new IllegalArgumentException("edge is required");
		}
		if (edge.compareTo(BigDecimal.ZERO) <= 0) {
			return NON_POSITIVE;
		}
		if (edge.compareTo(TWO_PERCENT) < 0) {
			return OPEN_0_TO_02;
		}
		if (edge.compareTo(FIVE_PERCENT) < 0) {
			return FROM_02_TO_05;
		}
		if (edge.compareTo(TEN_PERCENT) < 0) {
			return FROM_05_TO_10;
		}
		return FROM_10;
	}
}
