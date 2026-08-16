package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JointDixonColesFitterTest {

	@Test
	void identifiabilityCentersAttackAndDefence() {
		List<String> teams = List.of("A", "B", "C", "D");
		List<JointDixonColesFitter.FitObservation> observations = new ArrayList<>();
		observations.add(new JointDixonColesFitter.FitObservation(1.0d, 0, 1, 2, 1));
		observations.add(new JointDixonColesFitter.FitObservation(1.0d, 1, 0, 1, 1));
		observations.add(new JointDixonColesFitter.FitObservation(1.0d, 2, 3, 3, 0));
		observations.add(new JointDixonColesFitter.FitObservation(1.0d, 3, 2, 0, 2));
		observations.add(new JointDixonColesFitter.FitObservation(0.5d, 0, 2, 1, 0));
		observations.add(new JointDixonColesFitter.FitObservation(0.5d, 2, 0, 1, 1));
		JointDixonColesFit fit = JointDixonColesFitter.fit(
				teams, observations, 1.5d, 1.0d, ProbabilityModelV3Config.defaults(), null);
		assertThat(fit.success()).isTrue();
		assertThat(JointDixonColesFitter.identifiabilityResidual(fit.attack(), fit.defence())).isLessThan(1.0e-9d);
	}

	@Test
	void rhoIsBoundedByScale() {
		assertThat(Math.abs(JointDixonColesFitter.rho(100.0d, 0.4d))).isLessThanOrEqualTo(0.4d);
		assertThat(Math.abs(JointDixonColesFitter.rho(-100.0d, 0.4d))).isLessThanOrEqualTo(0.4d);
	}

	@Test
	void invalidStartReturnsFittingFailureNotSilentParameters() {
		List<String> teams = List.of("A", "B");
		List<JointDixonColesFitter.FitObservation> observations =
				List.of(new JointDixonColesFitter.FitObservation(1.0d, 0, 1, 1, 0));
		JointDixonColesFit fit = JointDixonColesFitter.fit(
				teams, observations, Double.NaN, Double.NaN, ProbabilityModelV3Config.defaults(), null);
		assertThat(fit.success()).isFalse();
		assertThat(Double.isFinite(fit.intercept())).isFalse();
	}
}
