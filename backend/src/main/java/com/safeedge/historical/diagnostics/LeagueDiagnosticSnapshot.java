package com.safeedge.historical.diagnostics;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * League-keyed diagnostic aggregates. Does not contain candidate rows, so two
 * leagues cannot mix betting opportunities.
 */
public record LeagueDiagnosticSnapshot(
		CanonicalCompetition competition,
		HistoricalQuoteSource quoteSource,
		int matchesLoaded,
		int matchesEvaluated,
		int matchesSkippedInsufficientHistory,
		int matchesSkippedMissingQuote,
		PredictionQualitySnapshot predictionQuality,
		int candidateCount,
		int positiveEvCount,
		int zeroEvCount,
		int negativeEvCount,
		BigDecimal averagePredictedEdge,
		BigDecimal averageRealizedReturn,
		BigDecimal calibrationGap,
		BigDecimal spearman,
		BigDecimal pearson,
		List<BucketTrendRow> edgeDeciles,
		List<BucketTrendRow> edgeBuckets,
		List<HighEdgeCalibrationSlice> highEdgeSlices,
		List<AhFamilySnapshot> ahFamilies,
		List<SideSnapshot> sides,
		List<SeasonStabilityRow> seasons,
		List<Integer> missingEvaluationStartYears,
		BigDecimal meanOverround,
		BigDecimal medianOverround,
		List<OverroundSeasonRow> overroundBySeason,
		List<StrategyRegressionSnapshot> strategies,
		List<NamedMeanInterval> confidenceIntervals,
		List<ForensicCandidateRow> topPredictedEdges) {

	public LeagueDiagnosticSnapshot {
		if (competition == null) {
			throw new IllegalArgumentException("competition is required");
		}
		if (quoteSource == null) {
			throw new IllegalArgumentException("quoteSource is required");
		}
		if (predictionQuality == null) {
			throw new IllegalArgumentException("predictionQuality is required");
		}
		averagePredictedEdge = strip(averagePredictedEdge);
		averageRealizedReturn = strip(averageRealizedReturn);
		calibrationGap = strip(calibrationGap);
		spearman = strip(spearman);
		pearson = strip(pearson);
		meanOverround = strip(meanOverround);
		medianOverround = strip(medianOverround);
		edgeDeciles = copy(edgeDeciles);
		edgeBuckets = copy(edgeBuckets);
		highEdgeSlices = copy(highEdgeSlices);
		ahFamilies = copy(ahFamilies);
		sides = copy(sides);
		seasons = copy(seasons);
		missingEvaluationStartYears = copy(missingEvaluationStartYears);
		overroundBySeason = copy(overroundBySeason);
		strategies = copy(strategies);
		confidenceIntervals = copy(confidenceIntervals);
		topPredictedEdges = copy(topPredictedEdges);
	}

	public static LeagueDiagnosticSnapshot fromReports(
			BaselineDiagnosticsReport baseline, EdgeQualityReport edge, List<Integer> missingEvaluationStartYears) {
		if (baseline == null || edge == null) {
			throw new IllegalArgumentException("baseline and edge reports are required");
		}
		WalkForwardBuildStats baselineStats = baseline.overview().datasetStats();
		WalkForwardBuildStats edgeStats = edge.datasetStats();
		if (baselineStats.competition() != edgeStats.competition()) {
			throw new IllegalArgumentException(
					"baseline and edge reports belong to different competitions: "
							+ baselineStats.competition()
							+ " vs "
							+ edgeStats.competition());
		}
		if (baselineStats.quoteSource() != edgeStats.quoteSource()) {
			throw new IllegalArgumentException("quote sources do not match");
		}
		GoalCalibrationDiagnostics goals = baseline.goalCalibration();
		EdgeQualityGroupSummary all = edge.allCandidates();
		return new LeagueDiagnosticSnapshot(
				edgeStats.competition(),
				edgeStats.quoteSource(),
				edgeStats.matchesLoaded(),
				edgeStats.matchesEvaluated(),
				edgeStats.matchesSkippedInsufficientHistory(),
				edgeStats.matchesSkippedMissingQuote(),
				new PredictionQualitySnapshot(
						edgeStats.predictionsAvailable(),
						edgeStats.logLossObservations(),
						edgeStats.averageActualScoreLogLoss(),
						goals.averagePredictedHomeGoals(),
						goals.averageActualHomeGoals(),
						goals.averagePredictedAwayGoals(),
						goals.averageActualAwayGoals(),
						goals.averagePredictedHomeWinProbability(),
						goals.actualHomeWinFrequency(),
						goals.averagePredictedDrawProbability(),
						goals.actualDrawFrequency(),
						goals.averagePredictedAwayWinProbability(),
						goals.actualAwayWinFrequency(),
						baseline.marginCalibration().categories()),
				edge.analyzedCandidateCount(),
				edgeStats.positiveEvCandidates(),
				edgeStats.zeroEvCandidates(),
				edgeStats.negativeEvCandidates(),
				all.averageEdge(),
				all.unitStakeRoi(),
				all.calibrationGap(),
				edge.rankQuality().spearman(),
				edge.rankQuality().pearson(),
				trendRows(edge.edgeDeciles()),
				trendRows(edge.edgeBuckets()),
				highEdge(edge.highEdgeThresholds()),
				families(edge.settlementByLineFamily()),
				sides(edge.settlementBySide()),
				edge.seasonStability(),
				missingEvaluationStartYears,
				edge.twoSidedCoherence().averageOverround(),
				edge.twoSidedCoherence().medianOverround(),
				overroundSeasons(edge.overroundBySeason()),
				edge.strategyRegression(),
				edge.confidenceIntervals(),
				edge.topPredictedEdges());
	}

	private static List<BucketTrendRow> trendRows(List<EdgeQualityGroupSummary> rows) {
		List<BucketTrendRow> out = new ArrayList<>();
		for (EdgeQualityGroupSummary row : rows) {
			out.add(new BucketTrendRow(row.key(), row.n(), row.averageEdge(), row.unitStakeRoi()));
		}
		return List.copyOf(out);
	}

	private static List<HighEdgeCalibrationSlice> highEdge(List<HighEdgeThresholdDiagnostics> rows) {
		List<HighEdgeCalibrationSlice> out = new ArrayList<>();
		for (HighEdgeThresholdDiagnostics row : rows) {
			SettlementCalibration cal = row.summary().settlementCalibration();
			out.add(new HighEdgeCalibrationSlice(
					row.threshold(),
					row.summary().n(),
					row.summary().averageEdge(),
					row.summary().unitStakeRoi(),
					cal.win().averagePredictedProbability(),
					cal.win().actualFrequency(),
					cal.loss().averagePredictedProbability(),
					cal.loss().actualFrequency()));
		}
		return List.copyOf(out);
	}

	private static List<AhFamilySnapshot> families(List<EdgeQualityGroupSummary> rows) {
		List<AhFamilySnapshot> out = new ArrayList<>();
		for (EdgeQualityGroupSummary row : rows) {
			out.add(new AhFamilySnapshot(
					DiagnosticLineFamily.valueOf(row.key()), row.n(), row.averageEdge(), row.unitStakeRoi()));
		}
		return List.copyOf(out);
	}

	private static List<SideSnapshot> sides(List<EdgeQualityGroupSummary> rows) {
		List<SideSnapshot> out = new ArrayList<>();
		for (EdgeQualityGroupSummary row : rows) {
			SelectionType.valueOf(row.key());
			out.add(new SideSnapshot(row.key(), row.n(), row.averageEdge(), row.unitStakeRoi()));
		}
		return List.copyOf(out);
	}

	private static List<OverroundSeasonRow> overroundSeasons(List<OverroundGroup> rows) {
		List<OverroundSeasonRow> out = new ArrayList<>();
		for (OverroundGroup row : rows) {
			out.add(new OverroundSeasonRow(row.key(), row.eventCount(), row.averageOverround()));
		}
		return List.copyOf(out);
	}

	private static <T> List<T> copy(List<T> values) {
		return List.copyOf(values == null ? List.of() : values);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
