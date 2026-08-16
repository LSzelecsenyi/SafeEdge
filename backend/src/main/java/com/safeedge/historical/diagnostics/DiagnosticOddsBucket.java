package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;

/**
 * Fixed decimal-odds buckets. Diagnostic only; not a production filter.
 */
public enum DiagnosticOddsBucket {
	OPEN_1_TO_120("1.00 < odds < 1.20"),
	FROM_120_TO_135("1.20 <= odds < 1.35"),
	FROM_135_TO_150("1.35 <= odds < 1.50"),
	FROM_150_TO_175("1.50 <= odds < 1.75"),
	FROM_175_TO_200("1.75 <= odds < 2.00"),
	FROM_200_TO_250("2.00 <= odds < 2.50"),
	FROM_250("odds >= 2.50");

	private static final BigDecimal ONE_TWENTY = new BigDecimal("1.20");
	private static final BigDecimal ONE_THIRTY_FIVE = new BigDecimal("1.35");
	private static final BigDecimal ONE_FIFTY = new BigDecimal("1.50");
	private static final BigDecimal ONE_SEVENTY_FIVE = new BigDecimal("1.75");
	private static final BigDecimal TWO = new BigDecimal("2.00");
	private static final BigDecimal TWO_FIFTY = new BigDecimal("2.50");

	private final String label;

	DiagnosticOddsBucket(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static DiagnosticOddsBucket of(BigDecimal odds) {
		if (odds == null) {
			throw new IllegalArgumentException("odds are required");
		}
		if (odds.compareTo(BigDecimal.ONE) <= 0) {
			throw new IllegalArgumentException("odds must be greater than 1");
		}
		if (odds.compareTo(ONE_TWENTY) < 0) {
			return OPEN_1_TO_120;
		}
		if (odds.compareTo(ONE_THIRTY_FIVE) < 0) {
			return FROM_120_TO_135;
		}
		if (odds.compareTo(ONE_FIFTY) < 0) {
			return FROM_135_TO_150;
		}
		if (odds.compareTo(ONE_SEVENTY_FIVE) < 0) {
			return FROM_150_TO_175;
		}
		if (odds.compareTo(TWO) < 0) {
			return FROM_175_TO_200;
		}
		if (odds.compareTo(TWO_FIFTY) < 0) {
			return FROM_200_TO_250;
		}
		return FROM_250;
	}
}
