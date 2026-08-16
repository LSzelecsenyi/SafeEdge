package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AttackDefenceShrinkageTest {

	private static final BigDecimal LEAGUE = new BigDecimal("1.5");

	@Test
	void zeroPriorReproducesRawRatio() {
		BigDecimal rate = AttackDefenceShrinkage.shrunkRate(
				new BigDecimal("6"), new BigDecimal("2"), LEAGUE, BigDecimal.ZERO);
		assertThat(rate).isEqualByComparingTo("3");
		assertThat(AttackDefenceShrinkage.shrunkStrength(
						new BigDecimal("6"), new BigDecimal("2"), LEAGUE, BigDecimal.ZERO))
				.isEqualByComparingTo("2");
	}

	@Test
	void largePriorPullsStrengthTowardOne() {
		BigDecimal small = AttackDefenceShrinkage.shrunkStrength(
				new BigDecimal("6"), new BigDecimal("2"), LEAGUE, new BigDecimal("5"));
		BigDecimal large = AttackDefenceShrinkage.shrunkStrength(
				new BigDecimal("6"), new BigDecimal("2"), LEAGUE, new BigDecimal("1000"));
		assertThat(small).isGreaterThan(BigDecimal.ONE);
		assertThat(large).isGreaterThan(BigDecimal.ONE);
		assertThat(large).isLessThan(small);
		assertThat(large).isCloseTo(BigDecimal.ONE, within(new BigDecimal("0.01")));
	}

	@Test
	void weightedExposureControlsShrinkage() {
		BigDecimal light = AttackDefenceShrinkage.shrunkStrength(
				new BigDecimal("3"), new BigDecimal("1"), LEAGUE, new BigDecimal("5"));
		BigDecimal heavy = AttackDefenceShrinkage.shrunkStrength(
				new BigDecimal("15"), new BigDecimal("5"), LEAGUE, new BigDecimal("5"));
		assertThat(light).isGreaterThan(BigDecimal.ONE);
		assertThat(heavy).isGreaterThan(light);
		assertThat(heavy.subtract(BigDecimal.ONE).abs()).isGreaterThan(light.subtract(BigDecimal.ONE).abs());
	}

	@Test
	void zeroExposureWithPriorIsLeagueAverage() {
		assertThat(AttackDefenceShrinkage.shrunkStrength(BigDecimal.ZERO, BigDecimal.ZERO, LEAGUE, new BigDecimal("5")))
				.isEqualByComparingTo("1");
	}

	@Test
	void rejectsZeroExposureWithoutPrior() {
		assertThatThrownBy(() -> AttackDefenceShrinkage.shrunkStrength(
						BigDecimal.ZERO, BigDecimal.ZERO, LEAGUE, BigDecimal.ZERO))
				.isInstanceOf(ProbabilityModelException.class);
	}

	@Test
	void strengthIsNeverNegative() {
		assertThat(AttackDefenceShrinkage.shrunkStrength(
						BigDecimal.ZERO, new BigDecimal("4"), LEAGUE, new BigDecimal("5")))
				.isGreaterThanOrEqualTo(BigDecimal.ZERO);
	}
}
