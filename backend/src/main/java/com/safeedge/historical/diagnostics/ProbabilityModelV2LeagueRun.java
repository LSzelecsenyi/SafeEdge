package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.probability.DixonColesFitSnapshot;
import com.safeedge.probability.ProbabilityModelV2Config;
import java.util.List;

public record ProbabilityModelV2LeagueRun(
		CanonicalCompetition competition,
		ProbabilityModelV2Config v2Config,
		HistoricalWalkForwardBuildOutput v1Output,
		HistoricalWalkForwardBuildOutput v2Output,
		HistoricalStrategyComparisonResult v1Strategies,
		HistoricalStrategyComparisonResult v2Strategies,
		BaselineDiagnosticsReport v1Baseline,
		BaselineDiagnosticsReport v2Baseline,
		EdgeQualityReport v1Edge,
		EdgeQualityReport v2Edge,
		List<DixonColesFitSnapshot> v2Fits,
		ProbabilityModelComparison comparison) {

	public ProbabilityModelV2LeagueRun {
		if (competition == null
				|| v2Config == null
				|| v1Output == null
				|| v2Output == null
				|| v1Strategies == null
				|| v2Strategies == null
				|| v1Baseline == null
				|| v2Baseline == null
				|| v1Edge == null
				|| v2Edge == null
				|| comparison == null) {
			throw new IllegalArgumentException("v2 league run fields are required");
		}
		ProbabilityModelDevelopmentLeagues.requireDevelopment(competition);
		v2Fits = List.copyOf(v2Fits == null ? List.of() : v2Fits);
	}
}
