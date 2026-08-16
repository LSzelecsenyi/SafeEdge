package com.safeedge.settlement;

import java.math.BigDecimal;

/**
 * Canonical Asian Handicap line helpers. {@code BettingMarket.line} and the HOME
 * selection line are the home-side handicap; AWAY is its negation.
 */
public final class AsianHandicapLines {

	private static final BigDecimal QUARTER = new BigDecimal("0.25");

	private AsianHandicapLines() {
	}

	public static boolean isSupportedIncrement(BigDecimal line) {
		return line != null && line.remainder(QUARTER).compareTo(BigDecimal.ZERO) == 0;
	}

	public static void requireSupportedIncrement(BigDecimal line) {
		if (!isSupportedIncrement(line)) {
			throw new SettlementException(
					"Asian handicap line must be a multiple of 0.25, not "
							+ (line == null ? "null" : line.toPlainString()));
		}
	}

	public static BigDecimal awayLine(BigDecimal homeLine) {
		if (homeLine == null) {
			throw new SettlementException("Asian handicap home line is required");
		}
		return homeLine.negate();
	}

}
