package com.safeedge.historical.diagnostics;

import com.safeedge.probability.ProbabilityModelV3Config;
import java.util.List;

public record ProbabilityModelV3DevelopmentReport(
		ProbabilityModelV3Config v3Config,
		List<ProbabilityModelV3LeagueRun> leagues,
		ProbabilityModelV3Classification classification,
		List<String> classificationReasons) {

	public ProbabilityModelV3DevelopmentReport {
		if (v3Config == null) {
			throw new IllegalArgumentException("v3Config is required");
		}
		if (classification == null) {
			throw new IllegalArgumentException("classification is required");
		}
		leagues = List.copyOf(leagues == null ? List.of() : leagues);
		classificationReasons = List.copyOf(classificationReasons == null ? List.of() : classificationReasons);
		if (leagues.size() != ProbabilityModelDevelopmentLeagues.DEVELOPMENT.size()) {
			throw new IllegalArgumentException("development report requires Premier League, Bundesliga, and Serie A");
		}
	}
}
