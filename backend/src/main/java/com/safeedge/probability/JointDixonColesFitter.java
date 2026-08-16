package com.safeedge.probability;

import java.util.Arrays;
import java.util.List;

/**
 * Deterministic penalized-likelihood fit for Probability Model v3.
 *
 * Defence convention: positive defence is stronger (concedes fewer). Log-link:
 *
 * <pre>
 * log λ_home = intercept + homeAdvantage + attack(home) − defence(away)
 * log λ_away = intercept + attack(away) − defence(home)
 * ρ = rhoScale * tanh(z)
 * </pre>
 *
 * Identifiability: {@code Σ attack = 0} and {@code Σ defence = 0} after every
 * step. Market odds never enter.
 */
final class JointDixonColesFitter {

	private static final double LOG_LAMBDA_MAX = 8.0d;
	private static final double LOG_LAMBDA_MIN = -20.0d;
	private static final double MIN_ALPHA = 1.0e-8d;
	private static final double ARMIJO_C = 1.0e-4d;

	private JointDixonColesFitter() {
	}

	static JointDixonColesFit fit(
			List<String> teams,
			List<FitObservation> observations,
			double leagueHomeRate,
			double leagueAwayRate,
			ProbabilityModelV3Config config,
			JointDixonColesFit warmStart) {
		int n = teams.size();
		double[] x = initialize(n, leagueHomeRate, leagueAwayRate, config, teams, warmStart);
		center(x, n);
		Objective start = objective(x, n, observations, config);
		if (!Double.isFinite(start.value())) {
			return JointDixonColesFit.failed(teams, 0, false);
		}
		double[] best = x.clone();
		double bestValue = start.value();
		double[] g = start.gradient();
		double[] prevX = x.clone();
		double[] prevG = g.clone();
		boolean converged = false;
		int iterations = 0;
		double alpha = 0.1d;
		for (int iter = 1; iter <= config.optimizerMaxIterations(); iter++) {
			iterations = iter;
			double gradNorm = rms(g);
			if (gradNorm < config.gradientTolerance()) {
				converged = true;
				break;
			}
			if (iter > 1) {
				alpha = barzilaiBorwein(prevX, x, prevG, g, alpha);
			}
			prevX = x.clone();
			prevG = g.clone();
			boolean improved = false;
			double step = alpha;
			while (step >= MIN_ALPHA) {
				double[] trial = addScaled(x, g, step);
				center(trial, n);
				Objective candidate = objective(trial, n, observations, config);
				if (Double.isFinite(candidate.value())
						&& candidate.value() >= bestValue + ARMIJO_C * step * gradNorm * gradNorm) {
					x = trial;
					g = candidate.gradient();
					bestValue = candidate.value();
					best = trial;
					improved = true;
					alpha = step;
					break;
				}
				step *= 0.5d;
			}
			if (!improved) {
				break;
			}
		}
		center(best, n);
		Objective fitted = objective(best, n, observations, config);
		if (!Double.isFinite(fitted.value())) {
			return JointDixonColesFit.failed(teams, iterations, false);
		}
		return JointDixonColesFit.success(
				teams,
				best[0],
				best[1],
				copyRange(best, 2, n),
				copyRange(best, 2 + n, n),
				best[rhoIndex(n)],
				rho(best[rhoIndex(n)], config.rhoScale()),
				fitted.value(),
				iterations,
				converged || rms(fitted.gradient()) < config.gradientTolerance());
	}

	static double rho(double unconstrained, double scale) {
		return scale * Math.tanh(unconstrained);
	}

	static void center(double[] x, int n) {
		shiftMean(x, 2, n);
		shiftMean(x, 2 + n, n);
	}

	static double identifiabilityResidual(double[] attack, double[] defence) {
		return Math.abs(mean(attack)) + Math.abs(mean(defence));
	}

