package com.safeedge.probability;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Diagnostics-only v2 internals for one target. Not used by CandidateEngine.
 */
public record DixonColesFitSnapshot(
		LocalDate matchDate,
		BigDecimal rawHomeAttackStrength,
		BigDecimal rawHomeDefenceStrength,
		BigDecimal rawAwayAttackStrength,
		BigDecimal rawAwayDefenceStrength,
		BigDecimal homeAttackStrength,
		BigDecimal homeDefenceStrength,
		BigDecimal awayAttackStrength,
		BigDecimal awayDefenceStrength,
		BigDecimal lambdaHome,
		BigDecimal lambdaAway,
		BigDecimal rho,
		int rhoFitObservations) {

	public DixonColesFitSnapshot {
		if (matchDate == null) {
			throw new ProbabilityModelException("matchDate is required");
		}
		rawHomeAttackStrength = strip(rawHomeAttackStrength);
		rawHomeDefenceStrength = strip(rawHomeDefenceStrength);
		rawAwayAttackStrength = strip(rawAwayAttackStrength);
		rawAwayDefenceStrength = strip(rawAwayDefenceStrength);
		homeAttackStrength = strip(homeAttackStrength);
		homeDefenceStrength = strip(homeDefenceStrength);
		awayAttackStrength = strip(awayAttackStrength);
		awayDefenceStrength = strip(awayDefenceStrength);
		lambdaHome = strip(lambdaHome);
		lambdaAway = strip(lambdaAway);
		rho = strip(rho);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
