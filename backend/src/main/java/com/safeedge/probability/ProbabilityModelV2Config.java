package com.safeedge.probability;

import java.math.BigDecimal;

/**
 * Immutable Probability Model v2 configuration. Shared Poisson fields keep the
 * same defaults as v1. {@code attackDefenceShrinkageStrength} is weighted
 * league-average pseudo-match exposure, not a betting parameter and not an
 * optimized value.
 */
public record ProbabilityModelV2Config(
		int decayHalfLifeDays,
		int maxGoalsPerTeam,
		int minimumTeamMatches,
		BigDecimal attackDefenceShrinkageStrength,
		boolean dixonColesEnabled) {

	public static final int DEFAULT_DECAY_HALF_LIFE_DAYS = ProbabilityModelConfig.DEFAULT_DECAY_HALF_LIFE_DAYS;
	public static final int DEFAULT_MAX_GOALS_PER_TEAM = ProbabilityModelConfig.DEFAULT_MAX_GOALS_PER_TEAM;
	public static final int DEFAULT_MINIMUM_TEAM_MATCHES = ProbabilityModelConfig.DEFAULT_MINIMUM_TEAM_MATCHES;

	/**
	 * Five weighted league-average pseudo-matches. Same order of magnitude as
	 * {@code minimumTeamMatches}. Chosen before evaluation; not fitted to ROI.
	 */
	public static final BigDecimal DEFAULT_ATTACK_DEFENCE_SHRINKAGE_STRENGTH = new BigDecimal("5");

	public ProbabilityModelV2Config {
		if (decayHalfLifeDays <= 0) {
			throw new ProbabilityModelException("decayHalfLifeDays must be > 0");
		}
		if (maxGoalsPerTeam < 1) {
			throw new ProbabilityModelException("maxGoalsPerTeam must be >= 1");
		}
		if (minimumTeamMatches < 1) {
			throw new ProbabilityModelException("minimumTeamMatches must be >= 1");
		}
		if (attackDefenceShrinkageStrength == null) {
			throw new ProbabilityModelException("attackDefenceShrinkageStrength is required");
		}
		if (attackDefenceShrinkageStrength.compareTo(BigDecimal.ZERO) < 0) {
			throw new ProbabilityModelException("attackDefenceShrinkageStrength must be >= 0");
		}
		attackDefenceShrinkageStrength = attackDefenceShrinkageStrength.stripTrailingZeros();
	}

	public static ProbabilityModelV2Config defaults() {
		return new ProbabilityModelV2Config(
				DEFAULT_DECAY_HALF_LIFE_DAYS,
				DEFAULT_MAX_GOALS_PER_TEAM,
				DEFAULT_MINIMUM_TEAM_MATCHES,
				DEFAULT_ATTACK_DEFENCE_SHRINKAGE_STRENGTH,
				true);
	}

	public static ProbabilityModelV2Config shrinkageOnly(BigDecimal shrinkageStrength) {
		return new ProbabilityModelV2Config(
				DEFAULT_DECAY_HALF_LIFE_DAYS,
				DEFAULT_MAX_GOALS_PER_TEAM,
				DEFAULT_MINIMUM_TEAM_MATCHES,
				shrinkageStrength,
				false);
	}

	public ProbabilityModelConfig sharedPoissonConfig() {
		return new ProbabilityModelConfig(decayHalfLifeDays, maxGoalsPerTeam, minimumTeamMatches);
	}
}
