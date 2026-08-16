package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.LeagueCoverageSnapshot;
import com.safeedge.historical.diagnostics.ThreeLeagueComparison;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;

public record Baseline004DiagnosticsRun(
		LeagueCoverageSnapshot coverage,
		HistoricalWalkForwardBuildOutput buildOutput,
		HistoricalStrategyComparisonResult comparison,
		BaselineDiagnosticsReport baselineReport,
		EdgeQualityReport edgeQualityReport,
		ThreeLeagueComparison threeLeagueComparison) {

	public Baseline004DiagnosticsRun {
		if (coverage == null
				|| buildOutput == null
				|| comparison == null
				|| baselineReport == null
				|| edgeQualityReport == null
				|| threeLeagueComparison == null) {
			throw new IllegalArgumentException("Baseline 004 run fields are required");
		}
	}
}
