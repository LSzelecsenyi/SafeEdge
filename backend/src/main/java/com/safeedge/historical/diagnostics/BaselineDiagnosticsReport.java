package com.safeedge.historical.diagnostics;

import java.util.List;

public record BaselineDiagnosticsReport(
		CandidateOverview overview,
		List<EdgeBucketDiagnostics> edgeBuckets,
		List<CandidateSubsetDiagnostics> positiveEdgeThresholds,
		List<OddsBucketDiagnostics> oddsBuckets,
		List<AhLineDiagnostics> ahLines,
		List<LineFamilyDiagnostics> lineFamilies,
		List<SideDiagnostics> sides,
		List<SeasonDiagnostics> seasons,
		EdgeSignDiagnostics edgeSign,
		GoalCalibrationDiagnostics goalCalibration,
		MarginCalibrationDiagnostics marginCalibration,
		EdgeQuantiles allCandidateEdgeQuantiles,
		EdgeQuantiles positiveEdgeQuantiles,
		PositiveEdgeConcentration positiveEdgeConcentration,
		List<CandidateSubsetDiagnostics> originalOddsRangeSubsets,
		List<StrategyAcceptedBetDiagnostics> strategyAcceptedBets) {

	public BaselineDiagnosticsReport {
		if (overview == null) {
			throw new IllegalArgumentException("overview is required");
		}
		if (edgeSign == null) {
			throw new IllegalArgumentException("edgeSign is required");
		}
		if (goalCalibration == null) {
			throw new IllegalArgumentException("goalCalibration is required");
		}
		if (marginCalibration == null) {
			throw new IllegalArgumentException("marginCalibration is required");
		}
		if (allCandidateEdgeQuantiles == null) {
			throw new IllegalArgumentException("allCandidateEdgeQuantiles are required");
		}
		if (positiveEdgeQuantiles == null) {
			throw new IllegalArgumentException("positiveEdgeQuantiles are required");
		}
		if (positiveEdgeConcentration == null) {
			throw new IllegalArgumentException("positiveEdgeConcentration is required");
		}
		edgeBuckets = List.copyOf(edgeBuckets == null ? List.of() : edgeBuckets);
		positiveEdgeThresholds = List.copyOf(positiveEdgeThresholds == null ? List.of() : positiveEdgeThresholds);
		oddsBuckets = List.copyOf(oddsBuckets == null ? List.of() : oddsBuckets);
		ahLines = List.copyOf(ahLines == null ? List.of() : ahLines);
		lineFamilies = List.copyOf(lineFamilies == null ? List.of() : lineFamilies);
		sides = List.copyOf(sides == null ? List.of() : sides);
		seasons = List.copyOf(seasons == null ? List.of() : seasons);
		originalOddsRangeSubsets =
				List.copyOf(originalOddsRangeSubsets == null ? List.of() : originalOddsRangeSubsets);
		strategyAcceptedBets = List.copyOf(strategyAcceptedBets == null ? List.of() : strategyAcceptedBets);
	}
}
