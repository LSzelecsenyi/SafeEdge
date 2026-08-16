package com.safeedge.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GeneralizedKellyCalculatorTest {

	private static final BigDecimal KELLY_TOLERANCE = new BigDecimal("1E-18");

	private final GeneralizedKellyCalculator calculator = new GeneralizedKellyCalculator();

	@Test
	void binaryKellyMatchesOrdinaryFormula() {
		BigDecimal odds = new BigDecimal("2.00");
		SettlementProbabilityDistribution distribution = SettlementProbabilityDistribution.binary(new BigDecimal("0.60"));
		assertThat(calculator.expectedReturnRate(odds, distribution)).isEqualByComparingTo("0.20");
		assertThat(calculator.fullKellyFraction(odds, distribution))
				.isCloseTo(new BigDecimal("0.20"), within(KELLY_TOLERANCE));
	}

	@Test
	void nonPositiveExpectedReturnYieldsZeroKelly() {
		BigDecimal odds = new BigDecimal("2.00");
		SettlementProbabilityDistribution fair = SettlementProbabilityDistribution.binary(new BigDecimal("0.50"));
		assertThat(calculator.expectedReturnRate(odds, fair)).isEqualByComparingTo("0");
		assertThat(calculator.fullKellyFraction(odds, fair)).isEqualByComparingTo("0");
		SettlementProbabilityDistribution losing = SettlementProbabilityDistribution.binary(new BigDecimal("0.40"));
		assertThat(calculator.expectedReturnRate(odds, losing)).isNegative();
		assertThat(calculator.fullKellyFraction(odds, losing)).isEqualByComparingTo("0");
	}

	@Test
	void pushMassIsNotTreatedAsLossAndHasClosedFormKelly() {
		BigDecimal odds = new BigDecimal("2");
		SettlementProbabilityDistribution withPush = new SettlementProbabilityDistribution(
				new BigDecimal("0.55"),
				BigDecimal.ZERO,
				new BigDecimal("0.10"),
				BigDecimal.ZERO,
				new BigDecimal("0.35"));
		assertThat(calculator.expectedReturnRate(odds, withPush)).isEqualByComparingTo("0.20");
		assertThat(calculator.fullKellyFraction(odds, withPush))
				.isCloseTo(new BigDecimal("2").divide(new BigDecimal("9"), GeneralizedKellyCalculator.MATH), within(KELLY_TOLERANCE));
		assertThat(calculator.fullKellyFraction(odds, withPush))
				.isGreaterThan(calculator.fullKellyFraction(odds, SettlementProbabilityDistribution.binary(new BigDecimal("0.55"))));
	}

	@Test
	void fiveOutcomeDistributionConvergesBetweenZeroAndOne() {
		BigDecimal odds = new BigDecimal("1.90");
		SettlementProbabilityDistribution distribution = quarterLike(new BigDecimal("0.35"), new BigDecimal("0.15"));
		BigDecimal kelly = calculator.fullKellyFraction(odds, distribution);
		assertThat(calculator.expectedReturnRate(odds, distribution)).isPositive();
		assertThat(kelly).isGreaterThan(BigDecimal.ZERO).isLessThan(BigDecimal.ONE);
		assertThat(kelly).isLessThan(GeneralizedKellyCalculator.F_UPPER);
	}

	@Test
	void increasingDownsideProbabilityReducesKelly() {
		BigDecimal odds = new BigDecimal("1.90");
		BigDecimal lowerDownside = calculator.fullKellyFraction(odds, quarterLike(new BigDecimal("0.35"), new BigDecimal("0.15")));
		BigDecimal higherDownside = calculator.fullKellyFraction(odds, quarterLike(new BigDecimal("0.30"), new BigDecimal("0.20")));
		assertThat(higherDownside).isLessThan(lowerDownside);
		assertThat(higherDownside).isGreaterThanOrEqualTo(BigDecimal.ZERO);
	}

	@Test
	void allPushHasZeroKelly() {
		SettlementProbabilityDistribution pushOnly = new SettlementProbabilityDistribution(
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ONE,
				BigDecimal.ZERO,
				BigDecimal.ZERO);
		assertThat(calculator.expectedReturnRate(new BigDecimal("1.90"), pushOnly)).isEqualByComparingTo("0");
		assertThat(calculator.fullKellyFraction(new BigDecimal("1.90"), pushOnly)).isEqualByComparingTo("0");
	}

	@Test
	void probabilitiesThatDoNotSumToOneAreRejected() {
		assertThatThrownBy(() -> new SettlementProbabilityDistribution(
						new BigDecimal("0.50"),
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						BigDecimal.ZERO,
						new BigDecimal("0.40")))
				.isInstanceOf(StrategyException.class);
	}

	private static SettlementProbabilityDistribution quarterLike(BigDecimal win, BigDecimal loss) {
		return new SettlementProbabilityDistribution(
				win,
				new BigDecimal("0.25"),
				new BigDecimal("0.10"),
				new BigDecimal("0.15"),
				loss);
	}

}
