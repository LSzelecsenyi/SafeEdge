package com.safeedge.historical.diagnostics;

import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import java.util.List;

public record EdgeQualityReport(
		WalkForwardBuildStats datasetStats,
		int analyzedCandidateCount,
		EdgeQualityGroupSummary allCandidates,
		RankQualityStats rankQuality,
		List<EdgeQualityGroupSummary> edgeBuckets,
		List<EdgeQualityGroupSummary> edgeDeciles,
		List<EdgeQualityGroupSummary> settlementByEdgeBucket,
		List<EdgeQualityGroupSummary> settlementByAhLine,
		List<EdgeQualityGroupSummary> settlementByLineFamily,
		List<EdgeQualityGroupSummary> settlementBySide,
		List<EdgeQualityGroupSummary> settlementByOddsBucket,
		List<CrossCellDiagnostics> edgeByOdds,
		List<CrossCellDiagnostics> edgeByAhLine,
		List<CrossCellDiagnostics> edgeByLineFamily,
		List<CrossCellDiagnostics> sideByLineFamily,
		List<CrossCellDiagnostics> sideByAhLine,
		List<SeasonStabilityRow> seasonStability,
		List<CrossCellDiagnostics> seasonByLineFamily,
		List<CrossCellDiagnostics> seasonByEdgeBucket,
		List<HighEdgeThresholdDiagnostics> highEdgeThresholds,
		List<ForensicCandidateRow> topPredictedEdges,
		List<OverroundGroup> overroundBySeason,
		List<OverroundGroup> overroundByAhLine,
		List<OverroundGroup> overroundByOddsBucket,
		TwoSidedCoherence twoSidedCoherence,
		List<DisagreementGroup> disagreementGroups,
		List<PositiveLineForensics> positiveHandicapLines,
		List<NamedMeanInterval> confidenceIntervals,
		ConsistencyChecks consistency,
		List<StrategyRegressionSnapshot> strategyRegression) {

	public EdgeQualityReport {
		if (datasetStats == null) {
			throw new IllegalArgumentException("datasetStats are required");
		}
		if (allCandidates == null) {
			throw new IllegalArgumentException("allCandidates is required");
		}
		if (rankQuality == null) {
			throw new IllegalArgumentException("rankQuality is required");
		}
		if (twoSidedCoherence == null) {
			throw new IllegalArgumentException("twoSidedCoherence is required");
		}
		if (consistency == null) {
			throw new IllegalArgumentException("consistency is required");
		}
		edgeBuckets = copy(edgeBuckets);
		edgeDeciles = copy(edgeDeciles);
		settlementByEdgeBucket = copy(settlementByEdgeBucket);
		settlementByAhLine = copy(settlementByAhLine);
		settlementByLineFamily = copy(settlementByLineFamily);
		settlementBySide = copy(settlementBySide);
		settlementByOddsBucket = copy(settlementByOddsBucket);
		edgeByOdds = copy(edgeByOdds);
		edgeByAhLine = copy(edgeByAhLine);
		edgeByLineFamily = copy(edgeByLineFamily);
		sideByLineFamily = copy(sideByLineFamily);
		sideByAhLine = copy(sideByAhLine);
		seasonStability = copy(seasonStability);
		seasonByLineFamily = copy(seasonByLineFamily);
		seasonByEdgeBucket = copy(seasonByEdgeBucket);
		highEdgeThresholds = copy(highEdgeThresholds);
		topPredictedEdges = copy(topPredictedEdges);
		overroundBySeason = copy(overroundBySeason);
		overroundByAhLine = copy(overroundByAhLine);
		overroundByOddsBucket = copy(overroundByOddsBucket);
		disagreementGroups = copy(disagreementGroups);
		positiveHandicapLines = copy(positiveHandicapLines);
		confidenceIntervals = copy(confidenceIntervals);
		strategyRegression = copy(strategyRegression);
	}

	private static <T> List<T> copy(List<T> values) {
		return List.copyOf(values == null ? List.of() : values);
	}
}
