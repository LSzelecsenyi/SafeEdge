package com.safeedge.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StrategyConfigTest {

	private final StrategyPresetFactory factory = new StrategyPresetFactory();

	@Test
	void validCustomConfigIsAcceptedAndIsNotAPreset() {
		StrategyConfig custom = custom(
				true,
				"0.10",
				StakingMode.FRACTIONAL_KELLY,
				"0.20",
				null,
				"0.025",
				"0.04",
				"0.04",
				"0.06",
				"0.12",
				"0.11",
				"0.16",
				"0.50",
				"0.22");
		assertThat(custom.vaultEnabled()).isTrue();
		assertThat(custom.vaultSweepRate()).isEqualByComparingTo("0.10");
		assertThat(custom.stakingMode()).isEqualTo(StakingMode.FRACTIONAL_KELLY);
		assertThat(custom.kellyFraction()).isEqualByComparingTo("0.20");
		assertThat(custom.flatStakeRate()).isNull();
		assertThat(custom.maxStakeRate()).isEqualByComparingTo("0.025");
		assertThat(custom.minimumEdge()).isEqualByComparingTo("0.04");
		assertThat(custom.maxMatchExposure()).isEqualByComparingTo("0.04");
		assertThat(custom.maxLeagueExposure()).isEqualByComparingTo("0.06");
		assertThat(custom.maxDailyExposure()).isEqualByComparingTo("0.12");
		assertThat(custom.drawdownWarningThreshold()).isEqualByComparingTo("0.11");
		assertThat(custom.drawdownReductionThreshold()).isEqualByComparingTo("0.16");
		assertThat(custom.drawdownStakeMultiplier()).isEqualByComparingTo("0.50");
		assertThat(custom.drawdownStopThreshold()).isEqualByComparingTo("0.22");
		assertThat(custom)
				.isNotEqualTo(factory.configFor(StrategyPreset.DEFENSIVE))
				.isNotEqualTo(factory.configFor(StrategyPreset.BALANCED))
				.isNotEqualTo(factory.configFor(StrategyPreset.GROWTH))
				.isNotEqualTo(factory.configFor(StrategyPreset.FLAT_STAKE));
	}

	@Test
	void valueEqualityIgnoresBigDecimalScale() {
		StrategyConfig first = custom(
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
		StrategyConfig second = custom(
				true,
				"0.3",
				StakingMode.FRACTIONAL_KELLY,
				"0.250",
				null,
				"0.020",
				"0.030",
				"0.030",
				"0.050",
				"0.100",
				"0.10",
				"0.150",
				"0.5",
				"0.20");
		assertThat(first).isEqualTo(second).isEqualTo(factory.configFor(StrategyPreset.DEFENSIVE));
	}

	@Test
	void fractionalKellyAllowsZeroFlatStakeRate() {
		StrategyConfig config = custom(
				true,
				"0.10",
				StakingMode.FRACTIONAL_KELLY,
				"0.20",
				"0",
				"0.025",
				"0.04",
				"0.04",
				"0.06",
				"0.12",
				"0.11",
				"0.16",
				"0.50",
				"0.22");
		assertThat(config.flatStakeRate()).isEqualByComparingTo("0");
	}

	@Test
	void flatStakeAllowsZeroKellyFraction() {
		StrategyConfig config = custom(
				false,
				"0",
				StakingMode.FLAT_STAKE,
				"0",
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
		assertThat(config.kellyFraction()).isEqualByComparingTo("0");
	}

	@Test
	void minimumEdgeZeroIsAllowed() {
		StrategyConfig config = custom(
				true,
				"0.10",
				StakingMode.FRACTIONAL_KELLY,
				"0.20",
				null,
				"0.025",
				"0",
				"0.04",
				"0.06",
				"0.12",
				"0.11",
				"0.16",
				"0.50",
				"0.22");
		assertThat(config.minimumEdge()).isEqualByComparingTo("0");
	}

	@Test
	void vaultEnabledWithZeroSweepIsInvalid() {
		assertInvalid(true, "0", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void vaultDisabledWithPositiveSweepIsInvalid() {
		assertInvalid(false, "0.30", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void vaultSweepAboveOneIsInvalid() {
		assertInvalid(true, "1.01", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void nullKellyInFractionalKellyIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, null, null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void zeroKellyInFractionalKellyIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void kellyAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "1.01", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void kellyActiveInFlatStakeIsInvalid() {
		assertInvalid(false, "0", StakingMode.FLAT_STAKE, "0.25", "0.01", "0.01", "0.03", "0.02", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void flatStakeActiveInFractionalKellyIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", "0.01", "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void zeroFlatStakeIsInvalid() {
		assertInvalid(false, "0", StakingMode.FLAT_STAKE, null, "0", "0.01", "0.03", "0.02", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void negativeFlatStakeIsInvalid() {
		assertInvalid(false, "0", StakingMode.FLAT_STAKE, null, "-0.01", "0.01", "0.03", "0.02", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void maxStakeZeroIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void maxStakeAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "1.01", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void negativeEdgeIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "-0.01", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void edgeAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "1.01", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void maxStakeNegativeIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "-0.01", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void matchExposureNegativeIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0", "0.05", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void leagueExposureZeroIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void dailyExposureZeroIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void matchExposureAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "1.01", "1.01", "1.01",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void leagueExposureAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "1.01", "1.01",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void dailyExposureAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "1.01",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void matchExposureAboveLeagueIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.10", "0.05", "0.15",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void leagueExposureAboveDailyIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.12", "0.10",
				"0.10", "0.15", "0.50", "0.20");
	}

	@Test
	void warningNotLessThanReductionIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.15", "0.15", "0.50", "0.20");
	}

	@Test
	void reductionNotLessThanStopIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.20", "0.50", "0.20");
	}

	@Test
	void stopAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0.50", "1.01");
	}

	@Test
	void multiplierZeroIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "0", "0.20");
	}

	@Test
	void multiplierAboveOneIsInvalid() {
		assertInvalid(true, "0.10", StakingMode.FRACTIONAL_KELLY, "0.20", null, "0.02", "0.03", "0.03", "0.05", "0.10",
				"0.10", "0.15", "1.50", "0.20");
	}

	@Test
	void nullRequiredRatesAreInvalid() {
		assertThatThrownBy(() -> new StrategyConfig(
						true,
						null,
						StakingMode.FRACTIONAL_KELLY,
						rate("0.20"),
						null,
						rate("0.02"),
						rate("0.03"),
						rate("0.03"),
						rate("0.05"),
						rate("0.10"),
						rate("0.10"),
						rate("0.15"),
						rate("0.50"),
						rate("0.20")))
				.isInstanceOf(StrategyException.class);
		assertThatThrownBy(() -> new StrategyConfig(
						true,
						rate("0.10"),
						null,
						rate("0.20"),
						null,
						rate("0.02"),
						rate("0.03"),
						rate("0.03"),
						rate("0.05"),
						rate("0.10"),
						rate("0.10"),
						rate("0.15"),
						rate("0.50"),
						rate("0.20")))
				.isInstanceOf(StrategyException.class);
	}

	private static void assertInvalid(
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
		assertThatThrownBy(() -> custom(
						vaultEnabled,
						vaultSweepRate,
						stakingMode,
						kellyFraction,
						flatStakeRate,
						maxStakeRate,
						minimumEdge,
						maxMatchExposure,
						maxLeagueExposure,
						maxDailyExposure,
						drawdownWarningThreshold,
						drawdownReductionThreshold,
						drawdownStakeMultiplier,
						drawdownStopThreshold))
				.isInstanceOf(StrategyException.class);
	}

	private static StrategyConfig custom(
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
		return new StrategyConfig(
				vaultEnabled,
				rate(vaultSweepRate),
				stakingMode,
				rate(kellyFraction),
				rate(flatStakeRate),
				rate(maxStakeRate),
				rate(minimumEdge),
				rate(maxMatchExposure),
				rate(maxLeagueExposure),
				rate(maxDailyExposure),
				rate(drawdownWarningThreshold),
				rate(drawdownReductionThreshold),
				rate(drawdownStakeMultiplier),
				rate(drawdownStopThreshold));
	}

	private static BigDecimal rate(String value) {
		return value == null ? null : new BigDecimal(value);
	}

}
