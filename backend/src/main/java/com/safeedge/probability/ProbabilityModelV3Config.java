package com.safeedge.probability;

/**
 * Immutable Probability Model v3 configuration. Defaults are modeling
 * assumptions declared before evaluation; they are not ROI-fitted optima.
 *
 * {@code attackRegularization} / {@code defenceRegularization} are L2 weights
 * on centered team parameters. Scale: penalty {@code λ Σ θ²}. With ~20 teams
 * and RMS(θ)≈0.2, {@code Σθ²≈0.8} so {@code λ=5} adds a penalty of a few
 * units versus a weighted log-likelihood of thousands. Modest Gaussian-prior
 * scale ({@code σ ≈ 1/√(2λ) ≈ 0.32} on the log-rate).
 */
public record ProbabilityModelV3Config(
		int decayHalfLifeDays,
		int maxGoalsPerTeam,
		int minimumTeamMatches,
		int minimumLeagueMatches,
		double attackRegularization,
		double defenceRegularization,
		int optimizerMaxIterations,
		double gradientTolerance,
		double rhoScale) {

	public static final int DEFAULT_DECAY_HALF_LIFE_DAYS = ProbabilityModelConfig.DEFAULT_DECAY_HALF_LIFE_DAYS;
	public static final int DEFAULT_MAX_GOALS_PER_TEAM = ProbabilityModelConfig.DEFAULT_MAX_GOALS_PER_TEAM;
	public static final int DEFAULT_MINIMUM_TEAM_MATCHES = ProbabilityModelConfig.DEFAULT_MINIMUM_TEAM_MATCHES;
	public static final int DEFAULT_MINIMUM_LEAGUE_MATCHES = 20;
	public static final double DEFAULT_ATTACK_REGULARIZATION = 5.0d;
	public static final double DEFAULT_DEFENCE_REGULARIZATION = 5.0d;
	public static final int DEFAULT_OPTIMIZER_MAX_ITERATIONS = 80;
	public static final double DEFAULT_GRADIENT_TOLERANCE = 1.0e-5d;
	public static final double DEFAULT_RHO_SCALE = 0.4d;

	public ProbabilityModelV3Config {
		if (decayHalfLifeDays <= 0) {
			throw new ProbabilityModelException("decayHalfLifeDays must be > 0");
		}
		if (maxGoalsPerTeam < 1) {
			throw new ProbabilityModelException("maxGoalsPerTeam must be >= 1");
		}
		if (minimumTeamMatches < 1) {
			throw new ProbabilityModelException("minimumTeamMatches must be >= 1");
		}
		if (minimumLeagueMatches < 1) {
			throw new ProbabilityModelException("minimumLeagueMatches must be >= 1");
		}
		if (!(attackRegularization >= 0.0d) || !Double.isFinite(attackRegularization)) {
			throw new ProbabilityModelException("attackRegularization must be finite and >= 0");
		}
		if (!(defenceRegularization >= 0.0d) || !Double.isFinite(defenceRegularization)) {
			throw new ProbabilityModelException("defenceRegularization must be finite and >= 0");
		}
		if (optimizerMaxIterations < 1) {
			throw new ProbabilityModelException("optimizerMaxIterations must be >= 1");
		}
		if (!(gradientTolerance > 0.0d) || !Double.isFinite(gradientTolerance)) {
			throw new ProbabilityModelException("gradientTolerance must be finite and > 0");
		}
		if (!(rhoScale > 0.0d) || rhoScale > 0.5d || !Double.isFinite(rhoScale)) {
			throw new ProbabilityModelException("rhoScale must be in (0, 0.5]");
		}
	}

	public static ProbabilityModelV3Config defaults() {
		return new ProbabilityModelV3Config(
				DEFAULT_DECAY_HALF_LIFE_DAYS,
				DEFAULT_MAX_GOALS_PER_TEAM,
				DEFAULT_MINIMUM_TEAM_MATCHES,
				DEFAULT_MINIMUM_LEAGUE_MATCHES,
				DEFAULT_ATTACK_REGULARIZATION,
				DEFAULT_DEFENCE_REGULARIZATION,
				DEFAULT_OPTIMIZER_MAX_ITERATIONS,
				DEFAULT_GRADIENT_TOLERANCE,
				DEFAULT_RHO_SCALE);
	}
}
