package com.safeedge.strategy;

import java.math.BigDecimal;

/**
 * Current committed stake amounts before this opportunity. Values are money,
 * not rates. Amounts may already exceed configured limits; remaining capacity
 * is then zero.
 */
public record PortfolioExposure(
		BigDecimal matchExposureAmount,
		BigDecimal leagueExposureAmount,
		BigDecimal dailyExposureAmount) {

	public static PortfolioExposure none() {
		return new PortfolioExposure(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
	}

	public PortfolioExposure {
		matchExposureAmount = requireNonNegative(matchExposureAmount, "matchExposureAmount");
		leagueExposureAmount = requireNonNegative(leagueExposureAmount, "leagueExposureAmount");
		dailyExposureAmount = requireNonNegative(dailyExposureAmount, "dailyExposureAmount");
	}

	private static BigDecimal requireNonNegative(BigDecimal value, String name) {
		if (value == null) {
			throw new StrategyException(name + " is required");
		}
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new StrategyException(name + " cannot be negative");
		}
		return value.stripTrailingZeros();
	}

}
