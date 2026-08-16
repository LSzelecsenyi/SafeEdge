package com.safeedge.historical.diagnostics;

import com.safeedge.probability.ProbabilityModelV2Config;
import java.util.List;

public record ProbabilityModelV2DevelopmentReport(
		ProbabilityModelV2Config v2Config,
		List<ProbabilityModelV2LeagueRun> leagues,
		ProbabilityModelV2Classification classification,
		List<String> classificationReasons) {

	public ProbabilityModelV2DevelopmentReport {
		if (v2Config == null) {
			throw new IllegalArgumentException("v2Config is required");
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
