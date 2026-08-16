package com.safeedge.historical.diagnostics;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import com.safeedge.probability.JointDixonColesFitRecorder;
import com.safeedge.probability.JointDixonColesFitSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Extracts v1/v2/v3 comparison metrics from already-built walk-forward
 * diagnostics. Does not refit models or change CandidateEngine / BacktestEngine.
 */
public final class ProbabilityModelV3ComparisonEngine {

	private static final BigDecimal THREE = new BigDecimal("0.03");
	private static final BigDecimal FIVE = new BigDecimal("0.05");
	private static final BigDecimal TEN = new BigDecimal("0.10");
	private static final BigDecimal TWENTY = new BigDecimal("0.20");
	private static final BigDecimal THIRTY = new BigDecimal("0.30");

	private final ProbabilityModelV2ComparisonEngine v2Engine;
	private final EdgeQualityDiagnosticsEngine edgeQualityEngine;

	public ProbabilityModelV3ComparisonEngine() {
		this(new ProbabilityModelV2ComparisonEngine(), new EdgeQualityDiagnosticsEngine());
	}

	public ProbabilityModelV3ComparisonEngine(
			ProbabilityModelV2ComparisonEngine v2Engine, EdgeQualityDiagnosticsEngine edgeQualityEngine) {
		if (v2Engine == null || edgeQualityEngine == null) {
			throw new IllegalArgumentException("comparison engines are required");
		}
		this.v2Engine = v2Engine;
		this.edgeQualityEngine = edgeQualityEngine;
	}

	public ProbabilityModelV3Comparison compare(
			CanonicalCompetition competition,
			HistoricalWalkForwardBuildOutput v1Output,
			EdgeQualityReport v1Edge,
			BaselineDiagnosticsReport v1Baseline,
			HistoricalStrategyComparisonResult v1Strategies,
			HistoricalWalkForwardBuildOutput v2Output,
			EdgeQualityReport v2Edge,
			BaselineDiagnosticsReport v2Baseline,
			HistoricalStrategyComparisonResult v2Strategies,
			HistoricalWalkForwardBuildOutput v3Output,
			EdgeQualityReport v3Edge,
			BaselineDiagnosticsReport v3Baseline,
			HistoricalStrategyComparisonResult v3Strategies,
			JointDixonColesFitRecorder v3Recorder) {
		ProbabilityModelV2LeagueMetrics v1 = v2Engine.metrics(v1Output, v1Edge, v1Baseline, v1Strategies, List.of(), false);
		ProbabilityModelV2LeagueMetrics v2 = v2Engine.metrics(v2Output, v2Edge, v2Baseline, v2Strategies, List.of(), false);
		ProbabilityModelV2LeagueMetrics v3 = v2Engine.metrics(v3Output, v3Edge, v3Baseline, v3Strategies, List.of(), false);
		return new ProbabilityModelV3Comparison(
				competition,
				v1,
				v2,
				v3,
				extras(v1Output, v1Edge, null),
				extras(v2Output, v2Edge, null),
				extras(v3Output, v3Edge, v3Recorder));
	}

	private ProbabilityModelV3Extras extras(
			HistoricalWalkForwardBuildOutput output,
			EdgeQualityReport edge,
			JointDixonColesFitRecorder recorder) {
		WalkForwardBuildStats stats = output.dataset().stats();
		List<EdgeQualityCandidate> candidates = edgeQualityEngine.assemble(output.dataset());
		HighEdgeThresholdDiagnostics at3 = EdgeQualityDiagnosticsEngine.highEdgeAt(candidates, THREE);
		HighEdgeThresholdDiagnostics at5 = EdgeQualityDiagnosticsEngine.highEdgeAt(candidates, FIVE);
		HighEdgeThresholdDiagnostics at10 = EdgeQualityDiagnosticsEngine.highEdgeAt(candidates, TEN);
		HighEdgeThresholdDiagnostics at20 = EdgeQualityDiagnosticsEngine.highEdgeAt(candidates, TWENTY);
		HighEdgeThresholdDiagnostics at30 = EdgeQualityDiagnosticsEngine.highEdgeAt(candidates, THIRTY);
		return new ProbabilityModelV3Extras(
				stats.matchesLoaded(),
				stats.matchesEvaluated(),
				stats.predictionsAvailable(),
				stats.matchesSkippedInsufficientHistory(),
				stats.matchesSkippedFittingFailed(),
				stats.candidatesGenerated(),
				stats.positiveEvCandidates(),
				stats.zeroEvCandidates(),
				stats.negativeEvCandidates(),
				calibration(at3),
				calibration(at5),
				fiveWay(at3),
				fiveWay(at5),
				fiveWay(at10),
				fiveWay(at20),
				fiveWay(at30),
				edge.edgeDeciles(),
				edge.settlementBySide(),
				edge.settlementByLineFamily(),
				edge.seasonStability(),
				optimizer(recorder));
	}

