package com.safeedge.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StrategyPresetFactoryTest {

	private final StrategyPresetFactory factory = new StrategyPresetFactory();

	@Test
	void defensivePresetValues() {
		assertConfig(
				factory.configFor(StrategyPreset.DEFENSIVE),
				true,
				"0.30",
				StakingMode.FRACTIONAL_KELLY,
				"0.25",
				null,
				"0.02",
				"0.03",
				"0.03",
				"0.05",
				"0.10",
				"0.10",
				"0.15",
				"0.50",
				"0.20");
	}

	@Test
	void balancedPresetValues() {
		assertConfig(
				factory.configFor(StrategyPreset.BALANCED),
				true,
				"0.15",
				StakingMode.FRACTIONAL_KELLY,
				"0.35",
				null,
				"0.03",
				"0.03",
				"0.05",
				"0.08",
				"0.15",
				"0.12",
				"0.18",
				"0.50",
				"0.25");
	}

	@Test
	void growthPresetValues() {
		assertConfig(
				factory.configFor(StrategyPreset.GROWTH),
				false,
				"0",
				StakingMode.FRACTIONAL_KELLY,
				"0.50",
				null,
				"0.04",
				"0.02",
				"0.06",
				"0.10",
				"0.20",
				"0.15",
				"0.20",
				"0.50",
				"0.30");
	}

	@Test
	void flatStakePresetValues() {
		assertConfig(
				factory.configFor(StrategyPreset.FLAT_STAKE),
				false,
				"0",
				StakingMode.FLAT_STAKE,
				null,
				"0.01",
				"0.01",
				"0.03",
				"0.02",
				"0.05",
				"0.10",
				"0.10",
				"0.15",
				"0.50",
				"0.20");
	}

	@Test
	void factoryReturnsIndependentEqualCopies() {
		StrategyConfig first = factory.configFor(StrategyPreset.DEFENSIVE);
		StrategyConfig second = factory.configFor(StrategyPreset.DEFENSIVE);
		assertThat(first).isNotSameAs(second).isEqualTo(second);
	}

	@Test
	void nullPresetIsRejected() {
		assertThatThrownBy(() -> factory.configFor(null)).isInstanceOf(StrategyException.class);
	}

	private static void assertConfig(
			StrategyConfig config,
			boolean vaultEnabled,
			String vaultSweepRate,
			StakingMode stakingMode,
			String kellyFraction,
			String flatStakeRate,
			String maxStakeRate,
			String minimumEdge,
			String maxMatchExposure,
			String maxLeagueExposure,
			String maxDailyExposure,
			String drawdownWarningThreshold,
			String drawdownReductionThreshold,
			String drawdownStakeMultiplier,
			String drawdownStopThreshold) {
		assertThat(config.vaultEnabled()).isEqualTo(vaultEnabled);
		assertRate(config.vaultSweepRate(), vaultSweepRate);
		assertThat(config.stakingMode()).isEqualTo(stakingMode);
		assertNullableRate(config.kellyFraction(), kellyFraction);
		assertNullableRate(config.flatStakeRate(), flatStakeRate);
		assertRate(config.maxStakeRate(), maxStakeRate);
		assertRate(config.minimumEdge(), minimumEdge);
		assertRate(config.maxMatchExposure(), maxMatchExposure);
		assertRate(config.maxLeagueExposure(), maxLeagueExposure);
		assertRate(config.maxDailyExposure(), maxDailyExposure);
		assertRate(config.drawdownWarningThreshold(), drawdownWarningThreshold);
		assertRate(config.drawdownReductionThreshold(), drawdownReductionThreshold);
		assertRate(config.drawdownStakeMultiplier(), drawdownStakeMultiplier);
		assertRate(config.drawdownStopThreshold(), drawdownStopThreshold);
	}

	private static void assertRate(BigDecimal actual, String expected) {
		assertThat(actual).isEqualByComparingTo(expected);
	}

	private static void assertNullableRate(BigDecimal actual, String expected) {
		if (expected == null) {
			assertThat(actual).isNull();
			return;
		}
		assertThat(actual).isEqualByComparingTo(expected);
	}

}
