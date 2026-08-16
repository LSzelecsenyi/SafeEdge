package com.safeedge.probability;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Diagnostics-only v3 fit internals for one target. Not used by CandidateEngine.
 */
public record JointDixonColesFitSnapshot(
		LocalDate matchDate,
		BigDecimal intercept,
		BigDecimal homeAdvantage,
		BigDecimal homeAttack,
		BigDecimal homeDefence,
		BigDecimal awayAttack,
		BigDecimal awayDefence,
		BigDecimal lambdaHome,
		BigDecimal lambdaAway,
		BigDecimal rho,
		int iterations,
		boolean converged,
		int teamCount,
		int observationCount) {

	public JointDixonColesFitSnapshot {
		if (matchDate == null) {
			throw new ProbabilityModelException("matchDate is required");
		}
		intercept = strip(intercept);
		homeAdvantage = strip(homeAdvantage);
		homeAttack = strip(homeAttack);
		homeDefence = strip(homeDefence);
		awayAttack = strip(awayAttack);
		awayDefence = strip(awayDefence);
		lambdaHome = strip(lambdaHome);
		lambdaAway = strip(lambdaAway);
		rho = strip(rho);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