	private static double[] initialize(
			int n,
			double leagueHomeRate,
			double leagueAwayRate,
			ProbabilityModelV3Config config,
			List<String> teams,
			JointDixonColesFit warmStart) {
		double[] x = new double[parameterCount(n)];
		double away = Math.max(leagueAwayRate, 0.05d);
		double home = Math.max(leagueHomeRate, 0.05d);
		x[0] = Math.log(away);
		x[1] = Math.log(home) - x[0];
		if (warmStart != null && warmStart.success()) {
			x[0] = warmStart.intercept();
			x[1] = warmStart.homeAdvantage();
			x[rhoIndex(n)] = warmStart.rhoUnconstrained();
			for (int i = 0; i < n; i++) {
				int old = warmStart.teamIndex(teams.get(i));
				if (old >= 0) {
					x[2 + i] = warmStart.attack()[old];
					x[2 + n + i] = warmStart.defence()[old];
				}
			}
		}
		return x;
	}

	private static Objective objective(
			double[] x, int n, List<FitObservation> observations, ProbabilityModelV3Config config) {
		double[] g = new double[x.length];
		double value = 0.0d;
		double rho = rho(x[rhoIndex(n)], config.rhoScale());
		double dRhoDz = config.rhoScale() * (1.0d - Math.tanh(x[rhoIndex(n)]) * Math.tanh(x[rhoIndex(n)]));
		for (FitObservation observation : observations) {
			double logHome = x[0] + x[1] + x[2 + observation.homeIndex()] - x[2 + n + observation.awayIndex()];
			double logAway = x[0] + x[2 + observation.awayIndex()] - x[2 + n + observation.homeIndex()];
			if (logHome > LOG_LAMBDA_MAX
					|| logAway > LOG_LAMBDA_MAX
					|| logHome < LOG_LAMBDA_MIN
					|| logAway < LOG_LAMBDA_MIN) {
				return Objective.invalid();
			}
			double lambdaHome = Math.exp(logHome);
			double lambdaAway = Math.exp(logAway);
			if (!DixonColesTau.validFor(lambdaHome, lambdaAway, rho)) {
				return Objective.invalid();
			}
			double logP = logPoisson(observation.homeGoals(), lambdaHome)
					+ logPoisson(observation.awayGoals(), lambdaAway)
					+ Math.log(DixonColesTau.tau(
							observation.homeGoals(),
							observation.awayGoals(),
							lambdaHome,
							lambdaAway,
							rho));
			if (!Double.isFinite(logP)) {
				return Objective.invalid();
			}
			value += observation.weight() * logP;
			double tauHome = dLogTauDLogLambdaHome(
					observation.homeGoals(), observation.awayGoals(), lambdaHome, lambdaAway, rho);
			double tauAway = dLogTauDLogLambdaAway(
					observation.homeGoals(), observation.awayGoals(), lambdaHome, lambdaAway, rho);
			double residHome = observation.homeGoals() - lambdaHome + tauHome;
			double residAway = observation.awayGoals() - lambdaAway + tauAway;
			double w = observation.weight();
			g[0] += w * (residHome + residAway);
			g[1] += w * residHome;
			g[2 + observation.homeIndex()] += w * residHome;
			g[2 + observation.awayIndex()] += w * residAway;
			g[2 + n + observation.awayIndex()] += w * (-residHome);
			g[2 + n + observation.homeIndex()] += w * (-residAway);
			g[rhoIndex(n)] += w
					* dLogTauDRho(observation.homeGoals(), observation.awayGoals(), lambdaHome, lambdaAway, rho)
					* dRhoDz;
		}
		for (int i = 0; i < n; i++) {
			value -= config.attackRegularization() * x[2 + i] * x[2 + i];
			value -= config.defenceRegularization() * x[2 + n + i] * x[2 + n + i];
			g[2 + i] -= 2.0d * config.attackRegularization() * x[2 + i];
			g[2 + n + i] -= 2.0d * config.defenceRegularization() * x[2 + n + i];
		}
		zeroMeanGradient(g, 2, n);
		zeroMeanGradient(g, 2 + n, n);
		if (!Double.isFinite(value)) {
			return Objective.invalid();
		}
		return new Objective(value, g);
	}

