package com.safeedge.historical.diagnostics;

import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D20;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.S19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.S20;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.away;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.blowoutShape;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.dataset;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.home;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.oneNilShape;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.prediction;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.backtest.BacktestEngine;
import com.safeedge.backtest.BacktestRequest;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.bankroll.OwnerId;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.evaluation.HistoricalPredictionSnapshot;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.NamedBacktestResult;
import com.safeedge.settlement.MatchScore;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BaselineDiagnosticsEngineTest {

	private final BaselineDiagnosticsEngine engine = new BaselineDiagnosticsEngine();

	@Nested
	class BucketsAndGrouping {

		@Test
		void edgeBucketBoundariesAreFixed() {
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0"))).isEqualTo(DiagnosticEdgeBucket.NON_POSITIVE);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("-0.01"))).isEqualTo(DiagnosticEdgeBucket.NON_POSITIVE);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.019999"))).isEqualTo(DiagnosticEdgeBucket.OPEN_0_TO_02);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.02"))).isEqualTo(DiagnosticEdgeBucket.FROM_02_TO_05);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.049999"))).isEqualTo(DiagnosticEdgeBucket.FROM_02_TO_05);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.05"))).isEqualTo(DiagnosticEdgeBucket.FROM_05_TO_10);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.099999"))).isEqualTo(DiagnosticEdgeBucket.FROM_05_TO_10);
			assertThat(DiagnosticEdgeBucket.of(new BigDecimal("0.10"))).isEqualTo(DiagnosticEdgeBucket.FROM_10);
		}

		@Test
		void oddsBucketBoundariesAreFixed() {
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.199999"))).isEqualTo(DiagnosticOddsBucket.OPEN_1_TO_120);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.20"))).isEqualTo(DiagnosticOddsBucket.FROM_120_TO_135);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.349999"))).isEqualTo(DiagnosticOddsBucket.FROM_120_TO_135);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.35"))).isEqualTo(DiagnosticOddsBucket.FROM_135_TO_150);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.50"))).isEqualTo(DiagnosticOddsBucket.FROM_150_TO_175);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("1.75"))).isEqualTo(DiagnosticOddsBucket.FROM_175_TO_200);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("2.00"))).isEqualTo(DiagnosticOddsBucket.FROM_200_TO_250);
			assertThat(DiagnosticOddsBucket.of(new BigDecimal("2.50"))).isEqualTo(DiagnosticOddsBucket.FROM_250);
		}

		@Test
		void exactAhLinesAreNotMerged() {
			HistoricalBettingOpportunity a = home("a", "e1", D19, "-0.25", "1.80", "0.01");
			HistoricalBettingOpportunity b = home("b", "e2", D19, "-0.50", "1.80", "0.01");
			HistoricalBettingOpportunity c = home("c", "e3", D19, "-0.250", "1.80", "0.01");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(a, b, c), List.of(result("e1", D19, 1, 0), result("e2", D19, 1, 0), result("e3", D19, 1, 0))),
					List.of(),
					List.of());
			assertThat(report.ahLines()).extracting(row -> row.selectedLine().stripTrailingZeros())
					.containsExactly(new BigDecimal("-0.5"), new BigDecimal("-0.25"));
			assertThat(line(report, "-0.25").candidateCount()).isEqualTo(2);
			assertThat(line(report, "-0.50").candidateCount()).isEqualTo(1);
		}

		@Test
		void homeAndAwayAreGroupedSeparately() {
			HistoricalBettingOpportunity homeRow = home("h", "e1", D19, "0", "1.90", "0.04");
			HistoricalBettingOpportunity awayRow = away("a", "e1", D19, "0", "1.90", "-0.04");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(homeRow, awayRow), List.of(result("e1", D19, 1, 0))),
					List.of(),
					List.of());
			assertThat(side(report, SelectionType.HOME).candidateCount()).isEqualTo(1);
			assertThat(side(report, SelectionType.AWAY).candidateCount()).isEqualTo(1);
			assertThat(side(report, SelectionType.HOME).positiveEdgeCount()).isEqualTo(1);
			assertThat(side(report, SelectionType.AWAY).positiveEdgeCount()).isZero();
		}

		@Test
		void seasonsGroupByEvaluationSeasonDisplay() {
			HistoricalBettingOpportunity a = home("a", "e19", D19, "0", "1.80", "0.01");
			HistoricalBettingOpportunity b = home("b", "e20", D20, "0", "1.80", "0.01");
			List<HistoricalPredictionSnapshot> predictions = List.of(
					prediction("e19", S19, D19, new MatchScore(1, 0), oneNilShape()),
					prediction("e20", S20, D20, new MatchScore(0, 1), oneNilShape()));
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(a, b), List.of(result("e19", D19, 1, 0), result("e20", D20, 0, 1))),
					predictions,
					List.of());
			assertThat(report.seasons()).extracting(SeasonDiagnostics::seasonDisplay)
					.containsExactly("2019/20", "2020/21");
			assertThat(report.seasons().get(0).predictionCount()).isEqualTo(1);
			assertThat(report.seasons().get(0).candidates().candidateCount()).isEqualTo(1);
			assertThat(report.seasons().get(1).predictionCount()).isEqualTo(1);
		}
	}

	@Nested
	class UnitStakeSettlement {

		@Test
		void winUsesPayoutCalculatorOnUnitStake() {
			assertRealized("0", "2.00", 1, 0, "1");
		}

		@Test
		void halfWinUsesPayoutCalculatorOnUnitStake() {
			assertRealized("0.25", "2.00", 0, 0, "0.5");
		}

		@Test
		void pushUsesPayoutCalculatorOnUnitStake() {
			assertRealized("0", "2.00", 0, 0, "0");
		}

		@Test
		void halfLossUsesPayoutCalculatorOnUnitStake() {
			assertRealized("-0.25", "2.00", 0, 0, "-0.5");
		}

		@Test
		void lossUsesPayoutCalculatorOnUnitStake() {
			assertRealized("0", "2.00", 0, 1, "-1");
		}

		private void assertRealized(String line, String odds, int homeGoals, int awayGoals, String expected) {
			HistoricalBettingOpportunity opportunity = home("a", "e1", D19, line, odds, "0.05");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(opportunity), List.of(result("e1", D19, homeGoals, awayGoals))),
					List.of(),
					List.of());
			assertThat(report.overview().allCandidates().unitStakeRoi()).isEqualByComparingTo(expected);
			assertThat(report.overview().allCandidates().averageRealizedReturnRate()).isEqualByComparingTo(expected);
		}
	}

	@Nested
	class CandidatePreservation {

		@Test
		void negativeEdgeCandidatesAreNotDropped() {
			HistoricalBettingOpportunity positive = home("p", "e1", D19, "0", "1.90", "0.05");
			HistoricalBettingOpportunity negative = away("n", "e1", D19, "0", "1.90", "-0.08");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(positive, negative), List.of(result("e1", D19, 1, 0))),
					List.of(),
					List.of());
			assertThat(report.overview().analyzedCandidateCount()).isEqualTo(2);
			assertThat(report.overview().datasetStats().negativeEvCandidates()).isEqualTo(1);
			assertThat(report.edgeSign().negativeEdge().candidateCount()).isEqualTo(1);
			assertThat(report.edgeSign().positiveEdge().candidateCount()).isEqualTo(1);
			assertThat(bucket(report, DiagnosticEdgeBucket.NON_POSITIVE).candidateCount()).isEqualTo(1);
		}

		@Test
		void candidateCountsArePreservedAcrossAllDiagnosticTables() {
			HistoricalBettingOpportunity a = home("a", "e1", D19, "-0.25", "1.25", "-0.10");
			HistoricalBettingOpportunity b = away("b", "e1", D19, "0.25", "1.80", "0.04");
			HistoricalBettingOpportunity c = home("c", "e2", D20, "0", "2.60", "0.12");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(a, b, c), List.of(result("e1", D19, 1, 0), result("e2", D20, 0, 0))),
					List.of(),
					List.of());
			int edgeSum = report.edgeBuckets().stream().mapToInt(row -> row.summary().candidateCount()).sum();
			int oddsSum = report.oddsBuckets().stream().mapToInt(row -> row.summary().candidateCount()).sum();
			int sideSum = report.sides().stream().mapToInt(row -> row.summary().candidateCount()).sum();
			int lineSum = report.ahLines().stream().mapToInt(row -> row.summary().candidateCount()).sum();
			assertThat(edgeSum).isEqualTo(3);
			assertThat(oddsSum).isEqualTo(3);
			assertThat(sideSum).isEqualTo(3);
			assertThat(lineSum).isEqualTo(3);
			assertThat(report.overview().analyzedCandidateCount()).isEqualTo(3);
		}

		@Test
		void emptyBucketUnitStakeRoiIsNull() {
			HistoricalBettingOpportunity onlyNegative = home("a", "e1", D19, "0", "1.80", "-0.05");
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(onlyNegative), List.of(result("e1", D19, 1, 0))),
					List.of(),
					List.of());
			assertThat(bucket(report, DiagnosticEdgeBucket.FROM_10).candidateCount()).isZero();
			assertThat(bucket(report, DiagnosticEdgeBucket.FROM_10).unitStakeRoi()).isNull();
			assertThat(bucket(report, DiagnosticEdgeBucket.FROM_10).unitStakeProfit()).isEqualByComparingTo("0");
		}
	}

	@Nested
	class ThresholdsAndQuantiles {

		@Test
		void positiveEdgeThresholdSubsetsUseFixedCuts() {
			List<HistoricalBettingOpportunity> opportunities = List.of(
					home("n", "e0", D19, "0", "1.80", "-0.01"),
					home("a", "e1", D19, "0", "1.80", "0.01"),
					home("b", "e2", D19, "0", "1.80", "0.02"),
					home("c", "e3", D19, "0", "1.80", "0.03"),
					home("d", "e4", D19, "0", "1.80", "0.05"),
					home("e", "e5", D19, "0", "1.80", "0.10"));
			List<HistoricalEventResult> results = List.of(
					result("e0", D19, 1, 0),
					result("e1", D19, 1, 0),
					result("e2", D19, 1, 0),
					result("e3", D19, 1, 0),
					result("e4", D19, 1, 0),
					result("e5", D19, 1, 0));
			BaselineDiagnosticsReport report = engine.analyze(dataset(opportunities, results), List.of(), List.of());
			assertThat(subset(report, "edge > 0").candidateCount()).isEqualTo(5);
			assertThat(subset(report, "edge >= 0.02").candidateCount()).isEqualTo(4);
			assertThat(subset(report, "edge >= 0.03").candidateCount()).isEqualTo(3);
			assertThat(subset(report, "edge >= 0.05").candidateCount()).isEqualTo(2);
			assertThat(subset(report, "edge >= 0.10").candidateCount()).isEqualTo(1);
		}

		@Test
		void quantilesAreDeterministicNearestRank() {
			assertThat(DiagnosticMath.quantile(
							List.of(
									new BigDecimal("0.00"),
									new BigDecimal("0.10"),
									new BigDecimal("0.20"),
									new BigDecimal("0.30"),
									new BigDecimal("0.40")),
							new BigDecimal("0.50")))
					.isEqualByComparingTo("0.20");
			assertThat(DiagnosticMath.quantile(List.of(new BigDecimal("0.07")), new BigDecimal("0.99")))
					.isEqualByComparingTo("0.07");
			assertThat(DiagnosticMath.quantile(List.of(), new BigDecimal("0.50"))).isNull();
			List<HistoricalBettingOpportunity> opportunities = List.of(
					home("a", "e1", D19, "0", "1.80", "0.00"),
					home("b", "e2", D19, "0", "1.80", "0.10"),
					home("c", "e3", D19, "0", "1.80", "0.20"),
					home("d", "e4", D19, "0", "1.80", "0.30"),
					home("e", "e5", D19, "0", "1.80", "0.40"));
			List<HistoricalEventResult> results = List.of(
					result("e1", D19, 0, 0),
					result("e2", D19, 0, 0),
					result("e3", D19, 0, 0),
					result("e4", D19, 0, 0),
					result("e5", D19, 0, 0));
			BaselineDiagnosticsReport first = engine.analyze(dataset(opportunities, results), List.of(), List.of());
			BaselineDiagnosticsReport second = engine.analyze(dataset(opportunities, results), List.of(), List.of());
			assertThat(second.allCandidateEdgeQuantiles()).isEqualTo(first.allCandidateEdgeQuantiles());
			assertThat(first.allCandidateEdgeQuantiles().median()).isEqualByComparingTo("0.20");
			assertThat(first.allCandidateEdgeQuantiles().min()).isEqualByComparingTo("0");
			assertThat(first.allCandidateEdgeQuantiles().max()).isEqualByComparingTo("0.40");
		}
	}

	@Nested
	class ScoreCalibration {

		@Test
		void predictedVersusActualGoalAggregatesSumTheDistribution() {
			HistoricalPredictionSnapshot snapshot = prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape());
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(), List.of()),
					List.of(snapshot),
					List.of());
			assertThat(report.goalCalibration().predictionCount()).isEqualTo(1);
			assertThat(report.goalCalibration().averagePredictedHomeGoals()).isEqualByComparingTo("0.40");
			assertThat(report.goalCalibration().averageActualHomeGoals()).isEqualByComparingTo("1");
			assertThat(report.goalCalibration().averagePredictedAwayGoals()).isEqualByComparingTo("0.30");
			assertThat(report.goalCalibration().averageActualAwayGoals()).isEqualByComparingTo("0");
			assertThat(report.goalCalibration().averagePredictedTotalGoals()).isEqualByComparingTo("0.70");
			assertThat(report.goalCalibration().averageActualTotalGoals()).isEqualByComparingTo("1");
			assertThat(report.goalCalibration().averagePredictedHomeWinProbability()).isEqualByComparingTo("0.40");
			assertThat(report.goalCalibration().actualHomeWinFrequency()).isEqualByComparingTo("1");
		}

		@Test
		void marginBucketsAggregatePredictedAndActualFrequencies() {
			HistoricalPredictionSnapshot first = prediction("e1", S19, D19, new MatchScore(3, 0), blowoutShape());
			HistoricalPredictionSnapshot second = prediction("e2", S19, D19, new MatchScore(0, 0), blowoutShape());
			BaselineDiagnosticsReport report = engine.analyze(
					dataset(List.of(), List.of()),
					List.of(first, second),
					List.of());
			MarginCategoryCalibration homeTwoPlus = report.marginCalibration().categories().stream()
					.filter(row -> row.category() == DiagnosticMarginCategory.HOME_WIN_BY_2_PLUS)
					.findFirst()
					.orElseThrow();
			assertThat(homeTwoPlus.averagePredictedProbability()).isEqualByComparingTo("0.40");
			assertThat(homeTwoPlus.actualCount()).isEqualTo(1);
			assertThat(homeTwoPlus.actualFrequency()).isEqualByComparingTo("0.5");
			ExactMarginCalibration plusThree = report.marginCalibration().exactMargins().stream()
					.filter(row -> row.bucket() == DiagnosticExactMarginBucket.GTE_PLUS_3)
					.findFirst()
					.orElseThrow();
			assertThat(plusThree.averagePredictedProbability()).isEqualByComparingTo("0.20");
			assertThat(plusThree.actualCount()).isEqualTo(1);
		}
	}

	@Nested
	class StrategyAndInvariant {

		@Test
		void acceptedBetCompositionUsesAcceptedDenominatorOnly() {
			HistoricalBettingOpportunity accepted = home("keep", "e1", D19, "-0.25", "1.80", "0.10");
			HistoricalBettingOpportunity rejected = home("skip", "e2", D19, "0", "1.80", "0.01");
			HistoricalWalkForwardDataset data = dataset(
					List.of(accepted, rejected), List.of(result("e1", D19, 1, 0), result("e2", D19, 0, 1)));
			BacktestResult backtest = new BacktestEngine().run(request(data, flatMinEdge("0.03")));
			BaselineDiagnosticsReport report = engine.analyze(
					data,
					List.of(),
					List.of(new NamedBacktestResult("FLAT", flatMinEdge("0.03"), backtest)));
			StrategyAcceptedBetDiagnostics strategy = report.strategyAcceptedBets().getFirst();
			assertThat(strategy.acceptedCount()).isEqualTo(1);
			assertThat(sideAccepted(strategy, SelectionType.HOME)).isEqualTo(1);
			assertThat(sideAccepted(strategy, SelectionType.AWAY)).isZero();
			assertThat(strategy.byAhLine()).hasSize(1);
			assertThat(strategy.byAhLine().getFirst().selectedLine()).isEqualByComparingTo("-0.25");
			assertThat(bucketAccepted(strategy, DiagnosticEdgeBucket.FROM_10)).isEqualTo(1);
			assertThat(bucketAccepted(strategy, DiagnosticEdgeBucket.NON_POSITIVE)).isZero();
		}

		@Test
		void diagnosticsDoNotMutateDatasetOrBacktestResult() {
			HistoricalBettingOpportunity first = home("a", "e1", D19, "0", "2.00", "0.05");
			HistoricalBettingOpportunity second = home("b", "e2", D20, "0", "2.00", "0.05");
			HistoricalWalkForwardDataset data = dataset(
					List.of(first, second), List.of(result("e1", D19, 0, 1), result("e2", D20, 0, 1)));
			StrategyConfig config = flatMinEdge("0.03");
			BacktestEngine backtestEngine = new BacktestEngine();
			BacktestResult before = backtestEngine.run(request(data, config));
			HistoricalWalkForwardDataset snapshot = new HistoricalWalkForwardDataset(
					data.stats(), data.opportunities(), data.eventResults());
			engine.analyze(data, List.of(), List.of(new NamedBacktestResult("FLAT", config, before)));
			BacktestResult after = backtestEngine.run(request(data, config));
			assertThat(after).isEqualTo(before);
			assertThat(data).isEqualTo(snapshot);
			assertThat(data.opportunities()).containsExactlyElementsOf(snapshot.opportunities());
		}

		@Test
		void analyzeIsDeterministic() {
			HistoricalBettingOpportunity opportunity = home("a", "e1", D19, "-1.00", "1.90", "0.04");
			HistoricalWalkForwardDataset data = dataset(List.of(opportunity), List.of(result("e1", D19, 2, 0)));
			List<HistoricalPredictionSnapshot> predictions =
					List.of(prediction("e1", S19, D19, new MatchScore(2, 0), blowoutShape()));
			BaselineDiagnosticsReport first = engine.analyze(data, predictions, List.of());
			BaselineDiagnosticsReport second = engine.analyze(data, predictions, List.of());
			assertThat(second).isEqualTo(first);
		}
	}

	@Test
	void originalOddsRangeIsInclusiveAndIndependentOfEdgeBuckets() {
		HistoricalBettingOpportunity inside = home("a", "e1", D19, "0", "1.15", "0.04");
		HistoricalBettingOpportunity top = home("b", "e2", D19, "0", "1.35", "-0.01");
		HistoricalBettingOpportunity outside = home("c", "e3", D19, "0", "1.36", "0.20");
		BaselineDiagnosticsReport report = engine.analyze(
				dataset(
						List.of(inside, top, outside),
						List.of(result("e1", D19, 1, 0), result("e2", D19, 1, 0), result("e3", D19, 1, 0))),
				List.of(),
				List.of());
		assertThat(original(report, "odds 1.15-1.35").candidateCount()).isEqualTo(2);
		assertThat(original(report, "odds 1.15-1.35 AND edge > 0").candidateCount()).isEqualTo(1);
		assertThat(original(report, "odds 1.15-1.35 AND edge >= 0.03").candidateCount()).isEqualTo(1);
	}

	private static UnitStakeSummary bucket(BaselineDiagnosticsReport report, DiagnosticEdgeBucket bucket) {
		return report.edgeBuckets().stream()
				.filter(row -> row.bucket() == bucket)
				.findFirst()
				.orElseThrow()
				.summary();
	}

	private static UnitStakeSummary side(BaselineDiagnosticsReport report, SelectionType side) {
		return report.sides().stream()
				.filter(row -> row.side() == side)
				.findFirst()
				.orElseThrow()
				.summary();
	}

	private static UnitStakeSummary line(BaselineDiagnosticsReport report, String line) {
		BigDecimal value = new BigDecimal(line);
		return report.ahLines().stream()
				.filter(row -> row.selectedLine().compareTo(value) == 0)
				.findFirst()
				.orElseThrow()
				.summary();
	}

	private static UnitStakeSummary subset(BaselineDiagnosticsReport report, String label) {
		return report.positiveEdgeThresholds().stream()
				.filter(row -> row.label().equals(label))
				.findFirst()
				.orElseThrow()
				.summary();
	}

	private static UnitStakeSummary original(BaselineDiagnosticsReport report, String label) {
		return report.originalOddsRangeSubsets().stream()
				.filter(row -> row.label().equals(label))
				.findFirst()
				.orElseThrow()
				.summary();
	}

	private static int sideAccepted(StrategyAcceptedBetDiagnostics strategy, SelectionType side) {
		return strategy.bySide().stream()
				.filter(row -> row.side() == side)
				.findFirst()
				.orElseThrow()
				.summary()
				.candidateCount();
	}

	private static int bucketAccepted(StrategyAcceptedBetDiagnostics strategy, DiagnosticEdgeBucket bucket) {
		return strategy.byEdgeBucket().stream()
				.filter(row -> row.bucket() == bucket)
				.findFirst()
				.orElseThrow()
				.summary()
				.candidateCount();
	}

	private static BacktestRequest request(HistoricalWalkForwardDataset data, StrategyConfig config) {
		return new BacktestRequest(
				new OwnerId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")),
				new BigDecimal("100000"),
				config,
				data.opportunities(),
				data.eventResults(),
				null);
	}

	private static StrategyConfig flatMinEdge(String minimumEdge) {
		return new StrategyConfig(
				false,
				BigDecimal.ZERO,
				StakingMode.FLAT_STAKE,
				null,
				new BigDecimal("0.02"),
				new BigDecimal("0.02"),
				new BigDecimal(minimumEdge),
				new BigDecimal("0.50"),
				new BigDecimal("0.50"),
				BigDecimal.ONE,
				new BigDecimal("0.05"),
				new BigDecimal("0.08"),
				new BigDecimal("0.50"),
				new BigDecimal("0.90"));
	}
}
