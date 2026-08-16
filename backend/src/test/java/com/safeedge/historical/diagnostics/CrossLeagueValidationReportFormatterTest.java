package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import org.junit.jupiter.api.Test;

class CrossLeagueValidationReportFormatterTest {

	@Test
	void reportLabelsSourcesAndDoesNotPromoteALeague() {
		CrossLeagueComparison comparison = CrossLeagueComparisonEngine.compare(
				PremierLeaguePublishedBaseline.snapshot(),
				bundesligaCopy(PremierLeaguePublishedBaseline.snapshot()));
		String markdown = CrossLeagueValidationReportFormatter.format(comparison);
		assertThat(markdown).contains("# Baseline 003 – Cross-League Structural Validation");
		assertThat(markdown).contains("PREMIER_LEAGUE");
		assertThat(markdown).contains("BUNDESLIGA");
		assertThat(markdown).contains("HISTORICAL QUOTE SOURCE = MARKET_AVERAGE");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("football-data.co.uk");
		assertThat(markdown).contains("decayHalfLifeDays = 180");
		assertThat(markdown).contains("FAILURE STRONGLY REPLICATES");
		assertThat(markdown).contains("NEXT HYPOTHESIS");
		assertThat(markdown).contains("Independent-Poisson score tails");
		assertThat(markdown).doesNotContain("therefore bet Bundesliga");
		assertThat(markdown).contains("Bundesliga is not selected as a betting venue");
		assertThat(comparison.premierLeague().quoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_AVERAGE);
		assertThat(comparison.bundesliga().competition()).isEqualTo(CanonicalCompetition.BUNDESLIGA);
	}

	@Test
	void doesNotReplicateOmitsNextHypothesis() {
		LeagueDiagnosticSnapshot pl = PremierLeaguePublishedBaseline.snapshot();
		LeagueDiagnosticSnapshot bl = new LeagueDiagnosticSnapshot(
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
				new java.math.BigDecimal("0.55"),
				pl.pearson(),
				pl.edgeDeciles(),
				java.util.List.of(
						new BucketTrendRow("edge <= 0", 100, new java.math.BigDecimal("-0.10"), new java.math.BigDecimal("-0.08")),
						new BucketTrendRow("edge >= 0.30", 100, new java.math.BigDecimal("0.40"), new java.math.BigDecimal("0.05"))),
				java.util.List.of(
						new HighEdgeCalibrationSlice(
								new java.math.BigDecimal("0.10"),
								10,
								new java.math.BigDecimal("0.20"),
								new java.math.BigDecimal("0.01"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40")),
						new HighEdgeCalibrationSlice(
								new java.math.BigDecimal("0.20"),
								10,
								new java.math.BigDecimal("0.30"),
								new java.math.BigDecimal("0.02"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40"),
								new java.math.BigDecimal("0.40"))),
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
		String markdown = CrossLeagueValidationReportFormatter.format(
				CrossLeagueComparisonEngine.compare(pl, bl));
		assertThat(markdown).contains("FAILURE DOES NOT REPLICATE");
		assertThat(markdown).doesNotContain("### NEXT HYPOTHESIS");
	}

	private static LeagueDiagnosticSnapshot bundesligaCopy(LeagueDiagnosticSnapshot pl) {
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
				pl.spearman(),
				pl.pearson(),
				pl.edgeDeciles(),
				pl.edgeBuckets(),
				pl.highEdgeSlices(),
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
