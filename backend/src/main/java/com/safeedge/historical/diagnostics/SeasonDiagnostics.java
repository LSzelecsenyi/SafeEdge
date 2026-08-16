package com.safeedge.historical.diagnostics;

public record SeasonDiagnostics(String seasonDisplay, int predictionCount, UnitStakeSummary candidates) {

	public SeasonDiagnostics {
		if (seasonDisplay == null || seasonDisplay.isBlank()) {
			throw new IllegalArgumentException("seasonDisplay is required");
		}
		if (predictionCount < 0) {
			throw new IllegalArgumentException("predictionCount must be >= 0");
		}
		if (candidates == null) {
			throw new IllegalArgumentException("candidates are required");
		}
	}
}
