package com.safeedge.historical.diagnostics;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.evaluation.HistoricalPredictionSnapshot;
import com.safeedge.historical.evaluation.HistoricalStrategyComparisonResult;
import com.safeedge.historical.evaluation.HistoricalWalkForwardBuildOutput;
import com.safeedge.historical.evaluation.NamedBacktestResult;
import com.safeedge.probability.DixonColesFitSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extracts v1/v2 comparison metrics from already-built walk-forward diagnostics.
 * Does not refit models or change CandidateEngine / BacktestEngine.
 */
public final class ProbabilityModelV2ComparisonEngine {

	private static final BigDecimal TEN = new BigDecimal("0.10");
	private static final BigDecimal TWENTY = new BigDecimal("0.20");
	private static final BigDecimal THIRTY = new BigDecimal("0.30");

	private final EdgeQualityDiagnosticsEngine edgeQualityEngine;

	public ProbabilityModelV2ComparisonEngine() {
		this(new EdgeQualityDiagnosticsEngine());
	}

	public ProbabilityModelV2ComparisonEngine(EdgeQualityDiagnosticsEngine edgeQualityEngine) {
		if (edgeQualityEngine == null) {
			throw new IllegalArgumentException("edgeQualityEngine is required");
		}
		this.edgeQualityEngine = edgeQualityEngine;
	}

	public ProbabilityModelComparison compare(
			CanonicalCompetition competition,
			HistoricalWalkForwardBuildOutput v1Output,
			EdgeQualityReport v1Edge,
			BaselineDiagnosticsReport v1Baseline,
			HistoricalStrategyComparisonResult v1Strategies,
			HistoricalWalkForwardBuildOutput v2Output,
			EdgeQualityReport v2Edge,
			BaselineDiagnosticsReport v2Baseline,
			HistoricalStrategyComparisonResult v2Strategies,
			List<DixonColesFitSnapshot> v2Fits) {
		ProbabilityModelV2LeagueMetrics v1 = metrics(v1Output, v1Edge, v1Baseline, v1Strategies, List.of(), false);
		ProbabilityModelV2LeagueMetrics v2 = metrics(v2Output, v2Edge, v2Baseline, v2Strategies, v2Fits, true);
		return new ProbabilityModelComparison(
				competition, v1, v2, lambdaDeltas(v1Output, v2Output, true), lambdaDeltas(v1Output, v2Output, false));
	}

	ProbabilityModelV2LeagueMetrics metrics(
			HistoricalWalkForwardBuildOutput output,
			EdgeQualityReport edge,
			BaselineDiagnosticsReport baseline,
			HistoricalStrategyComparisonResult strategies,
			List<DixonColesFitSnapshot> fits,
			boolean v2) {
		List<EdgeQualityCandidate> candidates = edgeQualityEngine.assemble(output.dataset());
		return new ProbabilityModelV2LeagueMetrics(
				output.dataset().stats().predictionsAvailable(),
				edge.analyzedCandidateCount(),
				output.dataset().stats().averageActualScoreLogLoss(),
				output.dataset().stats().logLossObservations(),
				baseline.goalCalibration(),
				baseline.marginCalibration().categories(),
				lowScores(output.predictions()),
				edge.rankQuality(),
				deciles(edge.edgeDeciles()),
				decileRoiInversions(edge.edgeDeciles()),
				highEdge(edge, TEN),
				highEdge(edge, TWENTY),
				highEdge(edge, THIRTY),
				edge.allCandidates().averageEdge(),
				edge.allCandidates().unitStakeRoi(),
				baseline.allCandidateEdgeQuantiles(),
				quantiles(candidates, row -> row.settlementProbabilities().winProbability()),
				quantiles(candidates, row -> row.settlementProbabilities().lossProbability()),
				quantiles(lambdas(output.predictions(), true)),
				quantiles(lambdas(output.predictions(), false)),
				v2 ? strengths(fits) : List.of(),
				v2 ? rho(fits) : new RhoSummary(0, null, null, null),
				strategySnapshots(strategies));
	}

	private static List<DecileSnapshot> deciles(List<EdgeQualityGroupSummary> rows) {
		List<DecileSnapshot> snapshots = new ArrayList<>();
		for (EdgeQualityGroupSummary row : rows) {
			snapshots.add(new DecileSnapshot(row.key(), row.n(), row.averageEdge(), row.unitStakeRoi()));
		}
		return snapshots;
	}

