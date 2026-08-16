package com.safeedge.historical.diagnostics;

public record ThreeLeagueComparison(
		LeagueDiagnosticSnapshot premierLeague,
		LeagueDiagnosticSnapshot bundesliga,
		LeagueDiagnosticSnapshot serieA,
		StructuralPatternFlags premierLeagueFlags,
		StructuralPatternFlags bundesligaFlags,
		StructuralPatternFlags serieAFlags,
		StructuralReplicationClassification classification) {

	public ThreeLeagueComparison {
		if (premierLeague == null || bundesliga == null || serieA == null) {
			throw new IllegalArgumentException("all three league snapshots are required");
		}
		if (premierLeagueFlags == null || bundesligaFlags == null || serieAFlags == null) {
			throw new IllegalArgumentException("pattern flags are required");
		}
		if (classification == null) {
			throw new IllegalArgumentException("classification is required");
		}
	}
}
