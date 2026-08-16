package com.safeedge.strategy;

import java.math.BigDecimal;

/**
 * Supplies initial-hypothesis {@link StrategyConfig} values for predefined presets.
 *
 * Presets are data, not behavior. A future StrategyEngine must consume
 * {@link StrategyConfig} only and must not branch on {@link StrategyPreset}.
 *
 * These numbers are not optimized or proven. They are starting hypotheses for
 * later backtests. {@code drawdownStakeMultiplier = 0.50} is an initial
 * assumption because preset notes specify the reduction trigger, not a distinct
 * multiplier. Flat-stake drawdown thresholds currently copy Defensive as a
 * conservative control, not a designed optimum.
 */
public final class StrategyPresetFactory {

	public StrategyConfig configFor(StrategyPreset preset) {
		if (preset == null) {
			throw new StrategyException("Strategy preset is required");
		}
		return switch (preset) {
			case DEFENSIVE -> defensive();
			case BALANCED -> balanced();
			case GROWTH -> growth();
			case FLAT_STAKE -> flatStake();
		};
	}

	private static StrategyConfig defensive() {
		return new StrategyConfig(
				true,
				rate("0.30"),
				StakingMode.FRACTIONAL_KELLY,
				rate("0.25"),
				null,
				rate("0.02"),
				rate("0.03"),
				rate("0.03"),
				rate("0.05"),
				rate("0.10"),
				rate("0.10"),
				rate("0.15"),
				rate("0.50"),
				rate("0.20"));
	}

	private static StrategyConfig balanced() {
		return new StrategyConfig(
				true,
				rate("0.15"),
				StakingMode.FRACTIONAL_KELLY,
				rate("0.35"),
				null,
				rate("0.03"),
				rate("0.03"),
				rate("0.05"),
				rate("0.08"),
				rate("0.15"),
				rate("0.12"),
				rate("0.18"),
				rate("0.50"),
				rate("0.25"));
	}

	private static StrategyConfig growth() {
		return new StrategyConfig(
				false,
				rate("0"),
				StakingMode.FRACTIONAL_KELLY,
				rate("0.50"),
				null,
				rate("0.04"),
				rate("0.02"),
				rate("0.06"),
				rate("0.10"),
				rate("0.20"),
				rate("0.15"),
				rate("0.20"),
				rate("0.50"),
				rate("0.30"));
	}

	private static StrategyConfig flatStake() {
		return new StrategyConfig(
				false,
				rate("0"),
				StakingMode.FLAT_STAKE,
				null,
				rate("0.01"),
				rate("0.01"),
				rate("0.03"),
				rate("0.02"),
				rate("0.05"),
				rate("0.10"),
				rate("0.10"),
				rate("0.15"),
				rate("0.50"),
				rate("0.20"));
	}

	private static BigDecimal rate(String value) {
		return new BigDecimal(value);
	}

}
