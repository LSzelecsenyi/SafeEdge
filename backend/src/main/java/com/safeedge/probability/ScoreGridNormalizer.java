package com.safeedge.probability;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Renormalizes a truncated non-negative score grid so published probabilities
 * sum to exactly 1. Poisson PMFs are computed in {@code double}; this step
 * converts each cell with {@link BigDecimal#valueOf(double)} and divides by
 * the BigDecimal sum of those cells ({@link MathContext#DECIMAL128}).
 *
 * The last cell is the residual {@code 1 - sum(previous)} so the domain
 * invariant holds exactly. A residual below 0 by at most
 * {@link #NUMERICAL_TOLERANCE} is absorbed into an earlier cell. A larger
 * residual fails loudly.
 */
final class ScoreGridNormalizer {

	private static final MathContext MATH = MathContext.DECIMAL128;

	/**
	 * Allowed last-cell residual overshoot. Real Premier League walk-forward
	 * remainders were about {@code -1e-17} to {@code -1e-15} when dividing by
	 * the {@code double} grid sum.
	 */
	static final BigDecimal NUMERICAL_TOLERANCE = new BigDecimal("1E-12");

	private ScoreGridNormalizer() {
	}

	static ScoreProbabilityDistribution normalize(double[][] raw, double rawSum) {
		return normalize(raw, rawSum, null, null);
	}

	static ScoreProbabilityDistribution normalize(
			double[][] raw, double rawSum, BigDecimal lambdaHome, BigDecimal lambdaAway) {
		if (raw == null || raw.length == 0 || raw[0] == null || raw[0].length == 0) {
			throw new ProbabilityModelException("raw score grid is required");
		}
		if (rawSum <= 0.0d || Double.isNaN(rawSum) || Double.isInfinite(rawSum)) {
			throw new ProbabilityModelException("raw probability mass must be positive and finite");
		}
		int homeMax = raw.length - 1;
		int awayMax = raw[0].length - 1;
		int lastIndex = (homeMax + 1) * (awayMax + 1);
		BigDecimal[] masses = new BigDecimal[lastIndex];
		BigDecimal gridSum = BigDecimal.ZERO;
		int index = 0;
		for (int home = 0; home <= homeMax; home++) {
			for (int away = 0; away <= awayMax; away++) {
				double rawCell = raw[home][away];
				if (Double.isNaN(rawCell) || Double.isInfinite(rawCell) || rawCell < 0.0d) {
					throw new ProbabilityModelException(
							detail(
									"raw cell probability is not finite and non-negative",
									null,
									home,
									away,
									rawCell,
									rawSum,
									lambdaHome,
									lambdaAway));
				}
				BigDecimal mass = BigDecimal.valueOf(rawCell);
				masses[index] = mass;
				gridSum = gridSum.add(mass);
				index++;
			}
		}
		if (gridSum.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ProbabilityModelException("raw probability mass must be positive and finite");
		}
		List<BigDecimal> probabilities = new ArrayList<>(lastIndex);
		BigDecimal remaining = BigDecimal.ONE;
		index = 0;
		for (int home = 0; home <= homeMax; home++) {
			for (int away = 0; away <= awayMax; away++) {
				if (index == lastIndex - 1) {
					BigDecimal last = finalizeResidual(
							remaining, probabilities, home, away, raw[home][away], rawSum, lambdaHome, lambdaAway);
					probabilities.add(last);
				}
				else {
					BigDecimal probability = masses[index].divide(gridSum, MATH);
					probability = requireUnitInterval(
							probability, home, away, raw[home][away], rawSum, lambdaHome, lambdaAway);
					remaining = remaining.subtract(probability);
					probabilities.add(probability);
				}
				index++;
			}
		}
		List<ScoreProbability> entries = new ArrayList<>(lastIndex);
		int pos = 0;
		for (int home = 0; home <= homeMax; home++) {
			for (int away = 0; away <= awayMax; away++) {
				entries.add(new ScoreProbability(new MatchScore(home, away), probabilities.get(pos)));
				pos++;
			}
		}
		return new ScoreProbabilityDistribution(entries);
	}

	private static BigDecimal finalizeResidual(
			BigDecimal remaining,
			List<BigDecimal> previous,
			int home,
			int away,
			double rawCell,
			double rawSum,
			BigDecimal lambdaHome,
			BigDecimal lambdaAway) {
		if (remaining.compareTo(BigDecimal.ZERO) >= 0 && remaining.compareTo(BigDecimal.ONE) <= 0) {
			return remaining;
		}
		if (remaining.compareTo(BigDecimal.ZERO) < 0
				&& remaining.abs().compareTo(NUMERICAL_TOLERANCE) <= 0) {
			return absorbNegativeResidual(remaining, previous, home, away, rawCell, rawSum, lambdaHome, lambdaAway);
		}
		throw new ProbabilityModelException(
				detail(
						"normalized last-cell residual out of [0,1]",
						remaining,
						home,
						away,
						rawCell,
						rawSum,
						lambdaHome,
						lambdaAway));
	}

	private static BigDecimal absorbNegativeResidual(
			BigDecimal remaining,
			List<BigDecimal> previous,
			int home,
			int away,
			double rawCell,
			double rawSum,
			BigDecimal lambdaHome,
			BigDecimal lambdaAway) {
		BigDecimal carry = remaining;
		for (int i = previous.size() - 1; i >= 0; i--) {
			BigDecimal adjusted = previous.get(i).add(carry);
			if (adjusted.compareTo(BigDecimal.ZERO) >= 0) {
				previous.set(i, adjusted);
				return BigDecimal.ZERO;
			}
			previous.set(i, BigDecimal.ZERO);
			carry = adjusted;
		}
		throw new ProbabilityModelException(
				detail(
						"could not absorb last-cell residual into earlier cells",
						remaining,
						home,
						away,
						rawCell,
						rawSum,
						lambdaHome,
						lambdaAway));
	}

	private static BigDecimal requireUnitInterval(
			BigDecimal probability,
			int home,
			int away,
			double rawCell,
			double rawSum,
			BigDecimal lambdaHome,
			BigDecimal lambdaAway) {
		if (probability.compareTo(BigDecimal.ZERO) >= 0 && probability.compareTo(BigDecimal.ONE) <= 0) {
			return probability;
		}
		if (probability.compareTo(BigDecimal.ZERO) < 0
				&& probability.abs().compareTo(NUMERICAL_TOLERANCE) <= 0) {
			return BigDecimal.ZERO;
		}
		if (probability.compareTo(BigDecimal.ONE) > 0
				&& probability.subtract(BigDecimal.ONE).compareTo(NUMERICAL_TOLERANCE) <= 0) {
			return BigDecimal.ONE;
		}
		throw new ProbabilityModelException(
				detail(
						"normalized probability out of [0,1]",
						probability,
						home,
						away,
						rawCell,
						rawSum,
						lambdaHome,
						lambdaAway));
	}

	private static String detail(
			String prefix,
			BigDecimal probability,
			int home,
			int away,
			double rawCell,
			double rawSum,
			BigDecimal lambdaHome,
			BigDecimal lambdaAway) {
		return prefix
				+ ": probability="
				+ probability
				+ " homeLambda="
				+ lambdaHome
				+ " awayLambda="
				+ lambdaAway
				+ " scoreCell=("
				+ home
				+ ","
				+ away
				+ ") rawCell="
				+ rawCell
				+ " rawGridSum="
				+ rawSum
				+ " MathContext="
				+ MATH;
	}
}
