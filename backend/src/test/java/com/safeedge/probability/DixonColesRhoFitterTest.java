package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class DixonColesRhoFitterTest {

	@Test
	void emptyObservationsYieldZeroRho() {
		assertThat(DixonColesRhoFitter.fit(List.of())).isEqualTo(0.0d);
	}

	@Test
	void fitIsDeterministic() {
		List<DixonColesRhoFitter.RhoObservation> observations = excessDraws();
		assertThat(DixonColesRhoFitter.fit(observations)).isEqualTo(DixonColesRhoFitter.fit(observations));
	}

	@Test
	void excessDrawsPullRhoNegative() {
		double rho = DixonColesRhoFitter.fit(excessDraws());
		assertThat(rho).isNegative();
		assertThat(rho).isGreaterThanOrEqualTo(DixonColesRhoFitter.HARD_MIN);
		assertThat(rho).isLessThanOrEqualTo(DixonColesRhoFitter.HARD_MAX);
	}

	@Test
	void fittedLikelihoodIsAtLeastAsGoodAsRhoZero() {
		List<DixonColesRhoFitter.RhoObservation> observations = excessDraws();
		double rho = DixonColesRhoFitter.fit(observations);
		double fitted = DixonColesRhoFitter.weightedLogLikelihood(observations, rho);
		double independent = DixonColesRhoFitter.weightedLogLikelihood(observations, 0.0d);
		assertThat(fitted).isGreaterThanOrEqualTo(independent);
	}

	@Test
	void heavierRecentDrawsMoveRhoVersusOldDraws() {
		List<DixonColesRhoFitter.RhoObservation> recentDraws = List.of(
				obs(1.0d, 1.4d, 1.1d, 0, 0),
				obs(1.0d, 1.4d, 1.1d, 0, 0),
				obs(0.1d, 1.4d, 1.1d, 0, 1));
		List<DixonColesRhoFitter.RhoObservation> oldDraws = List.of(
				obs(0.1d, 1.4d, 1.1d, 0, 0),
				obs(0.1d, 1.4d, 1.1d, 0, 0),
				obs(1.0d, 1.4d, 1.1d, 0, 1));
		assertThat(DixonColesRhoFitter.fit(recentDraws)).isLessThan(DixonColesRhoFitter.fit(oldDraws));
	}

	@Test
	void solutionStaysInsideTauValidInterval() {
		List<DixonColesRhoFitter.RhoObservation> observations = excessDraws();
		double rho = DixonColesRhoFitter.fit(observations);
		DixonColesRhoFitter.Interval interval = DixonColesRhoFitter.validInterval(observations);
		assertThat(rho).isBetween(interval.lo(), interval.hi());
		assertThat(DixonColesTau.validFor(1.4d, 1.1d, rho)).isTrue();
	}

	@Test
	void fitterApiHasNoOddsParameters() {
		assertThat(DixonColesRhoFitter.class.getDeclaredMethods())
				.extracting(method -> method.getName() + java.util.Arrays.toString(method.getParameterTypes()))
				.noneMatch(signature -> signature.toLowerCase().contains("odd") || signature.toLowerCase().contains("edge")
						|| signature.toLowerCase().contains("roi"));
	}

	private static List<DixonColesRhoFitter.RhoObservation> excessDraws() {
		List<DixonColesRhoFitter.RhoObservation> observations = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			observations.add(obs(1.0d, 1.4d, 1.1d, 0, 0));
		}
		for (int i = 0; i < 4; i++) {
			observations.add(obs(1.0d, 1.4d, 1.1d, 2, 1));
		}
		return observations;
	}

	private static DixonColesRhoFitter.RhoObservation obs(
			double weight, double lambdaHome, double lambdaAway, int home, int away) {
		double independent =
				IndependentPoisson.probability(lambdaHome, home) * IndependentPoisson.probability(lambdaAway, away);
		return new DixonColesRhoFitter.RhoObservation(weight, lambdaHome, lambdaAway, home, away, independent);
	}
}
