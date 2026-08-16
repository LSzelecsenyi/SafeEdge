package com.safeedge.historical.service;

import com.safeedge.historical.diagnostics.BaselineDiagnosticsReport;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;

public record BaselineDiagnosticsRun(
		HistoricalWalkForwardBuildOutput buildOutput,
		HistoricalStrategyComparisonResult comparison,
		BaselineDiagnosticsReport report) {

	public BaselineDiagnosticsRun {
		if (buildOutput == null) {
			throw new IllegalArgumentException("buildOutput is required");
		}
		if (comparison == null) {
			throw new IllegalArgumentException("comparison is required");
		}
		if (report == null) {
			throw new IllegalArgumentException("report is required");
		}
	}
}