	private static double logPoisson(int goals, double lambda) {
		if (!(lambda > 0.0d) || !Double.isFinite(lambda)) {
			return Double.NEGATIVE_INFINITY;
		}
		double log = goals * Math.log(lambda) - lambda;
		for (int k = 2; k <= goals; k++) {
			log -= Math.log(k);
		}
		return log;
	}

	private static double dLogTauDLogLambdaHome(int home, int away, double lambdaHome, double lambdaAway, double rho) {
		double tau = DixonColesTau.tau(home, away, lambdaHome, lambdaAway, rho);
		if (!(tau > 0.0d)) {
			return 0.0d;
		}
		if (home == 0 && away == 0) {
			return lambdaHome * (-lambdaAway * rho) / tau;
		}
		if (home == 0 && away == 1) {
			return lambdaHome * rho / tau;
		}
		return 0.0d;
	}

	private static double dLogTauDLogLambdaAway(int home, int away, double lambdaHome, double lambdaAway, double rho) {
		double tau = DixonColesTau.tau(home, away, lambdaHome, lambdaAway, rho);
		if (!(tau > 0.0d)) {
			return 0.0d;
		}
		if (home == 0 && away == 0) {
			return lambdaAway * (-lambdaHome * rho) / tau;
		}
		if (home == 1 && away == 0) {
			return lambdaAway * rho / tau;
		}
		return 0.0d;
	}

	private static double dLogTauDRho(int home, int away, double lambdaHome, double lambdaAway, double rho) {
		double tau = DixonColesTau.tau(home, away, lambdaHome, lambdaAway, rho);
		if (!(tau > 0.0d)) {
			return 0.0d;
		}
		if (home == 0 && away == 0) {
			return (-lambdaHome * lambdaAway) / tau;
		}
		if (home == 0 && away == 1) {
			return lambdaHome / tau;
		}
		if (home == 1 && away == 0) {
			return lambdaAway / tau;
		}
		if (home == 1 && away == 1) {
			return -1.0d / tau;
		}
		return 0.0d;
	}

	private static double barzilaiBorwein(double[] prevX, double[] x, double[] prevG, double[] g, double fallback) {
		double ss = 0.0d;
		double sy = 0.0d;
		for (int i = 0; i < x.length; i++) {
			double s = x[i] - prevX[i];
			double y = g[i] - prevG[i];
			ss += s * s;
			sy += s * y;
		}
		if (!(sy > 1.0e-12d) || !(ss > 0.0d)) {
			return fallback;
		}
		return clamp(ss / sy, MIN_ALPHA, 1.0d);
	}

	private static double[] addScaled(double[] x, double[] g, double step) {
		double[] out = new double[x.length];
		for (int i = 0; i < x.length; i++) {
			out[i] = x[i] + step * g[i];
		}
		return out;
	}

	private static void shiftMean(double[] x, int offset, int n) {
		double mean = 0.0d;
		for (int i = 0; i < n; i++) {
			mean += x[offset + i];
		}
		mean /= n;
		for (int i = 0; i < n; i++) {
			x[offset + i] -= mean;
		}
	}

	private static void zeroMeanGradient(double[] g, int offset, int n) {
		shiftMean(g, offset, n);
	}

	private static double mean(double[] values) {
		double sum = 0.0d;
		for (double value : values) {
			sum += value;
		}
		return values.length == 0 ? 0.0d : sum / values.length;
	}

	private static double rms(double[] g) {
		double sum = 0.0d;
		for (double value : g) {
			sum += value * value;
		}
		return Math.sqrt(sum / g.length);
	}

	private static double clamp(double value, double lo, double hi) {
		return Math.max(lo, Math.min(hi, value));
	}

	private static int parameterCount(int n) {
		return 2 + 2 * n + 1;
	}

	private static int rhoIndex(int n) {
		return 2 + 2 * n;
	}

	private static double[] copyRange(double[] x, int offset, int n) {
		return Arrays.copyOfRange(x, offset, offset + n);
	}

	record FitObservation(double weight, int homeIndex, int awayIndex, int homeGoals, int awayGoals) {
	}

	private record Objective(double value, double[] gradient) {

		private static Objective invalid() {
			return new Objective(Double.NEGATIVE_INFINITY, new double[0]);
		}
	}
}
