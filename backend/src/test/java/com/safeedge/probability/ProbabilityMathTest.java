package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class ProbabilityMathTest {

	@Test
	void halfLifeHasWeightOneHalf() {
		assertThat(TimeDecay.weight(0, 180)).isEqualTo(1.0d);
		assertThat(TimeDecay.weight(180, 180)).isCloseTo(0.5d, within(1e-12));
		assertThat(TimeDecay.weight(360, 180)).isCloseTo(0.25d, within(1e-12));
		assertThat(TimeDecay.weight(1, 180)).isGreaterThan(TimeDecay.weight(180, 180));
	}

	@Test
	void unitLambdaMatchesExponentialPmfs() {
		double e = Math.exp(-1.0d);
		double[] pmf = IndependentPoisson.pmf(1.0d, 10);
		assertThat(pmf[0]).isCloseTo(e, within(1e-12));
		assertThat(pmf[1]).isCloseTo(e, within(1e-12));
		assertThat(pmf[2]).isCloseTo(e / 2.0d, within(1e-12));
	}

	@Test
	void jointIsProductBeforeNormalization() {
		double[] home = IndependentPoisson.pmf(1.0d, 5);
		double[] away = IndependentPoisson.pmf(0.8d, 5);
		double[][] joint = IndependentPoisson.joint(home, away);
		assertThat(joint[1][0]).isCloseTo(home[1] * away[0], within(1e-15));
	}
}
