package com.safeedge.historical.diagnostics;

import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import java.util.List;

public record CandidateOverview(
		WalkForwardBuildStats datasetStats,
		int analyzedCandidateCount,
		UnitStakeSummary allCandidates) {

	public CandidateOverview {
		if (datasetStats == null) {
			throw new IllegalArgumentException("datasetStats are required");
		}
		if (analyzedCandidateCount < 0) {
			throw new IllegalArgumentException("analyzedCandidateCount must be >= 0");
		}
		if (allCandidates == null) {
			throw new IllegalArgumentException("allCandidates is required");
		}
	}
}