	static int decileRoiInversions(List<EdgeQualityGroupSummary> deciles) {
		int inversions = 0;
		BigDecimal previousRoi = null;
		for (EdgeQualityGroupSummary row : deciles) {
			if (row.n() < EdgeQualityGroupSummary.LOW_SAMPLE_THRESHOLD || row.unitStakeRoi() == null) {
				continue;
			}
			if (previousRoi != null && row.unitStakeRoi().compareTo(previousRoi) < 0) {
				inversions++;
			}
			previousRoi = row.unitStakeRoi();
		}
		return inversions;
	}

	private static HighEdgeCalibrationSnapshot highEdge(EdgeQualityReport report, BigDecimal threshold) {
		for (HighEdgeThresholdDiagnostics row : report.highEdgeThresholds()) {
			if (row.threshold().compareTo(threshold) == 0) {
				EdgeQualityGroupSummary summary = row.summary();
				return new HighEdgeCalibrationSnapshot(
						threshold,
						summary.n(),
						summary.averageEdge(),
						summary.unitStakeRoi(),
						summary.settlementCalibration().win().averagePredictedProbability(),
						summary.settlementCalibration().win().actualFrequency(),
						summary.settlementCalibration().loss().averagePredictedProbability(),
						summary.settlementCalibration().loss().actualFrequency());
			}
		}
		throw new IllegalArgumentException("missing high-edge threshold " + threshold);
	}

	static LowScoreCalibration lowScores(List<HistoricalPredictionSnapshot> predictions) {
		List<HistoricalPredictionSnapshot> rows = predictions == null ? List.of() : predictions;
		Accumulator score00 = new Accumulator("0-0");
		Accumulator score10 = new Accumulator("1-0");
		Accumulator score01 = new Accumulator("0-1");
		Accumulator score11 = new Accumulator("1-1");
		for (HistoricalPredictionSnapshot snapshot : rows) {
			score00.add(cell(snapshot, 0, 0), snapshot.actualScore().homeGoals() == 0 && snapshot.actualScore().awayGoals() == 0);
			score10.add(cell(snapshot, 1, 0), snapshot.actualScore().homeGoals() == 1 && snapshot.actualScore().awayGoals() == 0);
			score01.add(cell(snapshot, 0, 1), snapshot.actualScore().homeGoals() == 0 && snapshot.actualScore().awayGoals() == 1);
			score11.add(cell(snapshot, 1, 1), snapshot.actualScore().homeGoals() == 1 && snapshot.actualScore().awayGoals() == 1);
		}
		int n = rows.size();
		return new LowScoreCalibration(n, score00.toCell(n), score10.toCell(n), score01.toCell(n), score11.toCell(n));
	}

	private static BigDecimal cell(HistoricalPredictionSnapshot snapshot, int home, int away) {
		for (ScoreProbability entry : snapshot.scoreDistribution().entries()) {
			if (entry.score().homeGoals() == home && entry.score().awayGoals() == away) {
				return entry.probability();
			}
		}
		return BigDecimal.ZERO;
	}

	private static List<BigDecimal> lambdas(List<HistoricalPredictionSnapshot> predictions, boolean home) {
		List<BigDecimal> values = new ArrayList<>();
		for (HistoricalPredictionSnapshot snapshot : predictions) {
			values.add(home ? snapshot.homeExpectedGoals() : snapshot.awayExpectedGoals());
		}
		return values;
	}

	private static EdgeQuantiles lambdaDeltas(
			HistoricalWalkForwardBuildOutput v1, HistoricalWalkForwardBuildOutput v2, boolean home) {
		Map<String, HistoricalPredictionSnapshot> v2ByEvent = new LinkedHashMap<>();
		for (HistoricalPredictionSnapshot snapshot : v2.predictions()) {
			v2ByEvent.put(snapshot.eventId(), snapshot);
		}
		List<BigDecimal> deltas = new ArrayList<>();
		for (HistoricalPredictionSnapshot first : v1.predictions()) {
			HistoricalPredictionSnapshot second = v2ByEvent.get(first.eventId());
			if (second == null) {
				continue;
			}
			BigDecimal left = home ? first.homeExpectedGoals() : first.awayExpectedGoals();
			BigDecimal right = home ? second.homeExpectedGoals() : second.awayExpectedGoals();
			deltas.add(right.subtract(left));
		}
		return DiagnosticMath.quantiles(deltas);
	}

