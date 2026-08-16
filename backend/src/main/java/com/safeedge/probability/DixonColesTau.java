package com.safeedge.probability;

/**
 * Dixon-Coles low-score dependence correction. Applies only to 0-0, 0-1, 1-0,
 * and 1-1. Other cells are unchanged before grid renormalization.
 *
 * <pre>
 * τ(0,0) = 1 − λμρ
 * τ(0,1) = 1 + λρ
 * τ(1,0) = 1 + μρ
 * τ(1,1) = 1 − ρ
 * τ(x,y) = 1 otherwise
 * </pre>
 *
 * Every used τ must be strictly positive. {@code ρ = 0} leaves the independent
 * Poisson joint unchanged.
 */
final class DixonColesTau {

	private static final double MIN_TAU = 1.0e-15;

	private DixonColesTau() {
	}

	static boolean isLowScoreCell(int homeGoals, int awayGoals) {
		return homeGoals <= 1 && awayGoals <= 1;
	}

	static double tau(int homeGoals, int awayGoals, double lambdaHome, double lambdaAway, double rho) {
		if (homeGoals == 0 && awayGoals == 0) {
			return 1.0d - lambdaHome * lambdaAway * rho;
		}
		if (homeGoals == 0 && awayGoals == 1) {
			return 1.0d + lambdaHome * rho;
		}
		if (homeGoals == 1 && awayGoals == 0) {
			return 1.0d + lambdaAway * rho;
		}
		if (homeGoals == 1 && awayGoals == 1) {
			return 1.0d - rho;
		}
		return 1.0d;
	}

	static boolean validFor(double lambdaHome, double lambdaAway, double rho) {
		if (!finiteNonNegative(lambdaHome) || !finiteNonNegative(lambdaAway) || !Double.isFinite(rho)) {
			return false;
		}
		return tau(0, 0, lambdaHome, lambdaAway, rho) > MIN_TAU
				&& tau(0, 1, lambdaHome, lambdaAway, rho) > MIN_TAU
				&& tau(1, 0, lambdaHome, lambdaAway, rho) > MIN_TAU
				&& tau(1, 1, lambdaHome, lambdaAway, rho) > MIN_TAU;
	}

	static void applyToJoint(double[][] joint, double lambdaHome, double lambdaAway, double rho) {
		if (joint == null || joint.length == 0 || joint[0] == null) {
			throw new ProbabilityModelException("joint grid is required");
		}
		if (!validFor(lambdaHome, lambdaAway, rho)) {
			throw new ProbabilityModelException(
					"Dixon-Coles rho is invalid for lambdas " + lambdaHome + ", " + lambdaAway + ", rho=" + rho);
		}
		if (rho == 0.0d) {
			return;
		}
		scale(joint, 0, 0, tau(0, 0, lambdaHome, lambdaAway, rho));
		if (joint[0].length > 1) {
			scale(joint, 0, 1, tau(0, 1, lambdaHome, lambdaAway, rho));
		}
		if (joint.length > 1) {
			scale(joint, 1, 0, tau(1, 0, lambdaHome, lambdaAway, rho));
			if (joint[1].length > 1) {
				scale(joint, 1, 1, tau(1, 1, lambdaHome, lambdaAway, rho));
			}
		}
	}

	private static void scale(double[][] joint, int home, int away, double factor) {
		if (home >= joint.length || away >= joint[home].length) {
			return;
		}
		joint[home][away] *= factor;
	}

	private static boolean finiteNonNegative(double value) {
		return Double.isFinite(value) && value >= 0.0d;
	}
}
