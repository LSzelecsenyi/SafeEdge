package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Finer diagnostic edge buckets for Baseline 002. Not a production filter and
 * not a replacement for {@link DiagnosticEdgeBucket}.
 */
public enum EdgeQualityEdgeBucket {
	NON_POSITIVE("edge <= 0"),
	OPEN_0_TO_02("0 < edge < 0.02"),
	FROM_02_TO_05("0.02 <= edge < 0.05"),
	FROM_05_TO_10("0.05 <= edge < 0.10"),
	FROM_10_TO_20("0.10 <= edge < 0.20"),
	FROM_20_TO_30("0.20 <= edge < 0.30"),
	FROM_30("edge >= 0.30");

	private static final BigDecimal TWO = new BigDecimal("0.02");
	private static final BigDecimal FIVE = new BigDecimal("0.05");
	private static final BigDecimal TEN = new BigDecimal("0.10");
	private static final BigDecimal TWENTY = new BigDecimal("0.20");
	private static final BigDecimal THIRTY = new BigDecimal("0.30");

	private final String label;

	EdgeQualityEdgeBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static EdgeQualityEdgeBucket of(BigDecimal edge) {
		if (edge == null) {
			throw new IllegalArgumentException("edge is required");
		}
		if (edge.compareTo(BigDecimal.ZERO) <= 0) {
			return NON_POSITIVE;
		}
		if (edge.compareTo(TWO) < 0) {
			return OPEN_0_TO_02;
		}
		if (edge.compareTo(FIVE) < 0) {
			return FROM_02_TO_05;
		}
		if (edge.compareTo(TEN) < 0) {
			return FROM_05_TO_10;
		}
		if (edge.compareTo(TWENTY) < 0) {
			return FROM_10_TO_20;
		}
		if (edge.compareTo(THIRTY) < 0) {
			return FROM_20_TO_30;
		}
		return FROM_30;
	}
}
