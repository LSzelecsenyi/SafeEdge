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

import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BundesligaReplicationReportFormatterTest {

	@Test
	void labelsBundesligaAndHistoricalSource() {
		var opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
		var data = dataset(CanonicalCompetition.BUNDESLIGA, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var predictions = List.of(prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape()));
		BaselineDiagnosticsReport baseline = new BaselineDiagnosticsEngine().analyze(data, predictions, List.of());
		EdgeQualityReport edge = new EdgeQualityDiagnosticsEngine().analyze(data, List.of());
		BigDecimal rate = BigDecimal.ONE;
		LeagueCoverageSnapshot coverage = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.BUNDESLIGA,
				HistoricalQuoteSource.MARKET_AVERAGE,
				new HistoricalAhCoverageReport(
						Instant.parse("2026-08-16T00:00:00Z"),
						HistoricalSource.FOOTBALL_DATA_UK,
						List.of(new HistoricalAhLeagueSeasonCoverage(
								CanonicalCompetition.BUNDESLIGA,
								new FootballSeason(2019, 2020),
								1,
								1,
								rate,
								HistoricalQuoteSource.MARKET_AVERAGE,
								rate,
								List.of(new HistoricalAhQuoteSourceCoverage(
										HistoricalQuoteSource.MARKET_AVERAGE, 1, 1, rate))))));
		String markdown = BundesligaReplicationReportFormatter.format(coverage, baseline, edge);
		assertThat(markdown).contains("# Baseline 003 – Bundesliga Replication");
		assertThat(markdown).contains("Competition: BUNDESLIGA");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("decayHalfLifeDays = 180");
		assertThat(markdown).contains("Zero-tuning");
		assertThat(markdown).contains("Missing evaluation seasons:");
		assertThat(markdown).doesNotContain("adopt this threshold");
	}
}