	static HighEdgeCalibrationSnapshot calibration(HighEdgeThresholdDiagnostics row) {
		EdgeQualityGroupSummary summary = row.summary();
		return new HighEdgeCalibrationSnapshot(
				row.threshold(),
				summary.n(),
				summary.averageEdge(),
				summary.unitStakeRoi(),
				summary.settlementCalibration().win().averagePredictedProbability(),
				summary.settlementCalibration().win().actualFrequency(),
				summary.settlementCalibration().loss().averagePredictedProbability(),
				summary.settlementCalibration().loss().actualFrequency());
	}

	static HighEdgeFiveWaySnapshot fiveWay(HighEdgeThresholdDiagnostics row) {
		EdgeQualityGroupSummary summary = row.summary();
		SettlementCalibration calibration = summary.settlementCalibration();
		return new HighEdgeFiveWaySnapshot(
				row.threshold(),
				summary.n(),
				summary.averageEdge(),
				summary.unitStakeRoi(),
				calibration.win(),
				calibration.halfWin(),
				calibration.push(),
				calibration.halfLoss(),
				calibration.loss());
	}

	static JointDixonColesOptimizerSummary optimizer(JointDixonColesFitRecorder recorder) {
		if (recorder == null) {
			return new JointDixonColesOptimizerSummary(
					0, 0, 0, null, null, 0, emptyQuantiles(), emptyQuantiles(), emptyQuantiles(),
					new RhoSummary(0, null, null, null), true, false);
		}
		List<JointDixonColesFitSnapshot> snapshots = recorder.snapshots();
		List<BigDecimal> iterations = map(snapshots, snapshot -> BigDecimal.valueOf(snapshot.iterations()));
		List<BigDecimal> attack = concat(
				map(snapshots, JointDixonColesFitSnapshot::homeAttack),
				map(snapshots, JointDixonColesFitSnapshot::awayAttack));
		List<BigDecimal> defence = concat(
				map(snapshots, JointDixonColesFitSnapshot::homeDefence),
				map(snapshots, JointDixonColesFitSnapshot::awayDefence));
		List<BigDecimal> homeAdvantage = map(snapshots, JointDixonColesFitSnapshot::homeAdvantage);
		List<BigDecimal> rho = map(snapshots, JointDixonColesFitSnapshot::rho);
		int converged = 0;
		int maxIterations = 0;
		boolean finite = true;
		for (JointDixonColesFitSnapshot snapshot : snapshots) {
			if (snapshot.converged()) {
				converged++;
			}
			maxIterations = Math.max(maxIterations, snapshot.iterations());
			finite = finite && finite(snapshot.intercept()) && finite(snapshot.homeAdvantage())
					&& finite(snapshot.homeAttack()) && finite(snapshot.homeDefence())
					&& finite(snapshot.awayAttack()) && finite(snapshot.awayDefence())
					&& finite(snapshot.lambdaHome()) && finite(snapshot.lambdaAway())
					&& finite(snapshot.rho());
		}
		BigDecimal medianHa = DiagnosticMath.median(homeAdvantage);
		return new JointDixonColesOptimizerSummary(
				snapshots.size(),
				recorder.fittingFailures(),
				converged,
				DiagnosticMath.average(iterations),
				DiagnosticMath.median(iterations),
				maxIterations,
				DiagnosticMath.quantiles(attack),
				DiagnosticMath.quantiles(defence),
				DiagnosticMath.quantiles(homeAdvantage),
				rhoSummary(rho),
				finite,
				medianHa != null && medianHa.compareTo(BigDecimal.ZERO) > 0);
	}

	private static RhoSummary rhoSummary(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return new RhoSummary(0, null, null, null);
		}
		List<BigDecimal> sorted = new ArrayList<>(values);
		sorted.sort(BigDecimal::compareTo);
		return new RhoSummary(values.size(), sorted.getFirst(), DiagnosticMath.median(sorted), sorted.getLast());
	}

	private static List<BigDecimal> map(
			List<JointDixonColesFitSnapshot> snapshots, Function<JointDixonColesFitSnapshot, BigDecimal> getter) {
		List<BigDecimal> values = new ArrayList<>(snapshots.size());
		for (JointDixonColesFitSnapshot snapshot : snapshots) {
			BigDecimal value = getter.apply(snapshot);
			if (value != null) {
				values.add(value);
			}
		}
		return values;
	}

	private static List<BigDecimal> concat(List<BigDecimal> left, List<BigDecimal> right) {
		List<BigDecimal> all = new ArrayList<>(left.size() + right.size());
		all.addAll(left);
		all.addAll(right);
		return all;
	}

	private static boolean finite(BigDecimal value) {
		return value != null && Double.isFinite(value.doubleValue());
	}

	private static EdgeQuantiles emptyQuantiles() {
		return new EdgeQuantiles(null, null, null, null, null, null, null, null, null);
	}
}
