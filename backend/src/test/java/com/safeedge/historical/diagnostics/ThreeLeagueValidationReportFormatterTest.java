package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import org.junit.jupiter.api.Test;

class ThreeLeagueValidationReportFormatterTest {

	@Test
	void reportLabelsSourcesAndDoesNotPromoteALeague() {
		ThreeLeagueComparison comparison = CrossLeagueComparisonEngine.compareThree(
				PremierLeaguePublishedBaseline.snapshot(),
				BundesligaPublishedBaseline.snapshot(),
				serieACopy(PremierLeaguePublishedBaseline.snapshot()));
		String markdown = ThreeLeagueValidationReportFormatter.format(comparison);
		assertThat(markdown).contains("# Baseline 004 – Three-League Structural Validation");
		assertThat(markdown).contains("PREMIER_LEAGUE");
		assertThat(markdown).contains("BUNDESLIGA");
		assertThat(markdown).contains("SERIE_A");
		assertThat(markdown).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("football-data.co.uk");
		assertThat(markdown).contains("decayHalfLifeDays = 180");
		assertThat(markdown).contains("FAILURE STRONGLY REPLICATES AGAIN");
		assertThat(markdown).contains("Further same-model league replications are low-value");
		assertThat(markdown).contains("NEXT HYPOTHESIS");
		assertThat(markdown).contains("Dixon-Coles");
		assertThat(markdown).doesNotContain("therefore bet Serie A");
		assertThat(markdown).contains("Serie A is not selected as a betting venue");
		assertThat(comparison.premierLeague().quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(comparison.serieA().competition()).isEqualTo(CanonicalCompetition.SERIE_A);
		assertThat(comparison.premierLeague().spearman()).isEqualByComparingTo("0.0172");
		assertThat(comparison.bundesliga().spearman()).isEqualByComparingTo("-0.010761");
	}

	@Test
	void doesNotReplicateOmitsNextHypothesis() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = BundesligaPublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot sa = wellRankedSerieA(pl);
		String markdown = ThreeLeagueValidationReportFormatter.format(
				CrossLeagueComparisonEngine.compareThree(pl, bl, sa));
		assertThat(markdown).contains("FAILURE DOES NOT REPLICATE");
		assertThat(markdown).doesNotContain("### NEXT HYPOTHESIS");
		assertThat(markdown).doesNotContain("Further same-model league replications are low-value");
	}

	private static LeagueDiagnosticSnapshot serieACopy(LeagueDiagnosticSnapshot src) {
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.SERIE_A,
				src.quoteSource(),
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
				src.spearman(),
				src.pearson(),
				src.edgeDeciles(),
				src.edgeBuckets(),
				src.highEdgeSlices(),
				src.ahFamilies(),
				src.sides(),
				src.seasons(),
				java.util.List.of(),
				src.meanOverround(),
				src.medianOverround(),
				src.overroundBySeason(),
				src.strategies(),
				src.confidenceIntervals(),
				src.topPredictedEdges());
	}

	private static LeagueDiagnosticSnapshot wellRankedSerieA(LeagueDiagnosticSnapshot pl) {
		java.util.List<HighEdgeCalibrationSlice> calibrated = new java.util.ArrayList<>();
		for (HighEdgeCalibrationSlice slice : pl.highEdgeSlices()) {
			calibrated.add(new HighEdgeCalibrationSlice(
					slice.threshold(),
					slice.n(),
					slice.averageEdge(),
					slice.unitStakeRoi(),
					new java.math.BigDecimal("0.40"),
					new java.math.BigDecimal("0.40"),
					new java.math.BigDecimal("0.40"),
					new java.math.BigDecimal("0.40")));
		}
		return new LeagueDiagnosticSnapshot(
				CanonicalCompetition.SERIE_A,
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
				new java.math.BigDecimal("0.55"),
				new java.math.BigDecimal("0.40"),
				pl.edgeDeciles(),
				java.util.List.of(
						new BucketTrendRow("edge <= 0", 100, new java.math.BigDecimal("-0.10"), new java.math.BigDecimal("-0.08")),
						new BucketTrendRow("edge >= 0.30", 100, new java.math.BigDecimal("0.40"), new java.math.BigDecimal("0.05"))),
				calibrated,
				pl.ahFamilies(),
				pl.sides(),
				pl.seasons(),
				java.util.List.of(),
				pl.meanOverround(),
				pl.medianOverround(),
				pl.overroundBySeason(),
				pl.strategies(),
				pl.confidenceIntervals(),
				pl.topPredictedEdges());
	}
}
