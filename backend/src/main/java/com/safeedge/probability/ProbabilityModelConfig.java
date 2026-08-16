package com.safeedge.probability;

/**
 * Immutable Poisson v1 configuration. Values are modelling assumptions, not
 * proven optima.
 */
public record ProbabilityModelConfig(int decayHalfLifeDays, int maxGoalsPerTeam, int minimumTeamMatches) {

	public static final int DEFAULT_DECAY_HALF_LIFE_DAYS = 180;
	public static final int DEFAULT_MAX_GOALS_PER_TEAM = 10;
	public static final int DEFAULT_MINIMUM_TEAM_MATCHES = 5;

	public ProbabilityModelConfig {
		if (decayHalfLifeDays <= 0) {
			throw new ProbabilityModelException("decayHalfLifeDays must be > 0");
		}
		if (maxGoalsPerTeam < 1) {
			throw new ProbabilityModelException("maxGoalsPerTeam must be >= 1");
		}
		if (minimumTeamMatches < 1) {
			throw new ProbabilityModelException("minimumTeamMatches must be >= 1");
		}
	}

	public static ProbabilityModelConfig defaults() {
		return new ProbabilityModelConfig(
				DEFAULT_DECAY_HALF_LIFE_DAYS, DEFAULT_MAX_GOALS_PER_TEAM, DEFAULT_MINIMUM_TEAM_MATCHES);
	}
}
