package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class DixonColesTauTest {

	@Test
	void rhoZeroLeavesIndependentJointUnchanged() {
		double[] home = IndependentPoisson.pmf(1.4d, 4);
		double[] away = IndependentPoisson.pmf(1.1d, 4);
		double[][] joint = IndependentPoisson.joint(home, away);
		double[][] copy = copy(joint);
		DixonColesTau.applyToJoint(joint, 1.4d, 1.1d, 0.0d);
		assertThat(joint).isDeepEqualTo(copy);
	}

	@Test
	void correctsOnlyLowScoreCells() {
		double[] home = IndependentPoisson.pmf(1.4d, 4);
		double[] away = IndependentPoisson.pmf(1.1d, 4);
		double[][] before = IndependentPoisson.joint(home, away);
		double[][] after = copy(before);
		double rho = -0.1d;
		DixonColesTau.applyToJoint(after, 1.4d, 1.1d, rho);
		assertThat(after[0][0]).isCloseTo(before[0][0] * DixonColesTau.tau(0, 0, 1.4d, 1.1d, rho), within(1.0e-12));
		assertThat(after[0][1]).isCloseTo(before[0][1] * DixonColesTau.tau(0, 1, 1.4d, 1.1d, rho), within(1.0e-12));
		assertThat(after[1][0]).isCloseTo(before[1][0] * DixonColesTau.tau(1, 0, 1.4d, 1.1d, rho), within(1.0e-12));
		assertThat(after[1][1]).isCloseTo(before[1][1] * DixonColesTau.tau(1, 1, 1.4d, 1.1d, rho), within(1.0e-12));
		assertThat(after[2][2]).isEqualTo(before[2][2]);
		assertThat(after[0][2]).isEqualTo(before[0][2]);
		assertThat(after[3][1]).isEqualTo(before[3][1]);
	}

	@Test
	void tauFormulas() {
		assertThat(DixonColesTau.tau(0, 0, 1.2d, 1.1d, -0.1d)).isCloseTo(1.0d - 1.2d * 1.1d * -0.1d, within(1.0e-12));
		assertThat(DixonColesTau.tau(0, 1, 1.2d, 1.1d, -0.1d)).isCloseTo(1.0d + 1.2d * -0.1d, within(1.0e-12));
		assertThat(DixonColesTau.tau(1, 0, 1.2d, 1.1d, -0.1d)).isCloseTo(1.0d + 1.1d * -0.1d, within(1.0e-12));
		assertThat(DixonColesTau.tau(1, 1, 1.2d, 1.1d, -0.1d)).isCloseTo(1.0d - -0.1d, within(1.0e-12));
		assertThat(DixonColesTau.tau(2, 1, 1.2d, 1.1d, -0.1d)).isEqualTo(1.0d);
	}

	@Test
	void rejectsInvalidRhoThatMakesTauNonPositive() {
		assertThat(DixonColesTau.validFor(1.4d, 1.1d, 1.0d)).isFalse();
		assertThatThrownBy(() -> DixonColesTau.applyToJoint(IndependentPoisson.joint(
						IndependentPoisson.pmf(1.4d, 2), IndependentPoisson.pmf(1.1d, 2)), 1.4d, 1.1d, 1.0d))
				.isInstanceOf(ProbabilityModelException.class);
	}

	private static double[][] copy(double[][] source) {
		double[][] copy = new double[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = source[i].clone();
		}
		return copy;
	}
}
