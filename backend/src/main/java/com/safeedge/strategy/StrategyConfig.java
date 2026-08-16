package com.safeedge.strategy;

import java.math.BigDecimal;

/**
 * Immutable staking and risk configuration. Future strategy execution must read
 * these values only; it must not branch on {@link StrategyPreset}.
 *
 * All rates are decimal fractions (30% is {@code 0.30}). This object is
 * configuration, not live bankroll, Vault, exposure, or drawdown state.
 */
public record StrategyConfig(
		boolean vaultEnabled,
		BigDecimal vaultSweepRate,
		StakingMode stakingMode,
		BigDecimal kellyFraction,
		BigDecimal flatStakeRate,
		BigDecimal maxStakeRate,
		BigDecimal minimumEdge,
		BigDecimal maxMatchExposure,
		BigDecimal maxLeagueExposure,
		BigDecimal maxDailyExposure,
		BigDecimal drawdownWarningThreshold,
		BigDecimal drawdownReductionThreshold,
		BigDecimal drawdownStakeMultiplier,
		BigDecimal drawdownStopThreshold) {

	public StrategyConfig {
		if (stakingMode == null) {
			throw new StrategyException("Staking mode is required");
		}
		validateVault(vaultEnabled, vaultSweepRate);
		validateStaking(stakingMode, kellyFraction, flatStakeRate);
		requireExclusive(maxStakeRate, BigDecimal.ZERO, BigDecimal.ONE, "maxStakeRate");
		requireInclusive(minimumEdge, BigDecimal.ZERO, BigDecimal.ONE, "minimumEdge");
		requireExclusive(maxMatchExposure, BigDecimal.ZERO, BigDecimal.ONE, "maxMatchExposure");
		requireExclusive(maxLeagueExposure, BigDecimal.ZERO, BigDecimal.ONE, "maxLeagueExposure");
		requireExclusive(maxDailyExposure, BigDecimal.ZERO, BigDecimal.ONE, "maxDailyExposure");
		if (maxMatchExposure.compareTo(maxLeagueExposure) > 0) {
			throw new StrategyException("maxMatchExposure must be <= maxLeagueExposure");
		}
		if (maxLeagueExposure.compareTo(maxDailyExposure) > 0) {
			throw new StrategyException("maxLeagueExposure must be <= maxDailyExposure");
		}
		validateDrawdown(
				drawdownWarningThreshold,
				drawdownReductionThreshold,
				drawdownStakeMultiplier,
				drawdownStopThreshold);
		vaultSweepRate = normalized(vaultSweepRate);
		kellyFraction = normalizedNullable(kellyFraction);
		flatStakeRate = normalizedNullable(flatStakeRate);
		maxStakeRate = normalized(maxStakeRate);
		minimumEdge = normalized(minimumEdge);
		maxMatchExposure = normalized(maxMatchExposure);
		maxLeagueExposure = normalized(maxLeagueExposure);
		maxDailyExposure = normalized(maxDailyExposure);
		drawdownWarningThreshold = normalized(drawdownWarningThreshold);
		drawdownReductionThreshold = normalized(drawdownReductionThreshold);
		drawdownStakeMultiplier = normalized(drawdownStakeMultiplier);
		drawdownStopThreshold = normalized(drawdownStopThreshold);
	}

	private static void validateVault(boolean vaultEnabled, BigDecimal vaultSweepRate) {
		requireNonNull(vaultSweepRate, "vaultSweepRate");
		if (vaultEnabled) {
			if (vaultSweepRate.compareTo(BigDecimal.ZERO) <= 0 || vaultSweepRate.compareTo(BigDecimal.ONE) > 0) {
				throw new StrategyException("vaultSweepRate must be > 0 and <= 1 when vault is enabled");
			}
		}
		else if (vaultSweepRate.compareTo(BigDecimal.ZERO) != 0) {
			throw new StrategyException("vaultSweepRate must be 0 when vault is disabled");
		}
	}

	private static void validateStaking(StakingMode stakingMode, BigDecimal kellyFraction, BigDecimal flatStakeRate) {
		switch (stakingMode) {
			case FRACTIONAL_KELLY -> {
				if (isAbsent(kellyFraction)
						|| kellyFraction.compareTo(BigDecimal.ZERO) <= 0
						|| kellyFraction.compareTo(BigDecimal.ONE) > 0) {
					throw new StrategyException("kellyFraction must be > 0 and <= 1 for FRACTIONAL_KELLY");
				}
				if (!isAbsent(flatStakeRate)) {
					throw new StrategyException("flatStakeRate must be absent or 0 for FRACTIONAL_KELLY");
				}
			}
			case FLAT_STAKE -> {
				if (flatStakeRate == null || flatStakeRate.compareTo(BigDecimal.ZERO) <= 0) {
					throw new StrategyException("flatStakeRate must be > 0 for FLAT_STAKE");
				}
				if (!isAbsent(kellyFraction)) {
					throw new StrategyException("kellyFraction must be absent or 0 for FLAT_STAKE");
				}
			}
		}
	}

	private static void validateDrawdown(
			BigDecimal warning, BigDecimal reduction, BigDecimal multiplier, BigDecimal stop) {
		requireNonNull(warning, "drawdownWarningThreshold");
		requireNonNull(reduction, "drawdownReductionThreshold");
		requireNonNull(multiplier, "drawdownStakeMultiplier");
		requireNonNull(stop, "drawdownStopThreshold");
		if (warning.compareTo(BigDecimal.ZERO) < 0) {
			throw new StrategyException("drawdownWarningThreshold must be >= 0");
		}
		if (warning.compareTo(reduction) >= 0 || reduction.compareTo(stop) >= 0) {
			throw new StrategyException("drawdown thresholds must satisfy warning < reduction < stop");
		}
		if (stop.compareTo(BigDecimal.ONE) > 0) {
			throw new StrategyException("drawdownStopThreshold must be <= 1");
		}
		if (multiplier.compareTo(BigDecimal.ZERO) <= 0 || multiplier.compareTo(BigDecimal.ONE) > 0) {
			throw new StrategyException("drawdownStakeMultiplier must be > 0 and <= 1");
		}
	}

	private static void requireExclusive(BigDecimal value, BigDecimal exclusiveMin, BigDecimal max, String name) {
		requireNonNull(value, name);
		if (value.compareTo(exclusiveMin) <= 0 || value.compareTo(max) > 0) {
			throw new StrategyException(name + " must be > " + exclusiveMin.toPlainString() + " and <= " + max.toPlainString());
		}
	}

	private static void requireInclusive(BigDecimal value, BigDecimal min, BigDecimal max, String name) {
		requireNonNull(value, name);
		if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
			throw new StrategyException(name + " must be >= " + min.toPlainString() + " and <= " + max.toPlainString());
		}
	}

	private static boolean isAbsent(BigDecimal value) {
		return value == null || value.compareTo(BigDecimal.ZERO) == 0;
	}

	private static void requireNonNull(BigDecimal value, String name) {
		if (value == null) {
			throw new StrategyException(name + " is required");
		}
	}

	private static BigDecimal normalized(BigDecimal value) {
		return value.stripTrailingZeros();
	}

	private static BigDecimal normalizedNullable(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}

}
