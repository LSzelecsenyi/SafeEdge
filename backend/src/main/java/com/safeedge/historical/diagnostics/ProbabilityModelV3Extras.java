package com.safeedge.historical.diagnostics;

import java.util.List;

/**
 * Extra v3-report diagnostics that are not part of the frozen v2 metric record.
 * High-edge 3%/5% are computed here so v1/v2 published reports stay unchanged.
 */
public record ProbabilityModelV3Extras(
		int matchesLoaded,
		int matchesEvaluated,
		int predictionsAvailable,
		int skippedInsufficientHistory,
		int skippedFittingFailed,
		int candidates,
		int positiveEv,
		int zeroEv,
		int negativeEv,
		HighEdgeCalibrationSnapshot highEdge3,
		HighEdgeCalibrationSnapshot highEdge5,
		HighEdgeFiveWaySnapshot fiveWay3,
		HighEdgeFiveWaySnapshot fiveWay5,
		HighEdgeFiveWaySnapshot fiveWay10,
		HighEdgeFiveWaySnapshot fiveWay20,
		HighEdgeFiveWaySnapshot fiveWay30,
		List<EdgeQualityGroupSummary> edgeDeciles,
		List<EdgeQualityGroupSummary> bySide,
		List<EdgeQualityGroupSummary> byFamily,
		List<SeasonStabilityRow> bySeason,
		JointDixonColesOptimizerSummary optimizer) {

	public ProbabilityModelV3Extras {
		if (matchesLoaded < 0
				|| matchesEvaluated < 0
				|| predictionsAvailable < 0
				|| skippedInsufficientHistory < 0
				|| skippedFittingFailed < 0
				|| candidates < 0
				|| positiveEv < 0
				|| zeroEv < 0
				|| negativeEv < 0) {
			throw new IllegalArgumentException("counts must be >= 0");
		}
		edgeDeciles = List.copyOf(edgeDeciles == null ? List.of() : edgeDeciles);
		bySide = List.copyOf(bySide == null ? List.of() : bySide);
		byFamily = List.copyOf(byFamily == null ? List.of() : byFamily);
		bySeason = List.copyOf(bySeason == null ? List.of() : bySeason);
	}
}
