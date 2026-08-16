package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.diagnostics.CrossLeagueComparison;
import com.safeedge.historical.diagnostics.EdgeQualityReport;
import com.safeedge.historical.diagnostics.LeagueCoverageSnapshot;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;

public record Baseline003DiagnosticsRun(
		LeagueCoverageSnapshot coverage,
		HistoricalWalkForwardBuildOutput buildOutput,
		HistoricalStrategyComparisonResult comparison,
		BaselineDiagnosticsReport baselineReport,
		EdgeQualityReport edgeQualityReport,
		CrossLeagueComparison crossLeagueComparison) {

	public Baseline003DiagnosticsRun {
		if (coverage == null
				|| buildOutput == null
				|| comparison == null
				|| baselineReport == null
				|| edgeQualityReport == null
				|| crossLeagueComparison == null) {
			throw new IllegalArgumentException("Baseline 003 run fields are required");
		}
	}
}
