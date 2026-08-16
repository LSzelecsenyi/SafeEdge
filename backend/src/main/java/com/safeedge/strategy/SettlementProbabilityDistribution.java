package com.safeedge.strategy;

import java.math.BigDecimal;

/**
 * Provider-independent probabilities for the five {@code SettlementResult} outcomes.
 * Values are not silently normalized; they must already sum to 1.
 */
public record SettlementProbabilityDistribution(
		BigDecimal winProbability,
		BigDecimal halfWinProbability,
		BigDecimal pushProbability,
		BigDecimal halfLossProbability,
		BigDecimal lossProbability) {

	public SettlementProbabilityDistribution {
		winProbability = requireProbability(winProbability, "winProbability");
		halfWinProbability = requireProbability(halfWinProbability, "halfWinProbability");
		pushProbability = requireProbability(pushProbability, "pushProbability");
		halfLossProbability = requireProbability(halfLossProbability, "halfLossProbability");
		lossProbability = requireProbability(lossProbability, "lossProbability");
		BigDecimal sum = winProbability
				.add(halfWinProbability)
				.add(pushProbability)
				.add(halfLossProbability)
				.add(lossProbability);
		if (sum.compareTo(BigDecimal.ONE) != 0) {
			throw new StrategyException("Settlement probabilities must sum to 1");
		}
	}

	public static SettlementProbabilityDistribution binary(BigDecimal winProbability) {
		BigDecimal win = requireProbability(winProbability, "winProbability");
		return new SettlementProbabilityDistribution(
				win,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ONE.subtract(win));
	}

	private static BigDecimal requireProbability(BigDecimal value, String name) {
		if (value == null) {
			throw new StrategyException(name + " is required");
		}
		if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
			throw new StrategyException(name + " must be >= 0 and <= 1");
		}
		return value.stripTrailingZeros();
	}

}
