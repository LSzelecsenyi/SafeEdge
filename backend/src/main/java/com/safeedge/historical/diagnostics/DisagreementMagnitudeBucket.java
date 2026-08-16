package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Magnitude of model-vs-market disagreement, using {@code |predicted edge|} as
 * the diagnostic (edge is already model expected return at the quoted price).
 */
public enum DisagreementMagnitudeBucket {
	UNDER_02("|edge| < 0.02"),
	FROM_02_TO_05("0.02 <= |edge| < 0.05"),
	FROM_05_TO_10("0.05 <= |edge| < 0.10"),
	FROM_10_TO_20("0.10 <= |edge| < 0.20"),
	FROM_20_TO_30("0.20 <= |edge| < 0.30"),
	FROM_30("|edge| >= 0.30");

	private static final BigDecimal TWO = new BigDecimal("0.02");
	private static final BigDecimal FIVE = new BigDecimal("0.05");
	private static final BigDecimal TEN = new BigDecimal("0.10");
	private static final BigDecimal TWENTY = new BigDecimal("0.20");
	private static final BigDecimal THIRTY = new BigDecimal("0.30");

	private final String label;

	DisagreementMagnitudeBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static DisagreementMagnitudeBucket ofAbsoluteEdge(BigDecimal absoluteEdge) {
		if (absoluteEdge == null || absoluteEdge.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("absoluteEdge must be >= 0");
		}
		if (absoluteEdge.compareTo(TWO) < 0) {
			return UNDER_02;
		}
		if (absoluteEdge.compareTo(FIVE) < 0) {
			return FROM_02_TO_05;
		}
		if (absoluteEdge.compareTo(TEN) < 0) {
			return FROM_05_TO_10;
		}
		if (absoluteEdge.compareTo(TWENTY) < 0) {
			return FROM_10_TO_20;
		}
		if (absoluteEdge.compareTo(THIRTY) < 0) {
			return FROM_20_TO_30;
		}
		return FROM_30;
	}
}
