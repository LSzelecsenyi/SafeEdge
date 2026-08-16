package com.safeedge.probability;

/**
 * Independent Poisson PMFs. {@code double} is used for {@code exp} and factorials;
 * callers convert to {@code BigDecimal} for the public distribution.
 */
final class IndependentPoisson {

	private IndependentPoisson() {
	}

	static double[] pmf(double lambda, int maxGoals) {
		if (lambda < 0 || Double.isNaN(lambda) || Double.isInfinite(lambda)) {
			throw new ProbabilityModelException("lambda must be a finite non-negative number");
		}
		double[] probabilities = new double[maxGoals + 1];
		if (lambda == 0.0d) {
			probabilities[0] = 1.0d;
			return probabilities;
		}
		double exp = Math.exp(-lambda);
		double factorial = 1.0d;
		probabilities[0] = exp;
		for (int k = 1; k <= maxGoals; k++) {
			factorial *= k;
			probabilities[k] = exp * Math.pow(lambda, k) / factorial;
		}
		return probabilities;
	}

	static double[][] joint(double[] home, double[] away) {
		double[][] raw = new double[home.length][away.length];
		for (int i = 0; i < home.length; i++) {
			for (int j = 0; j < away.length; j++) {
				raw[i][j] = home[i] * away[j];
			}
		}
		return raw;
	}

	static double sum(double[][] raw) {
		double total = 0.0d;
		for (double[] row : raw) {
			for (double value : row) {
				total += value;
			}
		}
		return total;
	}
}
