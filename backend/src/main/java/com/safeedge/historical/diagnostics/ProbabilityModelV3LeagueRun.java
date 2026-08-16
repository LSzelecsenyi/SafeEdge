package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.probability.JointDixonColesFitSnapshot;
import com.safeedge.probability.ProbabilityModelV3Config;
import java.util.List;

public record ProbabilityModelV3LeagueRun(
		CanonicalCompetition competition,
		ProbabilityModelV3Config v3Config,
		HistoricalWalkForwardBuildOutput v1Output,
		HistoricalWalkForwardBuildOutput v2Output,
		HistoricalWalkForwardBuildOutput v3Output,
		HistoricalStrategyComparisonResult v1Strategies,
		HistoricalStrategyComparisonResult v2Strategies,
		HistoricalStrategyComparisonResult v3Strategies,
		BaselineDiagnosticsReport v1Baseline,
		BaselineDiagnosticsReport v2Baseline,
		BaselineDiagnosticsReport v3Baseline,
		EdgeQualityReport v1Edge,
		EdgeQualityReport v2Edge,
		EdgeQualityReport v3Edge,
		List<JointDixonColesFitSnapshot> v3Fits,
		int v3FittingFailures,
		ProbabilityModelV3Comparison comparison) {

	public ProbabilityModelV3LeagueRun {
		if (competition == null
				|| v3Config == null
				|| v1Output == null
				|| v2Output == null
				|| v3Output == null
				|| v1Strategies == null
				|| v2Strategies == null
				|| v3Strategies == null
				|| v1Baseline == null
				|| v2Baseline == null
				|| v3Baseline == null
				|| v1Edge == null
				|| v2Edge == null
				|| v3Edge == null
				|| comparison == null) {
			throw new IllegalArgumentException("v3 league run fields are required");
		}
		if (v3FittingFailures < 0) {
			throw new IllegalArgumentException("v3FittingFailures must be >= 0");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(competition);
		v3Fits = List.copyOf(v3Fits == null ? List.of() : v3Fits);
	}
}
