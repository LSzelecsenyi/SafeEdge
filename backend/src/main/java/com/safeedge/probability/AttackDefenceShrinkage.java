package com.safeedge.probability;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Bayesian / pseudo-count shrinkage of a venue scoring rate toward the
 * point-in-time league rate.
 *
 * {@code attackDefenceShrinkageStrength} is weighted league-average
 * pseudo-match exposure, using the same exponential time weights as team
 * history ({@code Σ timeWeight}), not raw match counts.
 *
 * <pre>
 * shrunkRate = (weightedTeamGoals + prior * leagueRate)
 *            / (weightedTeamExposure + prior)
 * shrunkStrength = shrunkRate / leagueRate
 * </pre>
 *
 * {@code prior = 0} reduces to the unregularized weighted ratio used by v1.
 */
final class AttackDefenceShrinkage {

	private static final MathContext MATH = MathContext.DECIMAL128;

	private AttackDefenceShrinkage() {
	}

	static BigDecimal shrunkRate(
			BigDecimal weightedGoals,
			BigDecimal weightedExposure,
			BigDecimal leagueRate,
			BigDecimal priorStrength) {
		requireNonNegative(weightedGoals, "weightedGoals");
		requireNonNegative(weightedExposure, "weightedExposure");
		requireNonNegative(priorStrength, "priorStrength");
		if (leagueRate == null || leagueRate.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ProbabilityModelException("leagueRate must be > 0");
		}
		BigDecimal denominator = weightedExposure.add(priorStrength, MATH);
		if (denominator.compareTo(BigDecimal.ZERO) == 0) {
			throw new ProbabilityModelException("weighted exposure and shrinkage prior are both zero");
		}
		BigDecimal numerator = weightedGoals.add(priorStrength.multiply(leagueRate, MATH), MATH);
		return numerator.divide(denominator, MATH);
	}

	static BigDecimal shrunkStrength(
			BigDecimal weightedGoals,
			BigDecimal weightedExposure,
			BigDecimal leagueRate,
			BigDecimal priorStrength) {
		return shrunkRate(weightedGoals, weightedExposure, leagueRate, priorStrength).divide(leagueRate, MATH);
	}

	private static void requireNonNegative(BigDecimal value, String field) {
		if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
			throw new ProbabilityModelException(field + " must be >= 0");
		}
	}
}
