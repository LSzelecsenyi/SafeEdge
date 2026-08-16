package com.safeedge.historical.diagnostics;

import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.BINARY_60;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D20;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.away;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.dataset;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.home;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.priced;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.result;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.twoWay;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.backtest.BacktestEngine;
import com.safeedge.backtest.BacktestRequest;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.bankroll.OwnerId;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.NamedBacktestResult;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EdgeQualityDiagnosticsEngineTest {

	private final EdgeQualityDiagnosticsEngine engine = new EdgeQualityDiagnosticsEngine();

	@Nested
	class Boundaries {

		@Test
		void edgeQualityBucketBoundariesAreFixed() {
			assertThat(EdgeQualityEdgeBucket.of(BigDecimal.ZERO)).isEqualTo(EdgeQualityEdgeBucket.NON_POSITIVE);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.019999"))).isEqualTo(EdgeQualityEdgeBucket.OPEN_0_TO_02);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.02"))).isEqualTo(EdgeQualityEdgeBucket.FROM_02_TO_05);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.10"))).isEqualTo(EdgeQualityEdgeBucket.FROM_10_TO_20);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.199999"))).isEqualTo(EdgeQualityEdgeBucket.FROM_10_TO_20);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.20"))).isEqualTo(EdgeQualityEdgeBucket.FROM_20_TO_30);
			assertThat(EdgeQualityEdgeBucket.of(new BigDecimal("0.30"))).isEqualTo(EdgeQualityEdgeBucket.FROM_30);
		}

		@Test
		void oddsQualityBucketBoundariesAreFixed() {
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("1.749"))).isEqualTo(EdgeQualityOddsBucket.UNDER_175);
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("1.75"))).isEqualTo(EdgeQualityOddsBucket.FROM_175_TO_190);
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("1.90"))).isEqualTo(EdgeQualityOddsBucket.FROM_190_TO_200);
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("2.00"))).isEqualTo(EdgeQualityOddsBucket.FROM_200_TO_210);
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("2.10"))).isEqualTo(EdgeQualityOddsBucket.FROM_210_TO_225);
			assertThat(EdgeQualityOddsBucket.of(new BigDecimal("2.25"))).isEqualTo(EdgeQualityOddsBucket.FROM_225);
		}

		@Test
		void highEdgeThresholdsAreInclusive() {
			HistoricalBettingOpportunity a = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			HistoricalWalkForwardDataset data = dataset(List.of(a), List.of(result("e1", D19, 1, 0)));
			EdgeQualityReport report = engine.analyze(data, List.of());
			assertThat(a.opportunity().edge()).isEqualByComparingTo("0.20");
			assertThat(report.highEdgeThresholds().get(0).threshold()).isEqualByComparingTo("0.10");
			assertThat(report.highEdgeThresholds().get(0).summary().n()).isEqualTo(1);
			assertThat(report.highEdgeThresholds().get(1).summary().n()).isEqualTo(1);
			assertThat(report.highEdgeThresholds().get(2).summary().n()).isZero();
		}
	}

	@Nested
	class Grouping {

		@Test
		void exactQuarterLinesAreNotMerged() {
			HistoricalBettingOpportunity a = home("a", "e1", D19, "-0.25", "1.90", "0.01");
			HistoricalBettingOpportunity b = home("b", "e2", D19, "-0.50", "1.90", "0.01");
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(a, b), List.of(result("e1", D19, 1, 0), result("e2", D19, 1, 0))),
					List.of());
			assertThat(report.settlementByAhLine()).extracting(EdgeQualityGroupSummary::key).containsExactly("-0.5", "-0.25");
		}

		@Test
		void homeAndAwayAreSeparated() {
			HistoricalBettingOpportunity homeRow = home("h", "e1", D19, "0", "1.90", "0.04");
			HistoricalBettingOpportunity awayRow = away("a", "e1", D19, "0", "1.90", "-0.04");
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(homeRow, awayRow), List.of(result("e1", D19, 1, 0))), List.of());
			assertThat(summary(report.settlementBySide(), "HOME").n()).isEqualTo(1);
			assertThat(summary(report.settlementBySide(), "AWAY").n()).isEqualTo(1);
		}

		@Test
		void seasonsAreGroupedSeparately() {
			HistoricalBettingOpportunity a = home("a", "e19", D19, "0", "1.90", "0.01");
			HistoricalBettingOpportunity b = home("b", "e20", D20, "0", "1.90", "0.01");
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(a, b), List.of(result("e19", D19, 1, 0), result("e20", D20, 0, 1))),
					List.of());
			assertThat(report.seasonStability()).extracting(SeasonStabilityRow::seasonDisplay)
					.containsExactly("2019/20", "2020/21");
			assertThat(report.seasonStability().get(0).candidateCount()).isEqualTo(1);
		}
	}

	@Nested
	class SettlementAndInvariants {

		@Test
		void settlementProbabilityAggregationUsesCandidateEngineDistribution() {
			HistoricalBettingOpportunity win = priced("w", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			HistoricalBettingOpportunity loss = priced("l", "e2", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(win, loss), List.of(result("e1", D19, 1, 0), result("e2", D19, 0, 1))),
					List.of());
			OutcomeCalibration winCal = report.allCandidates().settlementCalibration().win();
			assertThat(winCal.averagePredictedProbability()).isEqualByComparingTo("0.60");
			assertThat(winCal.actualFrequency()).isEqualByComparingTo("0.50");
			assertThat(report.allCandidates().settlements().win()).isEqualTo(1);
			assertThat(report.allCandidates().settlements().loss()).isEqualTo(1);
		}

		@Test
		void weightedBucketAveragesMatchGlobals() {
			HistoricalBettingOpportunity negative = priced(
					"n",
					"e1",
					D19,
					SelectionType.HOME,
					"0",
					"2.00",
					SettlementProbabilityDistribution.binary(new BigDecimal("0.40")));
			HistoricalBettingOpportunity positive = priced("p", "e2", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(negative, positive), List.of(result("e1", D19, 0, 1), result("e2", D19, 1, 0))),
					List.of());
			assertThat(report.consistency().weightedRealizedMatchesGlobal()).isTrue();
			assertThat(report.consistency().weightedEdgeMatchesGlobal()).isTrue();
			assertThat(report.consistency().exhaustiveGroupCounts()).isTrue();
			int sum = report.edgeBuckets().stream().mapToInt(EdgeQualityGroupSummary::n).sum();
			assertThat(sum).isEqualTo(2);
		}

		@Test
		void expectedReturnAndUnitReturnMatchCanonicalCalculators() {
			HistoricalBettingOpportunity opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(opportunity), List.of(result("e1", D19, 1, 0))), List.of());
			assertThat(report.consistency().expectedReturnMatchesCandidateEngine()).isTrue();
			assertThat(report.consistency().unitReturnMatchesPayoutCalculator()).isTrue();
			assertThat(report.consistency().settlementProbabilitiesSumToOne()).isTrue();
			assertThat(report.allCandidates().unitStakeRoi()).isEqualByComparingTo("1");
		}

		@Test
		void emptyGroupsHaveNullRoiAndZeroCount() {
			HistoricalBettingOpportunity onlyLong = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(onlyLong), List.of(result("e1", D19, 1, 0))), List.of());
			EdgeQualityGroupSummary empty = summary(report.edgeBuckets(), "edge <= 0");
			assertThat(empty.n()).isZero();
			assertThat(empty.unitStakeRoi()).isNull();
			assertThat(empty.lowSample()).isFalse();
		}

		@Test
		void oneElementGroupConfidenceIntervalIsTheMean() {
			HistoricalBettingOpportunity opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(opportunity), List.of(result("e1", D19, 1, 0))), List.of());
			MeanConfidenceInterval interval = named(report, "all candidates");
			assertThat(interval.n()).isEqualTo(1);
			assertThat(interval.mean()).isEqualByComparingTo(interval.lower95());
			assertThat(interval.upper95()).isEqualByComparingTo(interval.mean());
		}
	}

	@Nested
	class MarketAndDisagreement {

		@Test
		void overroundUsesRawInversePricesOncePerEvent() {
			HistoricalBettingOpportunity homeSide = twoWay(
					"h", "e1", D19, SelectionType.HOME, "0", "1.90", "2.00", BINARY_60);
			HistoricalBettingOpportunity awaySide = twoWay(
					"a", "e1", D19, SelectionType.AWAY, "0", "1.90", "2.00", BINARY_60);
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(homeSide, awaySide), List.of(result("e1", D19, 1, 0))), List.of());
			assertThat(report.twoSidedCoherence().twoSidedEvents()).isEqualTo(1);
			BigDecimal expected = BigDecimal.ONE.divide(homeSide.opportunity().odds(), DiagnosticMath.MATH)
					.add(BigDecimal.ONE.divide(awaySide.opportunity().odds(), DiagnosticMath.MATH))
					.subtract(BigDecimal.ONE);
			assertThat(report.twoSidedCoherence().averageOverround().setScale(12, java.math.RoundingMode.HALF_UP))
					.isEqualByComparingTo(expected.setScale(12, java.math.RoundingMode.HALF_UP));
			assertThat(report.overroundBySeason()).hasSize(1);
			assertThat(report.overroundBySeason().getFirst().eventCount()).isEqualTo(1);
		}

		@Test
		void disagreementBucketsUseAbsoluteEdge() {
			HistoricalBettingOpportunity positive = priced("p", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			HistoricalBettingOpportunity negative = priced(
					"n",
					"e2",
					D19,
					SelectionType.HOME,
					"0",
					"2.00",
					SettlementProbabilityDistribution.binary(new BigDecimal("0.40")));
			EdgeQualityReport report = engine.analyze(
					dataset(List.of(positive, negative), List.of(result("e1", D19, 1, 0), result("e2", D19, 0, 1))),
					List.of());
			assertThat(positive.opportunity().edge().abs()).isEqualByComparingTo("0.20");
			assertThat(negative.opportunity().edge().abs()).isEqualByComparingTo("0.20");
			DisagreementGroup group = report.disagreementGroups().stream()
					.filter(row -> row.bucket() == DisagreementMagnitudeBucket.FROM_20_TO_30)
					.findFirst()
					.orElseThrow();
			assertThat(group.summary().n()).isEqualTo(2);
		}
	}

	@Nested
	class DeterminismAndMutation {

		@Test
		void analyzeIsRepeatableAndDoesNotMutateDatasetOrBacktestResult() {
			HistoricalBettingOpportunity first = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
			HistoricalBettingOpportunity second = priced("b", "e2", D20, SelectionType.HOME, "0", "2.00", BINARY_60);
			HistoricalWalkForwardDataset data = dataset(
					List.of(first, second), List.of(result("e1", D19, 0, 1), result("e2", D20, 1, 0)));
			StrategyConfig config = flatMinEdge("0.03");
			BacktestEngine backtestEngine = new BacktestEngine();
			BacktestResult before = backtestEngine.run(request(data, config));
			HistoricalWalkForwardDataset snapshot = new HistoricalWalkForwardDataset(
					data.stats(), data.opportunities(), data.eventResults());
			EdgeQualityReport firstReport = engine.analyze(
					data, List.of(new NamedBacktestResult("FLAT", config, before)));
			EdgeQualityReport secondReport = engine.analyze(
					data, List.of(new NamedBacktestResult("FLAT", config, before)));
			assertThat(secondReport).isEqualTo(firstReport);
			assertThat(firstReport.consistency().inputNotMutated()).isTrue();
			assertThat(data).isEqualTo(snapshot);
			BacktestResult after = backtestEngine.run(request(data, config));
			assertThat(after).isEqualTo(before);
		}

		@Test
		void bootstrapIntervalsAreDeterministic() {
			List<BigDecimal> values = List.of(
					new BigDecimal("-1"),
					new BigDecimal("0.5"),
					new BigDecimal("1"),
					new BigDecimal("-0.5"),
					new BigDecimal("0"));
			MeanConfidenceInterval first = DeterministicBootstrap.meanInterval(values);
			MeanConfidenceInterval second = DeterministicBootstrap.meanInterval(values);
			assertThat(second).isEqualTo(first);
			assertThat(first.seed()).isEqualTo(DeterministicBootstrap.SEED);
			assertThat(first.bootstrapReplicates()).isEqualTo(DeterministicBootstrap.REPLICATES);
		}

		@Test
		void spearmanIsPlusOneForPerfectRankAgreementAndMinusOneForReversal() {
			List<BigDecimal> x = List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
			List<BigDecimal> up = List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30"));
			List<BigDecimal> down = List.of(new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("10"));
			assertThat(DiagnosticCorrelations.spearman(x, up)).isEqualByComparingTo("1");
			assertThat(DiagnosticCorrelations.spearman(x, down)).isEqualByComparingTo("-1");
			assertThat(DiagnosticCorrelations.pearson(x, up)).isEqualByComparingTo("1");
		}
	}

	private static EdgeQualityGroupSummary summary(List<EdgeQualityGroupSummary> rows, String key) {
		return rows.stream().filter(row -> row.key().equals(key)).findFirst().orElseThrow();
	}

	private static MeanConfidenceInterval named(EdgeQualityReport report, String label) {
		return report.confidenceIntervals().stream()
				.filter(row -> row.label().equals(label))
				.findFirst()
				.orElseThrow()
				.interval();
	}

	private static BacktestRequest request(HistoricalWalkForwardDataset data, StrategyConfig config) {
		return new BacktestRequest(
				new OwnerId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")),
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
