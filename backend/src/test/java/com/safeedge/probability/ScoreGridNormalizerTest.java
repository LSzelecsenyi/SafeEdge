package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ScoreGridNormalizerTest {

	private static final int MAX_GOALS = 10;

	@Test
	void oldDoubleQuotientResidualWasSlightlyNegativeForRealLambdas() {
		BigDecimal homeLambda = new BigDecimal("0.9997701915704261312243363881073959");
		BigDecimal awayLambda = new BigDecimal("0.6228642025904503649925218215240889");
		double[] home = IndependentPoisson.pmf(homeLambda.doubleValue(), MAX_GOALS);
		double[] away = IndependentPoisson.pmf(awayLambda.doubleValue(), MAX_GOALS);
		double[][] joint = IndependentPoisson.joint(home, away);
		double rawSum = IndependentPoisson.sum(joint);
		BigDecimal remaining = BigDecimal.ONE;
		int lastIndex = (MAX_GOALS + 1) * (MAX_GOALS + 1);
		int index = 0;
		for (int h = 0; h <= MAX_GOALS; h++) {
			for (int a = 0; a <= MAX_GOALS; a++) {
				index++;
				if (index == lastIndex) {
					assertThat(remaining).isNegative();
					assertThat(remaining.abs()).isLessThanOrEqualTo(ScoreGridNormalizer.NUMERICAL_TOLERANCE);
					assertThat(remaining).isEqualByComparingTo(new BigDecimal("-9.8340710048262E-18"));
					return;
				}
				remaining = remaining.subtract(BigDecimal.valueOf(joint[h][a] / rawSum));
			}
		}
	}

	@Test
	void secondRealBacktestLambdasAlsoProduceValidUnitDistribution() {
		BigDecimal homeLambda = new BigDecimal("1.727606002521317119518798104327540");
		BigDecimal awayLambda = new BigDecimal("0.2970259668026246410472243525599101");
		assertValidUnitDistribution(distribution(homeLambda, awayLambda));
	}

	@Test
	void realBacktestLambdasProduceValidUnitDistribution() {
		BigDecimal homeLambda = new BigDecimal("0.9997701915704261312243363881073959");
		BigDecimal awayLambda = new BigDecimal("0.6228642025904503649925218215240889");
		ScoreProbabilityDistribution distribution = distribution(homeLambda, awayLambda);
		assertValidUnitDistribution(distribution);
	}

	@Test
	void representativeLambdaPairsStayInsideUnitInterval() {
		double[] lambdas = {0.0, 0.05, 0.2, 0.5, 1.0, 1.5, 2.5, 4.0, 6.0};
		for (double homeLambda : lambdas) {
			for (double awayLambda : lambdas) {
				ScoreProbabilityDistribution distribution =
						distribution(BigDecimal.valueOf(homeLambda), BigDecimal.valueOf(awayLambda));
				assertValidUnitDistribution(distribution);
			}
		}
	}

	@Test
	void callerDoubleSumIsNotTheRenormalizationDenominator() {
		double[][] raw = {
			{0.6d, 0.6d},
			{0.6d, 0.0d}
		};
		ScoreProbabilityDistribution distribution =
				ScoreGridNormalizer.normalize(raw, 1.0d, BigDecimal.ONE, BigDecimal.ONE);
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : distribution.entries()) {
			assertThat(entry.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
			assertThat(entry.probability()).isLessThanOrEqualTo(BigDecimal.ONE);
			sum = sum.add(entry.probability());
		}
		assertThat(sum.compareTo(BigDecimal.ONE)).isZero();
	}

	@Test
	void negativeRawCellFailsLoudly() {
		double[][] raw = {
			{0.5d, -0.1d},
			{0.3d, 0.3d}
		};
		assertThatThrownBy(() -> ScoreGridNormalizer.normalize(raw, 1.0d))
				.isInstanceOf(ProbabilityModelException.class)
				.hasMessageContaining("raw cell probability is not finite and non-negative");
	}

	private static ScoreProbabilityDistribution distribution(BigDecimal homeLambda, BigDecimal awayLambda) {
		double[] home = IndependentPoisson.pmf(homeLambda.doubleValue(), MAX_GOALS);
		double[] away = IndependentPoisson.pmf(awayLambda.doubleValue(), MAX_GOALS);
		double[][] joint = IndependentPoisson.joint(home, away);
		double rawSum = IndependentPoisson.sum(joint);
		return ScoreGridNormalizer.normalize(joint, rawSum, homeLambda, awayLambda);
	}

	private static void assertValidUnitDistribution(ScoreProbabilityDistribution distribution) {
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : distribution.entries()) {
			assertThat(entry.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
			assertThat(entry.probability()).isLessThanOrEqualTo(BigDecimal.ONE);
			sum = sum.add(entry.probability());
		}
		assertThat(sum.compareTo(BigDecimal.ONE)).isZero();
		assertThat(distribution.entries()).hasSize((MAX_GOALS + 1) * (MAX_GOALS + 1));
	}
}
