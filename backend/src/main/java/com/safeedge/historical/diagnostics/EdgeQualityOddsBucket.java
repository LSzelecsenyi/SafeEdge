package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Diagnostic odds buckets for Baseline 002 edge×odds tables. Not a production
 * filter.
 */
public enum EdgeQualityOddsBucket {
	UNDER_175("odds < 1.75"),
	FROM_175_TO_190("1.75 <= odds < 1.90"),
	FROM_190_TO_200("1.90 <= odds < 2.00"),
	FROM_200_TO_210("2.00 <= odds < 2.10"),
	FROM_210_TO_225("2.10 <= odds < 2.25"),
	FROM_225("odds >= 2.25");

	private static final BigDecimal ONE_SEVENTY_FIVE = new BigDecimal("1.75");
	private static final BigDecimal ONE_NINETY = new BigDecimal("1.90");
	private static final BigDecimal TWO = new BigDecimal("2.00");
	private static final BigDecimal TWO_TEN = new BigDecimal("2.10");
	private static final BigDecimal TWO_TWENTY_FIVE = new BigDecimal("2.25");

	private final String label;

	EdgeQualityOddsBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static EdgeQualityOddsBucket of(BigDecimal odds) {
		if (odds == null) {
			throw new IllegalArgumentException("odds are required");
		}
		if (odds.compareTo(BigDecimal.ONE) <= 0) {
			throw new IllegalArgumentException("odds must be greater than 1");
		}
		if (odds.compareTo(ONE_SEVENTY_FIVE) < 0) {
			return UNDER_175;
		}
		if (odds.compareTo(ONE_NINETY) < 0) {
			return FROM_175_TO_190;
		}
		if (odds.compareTo(TWO) < 0) {
			return FROM_190_TO_200;
		}
		if (odds.compareTo(TWO_TEN) < 0) {
			return FROM_200_TO_210;
		}
		if (odds.compareTo(TWO_TWENTY_FIVE) < 0) {
			return FROM_210_TO_225;
		}
		return FROM_225;
	}
}
