package com.safeedge.probability;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fitted v3 parameters at one cutoff. Attack/defence are centered.
 */
final class JointDixonColesFit {

	private final List<String> teams;
	private final Map<String, Integer> index;
	private final double intercept;
	private final double homeAdvantage;
	private final double[] attack;
	private final double[] defence;
	private final double rhoUnconstrained;
	private final double rho;
	private final double logLikelihood;
	private final int iterations;
	private final boolean converged;
	private final boolean success;

	private JointDixonColesFit(
			List<String> teams,
			double intercept,
			double homeAdvantage,
			double[] attack,
			double[] defence,
			double rhoUnconstrained,
			double rho,
			double logLikelihood,
			int iterations,
			boolean converged,
			boolean success) {
		this.teams = List.copyOf(teams);
		this.index = index(this.teams);
		this.intercept = intercept;
		this.homeAdvantage = homeAdvantage;
		this.attack = attack;
		this.defence = defence;
		this.rhoUnconstrained = rhoUnconstrained;
		this.rho = rho;
		this.logLikelihood = logLikelihood;
		this.iterations = iterations;
		this.converged = converged;
		this.success = success;
	}

	static JointDixonColesFit success(
			List<String> teams,
			double intercept,
			double homeAdvantage,
			double[] attack,
			double[] defence,
			double rhoUnconstrained,
			double rho,
			double logLikelihood,
			int iterations,
			boolean converged) {
		return new JointDixonColesFit(
				teams,
				intercept,
				homeAdvantage,
				attack,
				defence,
				rhoUnconstrained,
				rho,
				logLikelihood,
				iterations,
				converged,
				true);
	}

	static JointDixonColesFit failed(List<String> teams, int iterations, boolean converged) {
		int n = teams.size();
		return new JointDixonColesFit(
				teams, Double.NaN, Double.NaN, new double[n], new double[n], 0.0d, 0.0d, Double.NEGATIVE_INFINITY, iterations, converged, false);
	}

	boolean success() {
		return success;
	}

	List<String> teams() {
		return teams;
	}

	int teamIndex(String team) {
		Integer value = index.get(team);
		return value == null ? -1 : value;
	}

	double intercept() {
		return intercept;
	}

	double homeAdvantage() {
		return homeAdvantage;
	}

	double[] attack() {
		return attack;
	}

	double[] defence() {
		return defence;
	}

	double rhoUnconstrained() {
		return rhoUnconstrained;
	}

	double rho() {
		return rho;
	}

	double logLikelihood() {
		return logLikelihood;
	}

	int iterations() {
		return iterations;
	}

	boolean converged() {
		return converged;
	}

	double lambdaHome(String homeTeam, String awayTeam) {
		int home = teamIndex(homeTeam);
		int away = teamIndex(awayTeam);
		if (home < 0 || away < 0) {
			return Double.NaN;
		}
		return Math.exp(intercept + homeAdvantage + attack[home] - defence[away]);
	}

	double lambdaAway(String homeTeam, String awayTeam) {
		int home = teamIndex(homeTeam);
		int away = teamIndex(awayTeam);
		if (home < 0 || away < 0) {
			return Double.NaN;
		}
		return Math.exp(intercept + attack[away] - defence[home]);
	}

	private static Map<String, Integer> index(List<String> teams) {
		Map<String, Integer> map = new LinkedHashMap<>();
		for (int i = 0; i < teams.size(); i++) {
			map.put(teams.get(i), i);
		}
		return map;
	}
}
