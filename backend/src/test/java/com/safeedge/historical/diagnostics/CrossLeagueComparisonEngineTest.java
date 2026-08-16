package com.safeedge.historical.diagnostics;

import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.BINARY_60;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.D19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.S19;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.dataset;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.oneNilShape;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.prediction;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.priced;
import static com.safeedge.historical.diagnostics.BaselineDiagnosticsFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrossLeagueComparisonEngineTest {

	@Test
	void rejectsSwappedLeagueIdentity() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compare(pl, pl))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("BUNDESLIGA");
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compare(bundesligaLike(pl, "0.02"), pl))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("PREMIER_LEAGUE");
	}

	@Test
	void snapshotsDoNotCarryCandidateRowsToMix() {
		assertThat(PremierLeaguePublishedBaseline.snapshot().topPredictedEdges()).isEmpty();
		assertThat(PremierLeaguePublishedBaseline.snapshot().competition())
				.isEqualTo(CanonicalCompetition.PREMIER_LEAGUE);
	}

	@Test
	void fromReportsRejectsMixedCompetitions() {
		var opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
		var plDataset = dataset(CanonicalCompetition.PREMIER_LEAGUE, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var blDataset = dataset(CanonicalCompetition.BUNDESLIGA, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var predictions = List.of(prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape()));
		BaselineDiagnosticsReport baseline = new BaselineDiagnosticsEngine().analyze(plDataset, predictions, List.of());
		EdgeQualityReport edge = new EdgeQualityDiagnosticsEngine().analyze(blDataset, List.of());
		assertThatThrownBy(() -> LeagueDiagnosticSnapshot.fromReports(baseline, edge, List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("different competitions");
	}

	@Test
	void fromReportsKeepsEdgeQualityDefinitions() {
		var opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
		var data = dataset(CanonicalCompetition.BUNDESLIGA, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var predictions = List.of(prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape()));
		BaselineDiagnosticsReport baseline = new BaselineDiagnosticsEngine().analyze(data, predictions, List.of());
		EdgeQualityReport edge = new EdgeQualityDiagnosticsEngine().analyze(data, List.of());
		LeagueDiagnosticSnapshot snapshot = LeagueDiagnosticSnapshot.fromReports(baseline, edge, List.of());
		assertThat(snapshot.competition()).isEqualTo(CanonicalCompetition.BUNDESLIGA);
		assertThat(snapshot.quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(snapshot.candidateCount()).isEqualTo(edge.analyzedCandidateCount());
		assertThat(snapshot.spearman()).isEqualTo(edge.rankQuality().spearman());
		assertThat(snapshot.averagePredictedEdge()).isEqualByComparingTo(edge.allCandidates().averageEdge());
		assertThat(snapshot.averageRealizedReturn()).isEqualByComparingTo(edge.allCandidates().unitStakeRoi());
		assertThat(snapshot.predictionQuality().predictedHomeWin())
				.isEqualByComparingTo(baseline.goalCalibration().averagePredictedHomeWinProbability());
	}

	@Test
	void publishedPremierLeaguePatternIsTheKnownFailure() {
		StructuralPatternFlags flags = CrossLeagueComparisonEngine.flags(PremierLeaguePublishedBaseline.snapshot());
		assertThat(flags.aggregateGoalsAndMatchResultCalibrated()).isTrue();
		assertThat(flags.aggregateEdgeNearRealizedReturn()).isTrue();
		assertThat(flags.edgeRankingWeak()).isTrue();
		assertThat(flags.highEdgeWinOverconfidentAndLossUnderconfident()).isTrue();
		assertThat(flags.higherEdgeDoesNotMonotonicallyImproveRoi()).isTrue();
		assertThat(flags.failureStableAcrossSeasons()).isTrue();
	}

	@Test
	void stronglyReplicatesWhenBundesligaShowsTheSameCorePattern() {
		LeagueDiagnosticSnapshot bl = bundesligaLike(PremierLeaguePublishedBaseline.snapshot(), "0.02");
		CrossLeagueComparison comparison =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bl);
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES);
		assertThat(comparison.bundesliga().competition()).isEqualTo(CanonicalCompetition.BUNDESLIGA);
		assertThat(comparison.premierLeague().candidateCount()).isEqualTo(2940);
		assertThat(comparison.bundesliga().candidateCount()).isEqualTo(2940);
	}

	@Test
	void doesNotReplicateWhenBundesligaRankingAndHighEdgeAreFine() {
		LeagueDiagnosticSnapshot bl = wellRankedBundesliga();
		CrossLeagueComparison comparison =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bl);
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_DOES_NOT_REPLICATE);
		assertThat(comparison.bundesligaFlags().edgeRankingWeak()).isFalse();
		assertThat(comparison.bundesligaFlags().highEdgeWinOverconfidentAndLossUnderconfident()).isFalse();
	}

	@Test
	void partiallyReplicatesWhenOnlyRankingIsWeak() {
		LeagueDiagnosticSnapshot bl = weakRankCalibratedHighEdgeBundesliga();
		CrossLeagueComparison comparison =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bl);
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_PARTIALLY_REPLICATES);
	}

	@Test
	void comparisonIsDeterministic() {
		LeagueDiagnosticSnapshot bl = bundesligaLike(PremierLeaguePublishedBaseline.snapshot(), "0.03");
		CrossLeagueComparison first =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bl);
		CrossLeagueComparison second =
				CrossLeagueComparisonEngine.compare(PremierLeaguePublishedBaseline.snapshot(), bl);
		assertThat(first.classification()).isEqualTo(second.classification());
		assertThat(first.bundesligaFlags()).isEqualTo(second.bundesligaFlags());
	}

	@Test
	void compareThreeRejectsWrongLeagueIdentity() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = bundesligaLike(pl, "0.02");
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compareThree(pl, pl, serieALike(pl, "0.02")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("BUNDESLIGA");
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compareThree(pl, bl, bl))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("SERIE_A");
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compareThree(bl, bl, serieALike(pl, "0.02")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("PREMIER_LEAGUE");
	}

	@Test
	void compareThreeRejectsMismatchedQuoteSource() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = bundesligaLike(pl, "0.02");
		LeagueDiagnosticSnapshot sa = copy(
				pl,
				CanonicalCompetition.SERIE_A,
				HistoricalQuoteSource.MARKET_MAX,
				pl.spearman(),
				pl.pearson(),
				pl.edgeBuckets(),
				pl.highEdgeSlices(),
				pl.seasons());
		assertThatThrownBy(() -> CrossLeagueComparisonEngine.compareThree(pl, bl, sa))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("quote sources");
	}

	@Test
	void fromReportsKeepsSerieAIdentityAndMetricDefinitions() {
		var opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
		var data = dataset(CanonicalCompetition.SERIE_A, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var predictions = List.of(prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape()));
		BaselineDiagnosticsReport baseline = new BaselineDiagnosticsEngine().analyze(data, predictions, List.of());
		EdgeQualityReport edge = new EdgeQualityDiagnosticsEngine().analyze(data, List.of());
		LeagueDiagnosticSnapshot snapshot = LeagueDiagnosticSnapshot.fromReports(baseline, edge, List.of());
		assertThat(snapshot.competition()).isEqualTo(CanonicalCompetition.SERIE_A);
		assertThat(snapshot.quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(snapshot.candidateCount()).isEqualTo(edge.analyzedCandidateCount());
		assertThat(snapshot.spearman()).isEqualTo(edge.rankQuality().spearman());
		assertThat(snapshot.averagePredictedEdge()).isEqualByComparingTo(edge.allCandidates().averageEdge());
		assertThat(snapshot.averageRealizedReturn()).isEqualByComparingTo(edge.allCandidates().unitStakeRoi());
		assertThat(edge.consistency().inputNotMutated()).isTrue();
	}

	@Test
	void threeLeagueStronglyReplicatesAgainWhenSerieAShowsTheSameCorePattern() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = bundesligaLike(pl, "0.02");
		LeagueDiagnosticSnapshot sa = serieALike(pl, "0.01");
		ThreeLeagueComparison comparison = CrossLeagueComparisonEngine.compareThree(pl, bl, sa);
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_STRONGLY_REPLICATES_AGAIN);
		assertThat(comparison.serieA().competition()).isEqualTo(CanonicalCompetition.SERIE_A);
		assertThat(comparison.premierLeague().candidateCount()).isEqualTo(2940);
		assertThat(comparison.bundesliga().candidateCount()).isEqualTo(2940);
		assertThat(comparison.serieA().candidateCount()).isEqualTo(2940);
		assertThat(comparison.premierLeague().topPredictedEdges()).isEmpty();
		assertThat(comparison.bundesliga().topPredictedEdges()).isEmpty();
		assertThat(comparison.serieA().topPredictedEdges()).isEmpty();
	}

	@Test
	void threeLeagueDoesNotReplicateWhenSerieARankingAndHighEdgeAreFine() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		ThreeLeagueComparison comparison = CrossLeagueComparisonEngine.compareThree(
				pl, bundesligaLike(pl, "0.02"), wellRankedSerieA());
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_DOES_NOT_REPLICATE);
		assertThat(comparison.serieAFlags().edgeRankingWeak()).isFalse();
		assertThat(comparison.serieAFlags().highEdgeWinOverconfidentAndLossUnderconfident()).isFalse();
	}

	@Test
	void threeLeaguePartiallyReplicatesWhenOnlySerieARankingIsWeak() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot ranked = wellRankedSerieA();
		LeagueDiagnosticSnapshot sa = copy(
				ranked,
				CanonicalCompetition.SERIE_A,
				ranked.quoteSource(),
				new BigDecimal("0.02"),
				ranked.pearson(),
				ranked.edgeBuckets(),
				ranked.highEdgeSlices(),
				ranked.seasons());
		ThreeLeagueComparison comparison =
				CrossLeagueComparisonEngine.compareThree(pl, bundesligaLike(pl, "0.02"), sa);
		assertThat(comparison.classification())
				.isEqualTo(StructuralReplicationClassification.FAILURE_PARTIALLY_REPLICATES);
	}

	@Test
	void threeLeagueComparisonIsDeterministic() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = bundesligaLike(pl, "0.03");
		LeagueDiagnosticSnapshot sa = serieALike(pl, "0.01");
		ThreeLeagueComparison first = CrossLeagueComparisonEngine.compareThree(pl, bl, sa);
		ThreeLeagueComparison second = CrossLeagueComparisonEngine.compareThree(pl, bl, sa);
		assertThat(first.classification()).isEqualTo(second.classification());
		assertThat(first.serieAFlags()).isEqualTo(second.serieAFlags());
		assertThat(first.premierLeague().spearman()).isEqualByComparingTo(pl.spearman());
		assertThat(first.bundesliga().spearman()).isEqualByComparingTo(bl.spearman());
	}

	private static LeagueDiagnosticSnapshot bundesligaLike(LeagueDiagnosticSnapshot pl, String spearman) {
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.BUNDESLIGA,
				pl.quoteSource(),
				pl.matchesLoaded(),
				pl.matchesEvaluated(),
				pl.matchesSkippedInsufficientHistory(),
				pl.matchesSkippedMissingQuote(),
				pl.predictionQuality(),
				pl.candidateCount(),
				pl.positiveEvCount(),
				pl.zeroEvCount(),
				pl.negativeEvCount(),
				pl.averagePredictedEdge(),
				pl.averageRealizedReturn(),
				pl.calibrationGap(),
				new BigDecimal(spearman),
				pl.pearson(),
				pl.edgeDeciles(),
				pl.edgeBuckets(),
				pl.highEdgeSlices(),
				pl.ahFamilies(),
				pl.sides(),
				pl.seasons(),
				List.of(),
				pl.meanOverround(),
				pl.medianOverround(),
				pl.overroundBySeason(),
				pl.strategies(),
				pl.confidenceIntervals(),
				pl.topPredictedEdges());
	}

	private static LeagueDiagnosticSnapshot wellRankedBundesliga() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		List<HighEdgeCalibrationSlice> calibrated = new ArrayList<>();
		for (HighEdgeCalibrationSlice slice : pl.highEdgeSlices()) {
			calibrated.add(new HighEdgeCalibrationSlice(
					slice.threshold(),
					slice.n(),
					slice.averageEdge(),
					slice.unitStakeRoi(),
					new BigDecimal("0.40"),
					new BigDecimal("0.40"),
					new BigDecimal("0.40"),
					new BigDecimal("0.40")));
		}
		List<BucketTrendRow> monotone = List.of(
				new BucketTrendRow("edge <= 0", 100, new BigDecimal("-0.10"), new BigDecimal("-0.08")),
				new BucketTrendRow("0.10 <= edge < 0.20", 100, new BigDecimal("0.15"), new BigDecimal("0.02")),
				new BucketTrendRow("edge >= 0.30", 100, new BigDecimal("0.40"), new BigDecimal("0.05")));
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.BUNDESLIGA,
				pl.quoteSource(),
				pl.matchesLoaded(),
				pl.matchesEvaluated(),
				pl.matchesSkippedInsufficientHistory(),
				pl.matchesSkippedMissingQuote(),
				pl.predictionQuality(),
				pl.candidateCount(),
				pl.positiveEvCount(),
				pl.zeroEvCount(),
				pl.negativeEvCount(),
				pl.averagePredictedEdge(),
				pl.averageRealizedReturn(),
				pl.calibrationGap(),
				new BigDecimal("0.55"),
				new BigDecimal("0.40"),
				pl.edgeDeciles(),
				monotone,
				calibrated,
				pl.ahFamilies(),
				pl.sides(),
				pl.seasons(),
				List.of(),
				pl.meanOverround(),
				pl.medianOverround(),
				pl.overroundBySeason(),
				pl.strategies(),
				pl.confidenceIntervals(),
				pl.topPredictedEdges());
	}

	private static LeagueDiagnosticSnapshot weakRankCalibratedHighEdgeBundesliga() {
		LeagueDiagnosticSnapshot ranked = wellRankedBundesliga();
		return new LeagueDiagnosticSnapshot(
				ranked.competition(),
				ranked.quoteSource(),
				ranked.matchesLoaded(),
				ranked.matchesEvaluated(),
				ranked.matchesSkippedInsufficientHistory(),
				ranked.matchesSkippedMissingQuote(),
				ranked.predictionQuality(),
				ranked.candidateCount(),
				ranked.positiveEvCount(),
				ranked.zeroEvCount(),
				ranked.negativeEvCount(),
				ranked.averagePredictedEdge(),
				ranked.averageRealizedReturn(),
				ranked.calibrationGap(),
				new BigDecimal("0.02"),
				ranked.pearson(),
				ranked.edgeDeciles(),
				ranked.edgeBuckets(),
				ranked.highEdgeSlices(),
				ranked.ahFamilies(),
				ranked.sides(),
				ranked.seasons(),
				ranked.missingEvaluationStartYears(),
				ranked.meanOverround(),
				ranked.medianOverround(),
				ranked.overroundBySeason(),
				ranked.strategies(),
				ranked.confidenceIntervals(),
				ranked.topPredictedEdges());
	}

	private static LeagueDiagnosticSnapshot serieALike(LeagueDiagnosticSnapshot pl, String spearman) {
		return copy(
				pl,
				CanonicalCompetition.SERIE_A,
				pl.quoteSource(),
				new BigDecimal(spearman),
				pl.pearson(),
				pl.edgeBuckets(),
				pl.highEdgeSlices(),
				pl.seasons());
	}

	private static LeagueDiagnosticSnapshot wellRankedSerieA() {
		LeagueDiagnosticSnapshot ranked = wellRankedBundesliga();
		return copy(
				ranked,
				CanonicalCompetition.SERIE_A,
				ranked.quoteSource(),
				ranked.spearman(),
				ranked.pearson(),
				ranked.edgeBuckets(),
				ranked.highEdgeSlices(),
				ranked.seasons());
	}

	private static LeagueDiagnosticSnapshot copy(
			LeagueDiagnosticSnapshot src,
			CanonicalCompetition competition,
			HistoricalQuoteSource quoteSource,
			BigDecimal spearman,
			BigDecimal pearson,
			List<BucketTrendRow> edgeBuckets,
			List<HighEdgeCalibrationSlice> highEdgeSlices,
			List<SeasonStabilityRow> seasons) {
		return new LeagueDiagnosticSnapshot(
				competition,
				quoteSource,
				src.matchesLoaded(),
				src.matchesEvaluated(),
				src.matchesSkippedInsufficientHistory(),
				src.matchesSkippedMissingQuote(),
				src.predictionQuality(),
				src.candidateCount(),
				src.positiveEvCount(),
				src.zeroEvCount(),
				src.negativeEvCount(),
				src.averagePredictedEdge(),
				src.averageRealizedReturn(),
				src.calibrationGap(),
				spearman,
				pearson,
				src.edgeDeciles(),
				edgeBuckets,
				highEdgeSlices,
				src.ahFamilies(),
				src.sides(),
				seasons,
				src.missingEvaluationStartYears(),
				src.meanOverround(),
				src.medianOverround(),
				src.overroundBySeason(),
				src.strategies(),
				src.confidenceIntervals(),
				src.topPredictedEdges());
	}
}
