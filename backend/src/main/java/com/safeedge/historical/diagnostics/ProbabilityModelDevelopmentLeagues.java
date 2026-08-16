package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import java.util.List;
import java.util.Set;

/**
 * Probability-model development uses Premier League, Bundesliga, and Serie A
 * only. La Liga and Ligue 1 are reserved later validation and must not be
 * inspected during v1/v2/v3 implementation or tuning.
 */
public final class ProbabilityModelDevelopmentLeagues {

	public static final List<CanonicalCompetition> DEVELOPMENT = List.of(
			CanonicalCompetition.PREMIER_LEAGUE,
			CanonicalCompetition.BUNDESLIGA,
			CanonicalCompetition.SERIE_A);

	public static final Set<CanonicalCompetition> RESERVED_VALIDATION = Set.of(
			CanonicalCompetition.LA_LIGA, CanonicalCompetition.LIGUE_1);

	private ProbabilityModelDevelopmentLeagues() {
	}

	public static void requireDevelopment(CanonicalCompetition competition) {
		if (competition == null) {
			throw new IllegalArgumentException("competition is required");
		}
		if (RESERVED_VALIDATION.contains(competition)) {
			throw new IllegalArgumentException(
					competition
							+ " is a reserved validation league and must not be used during probability-model development");
		}
		if (!DEVELOPMENT.contains(competition)) {
			throw new IllegalArgumentException(
					"Probability-model development allows only PREMIER_LEAGUE, BUNDESLIGA, SERIE_A");
		}
	}
}
