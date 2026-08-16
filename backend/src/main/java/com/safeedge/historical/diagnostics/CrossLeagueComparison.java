package com.safeedge.historical.diagnostics;

public record CrossLeagueComparison(
		LeagueDiagnosticSnapshot premierLeague,
		LeagueDiagnosticSnapshot bundesliga,
		StructuralPatternFlags premierLeagueFlags,
		StructuralPatternFlags bundesligaFlags,
		StructuralReplicationClassification classification) {

	public CrossLeagueComparison {
		if (premierLeague == null || bundesliga == null) {
			throw new IllegalArgumentException("both league snapshots are required");
		}
		if (premierLeagueFlags == null || bundesligaFlags == null) {
			throw new IllegalArgumentException("pattern flags are required");
		}
		if (classification == null) {
			throw new IllegalArgumentException("classification is required");
		}
	}
}