	private static List<StrengthQuantileReport> strengths(List<DixonColesFitSnapshot> fits) {
		return List.of(
				strength("homeAttack", fits, DixonColesFitSnapshot::rawHomeAttackStrength, DixonColesFitSnapshot::homeAttackStrength),
				strength("homeDefence", fits, DixonColesFitSnapshot::rawHomeDefenceStrength, DixonColesFitSnapshot::homeDefenceStrength),
				strength("awayAttack", fits, DixonColesFitSnapshot::rawAwayAttackStrength, DixonColesFitSnapshot::awayAttackStrength),
				strength("awayDefence", fits, DixonColesFitSnapshot::rawAwayDefenceStrength, DixonColesFitSnapshot::awayDefenceStrength));
	}

	private static StrengthQuantileReport strength(
			String name,
			List<DixonColesFitSnapshot> fits,
			Function<DixonColesFitSnapshot, BigDecimal> raw,
			Function<DixonColesFitSnapshot, BigDecimal> shrunk) {
		List<BigDecimal> rawValues = new ArrayList<>();
		List<BigDecimal> shrunkValues = new ArrayList<>();
		for (DixonColesFitSnapshot snapshot : fits) {
			rawValues.add(raw.apply(snapshot));
			shrunkValues.add(shrunk.apply(snapshot));
		}
		return new StrengthQuantileReport(
				name,
				DiagnosticMath.quantiles(rawValues),
				DiagnosticMath.quantiles(shrunkValues),
				madFromOne(rawValues),
				madFromOne(shrunkValues));
	}

	private static BigDecimal madFromOne(List<BigDecimal> values) {
		if (values.isEmpty()) {
			return null;
		}
		List<BigDecimal> deviations = new ArrayList<>(values.size());
		for (BigDecimal value : values) {
			deviations.add(value.subtract(BigDecimal.ONE).abs());
		}
		return DiagnosticMath.average(deviations);
	}

	private static RhoSummary rho(List<DixonColesFitSnapshot> fits) {
		List<BigDecimal> values = new ArrayList<>();
		for (DixonColesFitSnapshot snapshot : fits) {
			if (snapshot.rho() != null) {
				values.add(snapshot.rho());
			}
		}
		if (values.isEmpty()) {
			return new RhoSummary(0, null, null, null);
		}
		List<BigDecimal> sorted = new ArrayList<>(values);
		sorted.sort(BigDecimal::compareTo);
		return new RhoSummary(
				values.size(),
				sorted.getFirst(),
				DiagnosticMath.median(sorted),
				sorted.getLast());
	}

	private static List<StrategySecondarySnapshot> strategySnapshots(HistoricalStrategyComparisonResult comparison) {
		List<StrategySecondarySnapshot> rows = new ArrayList<>();
		for (NamedBacktestResult named : comparison.strategyResults()) {
			rows.add(new StrategySecondarySnapshot(
					named.name(), named.result().counts().betsAccepted(), named.result().metrics().roi()));
		}
		return rows;
	}

	private static EdgeQuantiles quantiles(List<EdgeQualityCandidate> candidates, Function<EdgeQualityCandidate, BigDecimal> getter) {
		List<BigDecimal> values = new ArrayList<>(candidates.size());
		for (EdgeQualityCandidate candidate : candidates) {
			values.add(getter.apply(candidate));
		}
		return DiagnosticMath.quantiles(values);
	}

	private static EdgeQuantiles quantiles(List<BigDecimal> values) {
		return DiagnosticMath.quantiles(values);
	}

	private static final class Accumulator {
		private final String scoreline;
		private BigDecimal predictedSum = BigDecimal.ZERO;
		private int actualCount;

		private Accumulator(String scoreline) {
			this.scoreline = scoreline;
		}

		private void add(BigDecimal predicted, boolean actual) {
			predictedSum = predictedSum.add(predicted, DiagnosticMath.MATH);
			if (actual) {
				actualCount++;
			}
		}

		private LowScoreCellCalibration toCell(int n) {
			return new LowScoreCellCalibration(
					scoreline, DiagnosticMath.divide(predictedSum, n), DiagnosticMath.divide(BigDecimal.valueOf(actualCount), n), actualCount);
		}
	}
}
