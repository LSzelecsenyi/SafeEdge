package com.safeedge.probability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * One-dimensional weighted score-likelihood fit for Dixon-Coles ρ. Uses only
 * historical final scores and model lambdas. Market odds, candidate edge, and
 * ROI never enter the objective.
 *
 * Search is deterministic golden-section maximization on a tau-valid interval.
 * ρ = 0 (independent Poisson) is always a candidate.
 */
final class DixonColesRhoFitter {

	static final double HARD_MIN = -0.5d;
	static final double HARD_MAX = 0.5d;
	static final double TAU_MARGIN = 1.0e-6;
	private static final int GOLDEN_STEPS = 48;
	private static final double GOLDEN = (Math.sqrt(5.0d) - 1.0d) / 2.0d;

	private DixonColesRhoFitter() {
	}

	static double fit(List<RhoObservation> observations) {
		if (observations == null || observations.isEmpty()) {
			return 0.0d;
		}
		Interval interval = validInterval(observations);
		DoubleUnaryOperator logLikelihood = rho -> weightedLogLikelihood(observations, rho);
		double best = 0.0d;
		double bestValue = logLikelihood.applyAsDouble(0.0d);
		if (interval.width() > TAU_MARGIN) {
			double candidate = maximize(logLikelihood, interval.lo(), interval.hi());
			double endpoints = bestOf(
					logLikelihood, interval.lo(), interval.hi(), candidate);
			double endpointValue = logLikelihood.applyAsDouble(endpoints);
			if (endpointValue > bestValue) {
				best = endpoints;
				bestValue = endpointValue;
			}
		}
		if (!Double.isFinite(bestValue)) {
			return 0.0d;
		}
		return best;
	}

	static double weightedLogLikelihood(List<RhoObservation> observations, double rho) {
		double total = 0.0d;
		for (RhoObservation observation : observations) {
			if (!DixonColesTau.validFor(observation.lambdaHome(), observation.lambdaAway(), rho)) {
				return Double.NEGATIVE_INFINITY;
			}
			double probability = observation.independentProbability()
					* DixonColesTau.tau(
							observation.homeGoals(),
							observation.awayGoals(),
							observation.lambdaHome(),
							observation.lambdaAway(),
							rho);
			if (!(probability > 0.0d) || !Double.isFinite(probability)) {
				return Double.NEGATIVE_INFINITY;
			}
			total += observation.weight() * Math.log(probability);
		}
		return total;
	}

	static Interval validInterval(List<RhoObservation> observations) {
		double lo = HARD_MIN;
		double hi = HARD_MAX;
		for (RhoObservation observation : observations) {
			double lambda = observation.lambdaHome();
			double mu = observation.lambdaAway();
			if (lambda > 0.0d) {
				lo = Math.max(lo, -1.0d / lambda + TAU_MARGIN);
			}
			if (mu > 0.0d) {
				lo = Math.max(lo, -1.0d / mu + TAU_MARGIN);
			}
			double product = lambda * mu;
			if (product > 0.0d) {
				hi = Math.min(hi, 1.0d / product - TAU_MARGIN);
			}
			hi = Math.min(hi, 1.0d - TAU_MARGIN);
		}
		if (lo >= hi) {
			return new Interval(0.0d, 0.0d);
		}
		return new Interval(lo, hi);
	}

	private static double maximize(DoubleUnaryOperator f, double lo, double hi) {
		double a = lo;
		double b = hi;
		double c = b - GOLDEN * (b - a);
		double d = a + GOLDEN * (b - a);
		double fc = f.applyAsDouble(c);
		double fd = f.applyAsDouble(d);
		for (int i = 0; i < GOLDEN_STEPS; i++) {
			if (fc < fd) {
				a = c;
				c = d;
				fc = fd;
				d = a + GOLDEN * (b - a);
				fd = f.applyAsDouble(d);
			}
			else {
				b = d;
				d = c;
				fd = fc;
				c = b - GOLDEN * (b - a);
				fc = f.applyAsDouble(c);
			}
		}
		return fc >= fd ? c : d;
	}

	private static double bestOf(DoubleUnaryOperator f, double... values) {
		double bestX = values[0];
		double bestY = f.applyAsDouble(bestX);
		for (int i = 1; i < values.length; i++) {
			double y = f.applyAsDouble(values[i]);
			if (y > bestY) {
				bestX = values[i];
				bestY = y;
			}
		}
		return bestX;
	}

	record RhoObservation(
			double weight, double lambdaHome, double lambdaAway, int homeGoals, int awayGoals, double independentProbability) {

		RhoObservation {
			if (!(weight > 0.0d) || !Double.isFinite(weight)) {
				throw new ProbabilityModelException("rho observation weight must be finite and > 0");
			}
			if (homeGoals < 0 || awayGoals < 0) {
				throw new ProbabilityModelException("goals must be >= 0");
			}
			if (!(independentProbability >= 0.0d) || !Double.isFinite(independentProbability)) {
				throw new ProbabilityModelException("independent probability must be finite and >= 0");
			}
		}
	}

	record Interval(double lo, double hi) {

		double width() {
			return hi - lo;
		}
	}

	static List<RhoObservation> copy(List<RhoObservation> observations) {
		return List.copyOf(observations == null ? List.of() : new ArrayList<>(observations));
	}
}
