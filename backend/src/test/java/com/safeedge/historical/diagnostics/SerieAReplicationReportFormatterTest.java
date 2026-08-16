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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SerieAReplicationReportFormatterTest {

	@Test
	void labelsSerieAAndHistoricalSource() {
		var opportunity = priced("a", "e1", D19, SelectionType.HOME, "0", "2.00", BINARY_60);
		var data = dataset(CanonicalCompetition.SERIE_A, List.of(opportunity), List.of(result("e1", D19, 1, 0)));
		var predictions = List.of(prediction("e1", S19, D19, new MatchScore(1, 0), oneNilShape()));
		BaselineDiagnosticsReport baseline = new BaselineDiagnosticsEngine().analyze(data, predictions, List.of());
		EdgeQualityReport edge = new EdgeQualityDiagnosticsEngine().analyze(data, List.of());
		List<HistoricalAhLeagueSeasonCoverage> rows = new ArrayList<>();
		for (int year = 2014; year <= 2023; year++) {
			int quoted = year >= 2019 ? (year == 2020 || year == 2021 ? 379 : 380) : 0;
			rows.add(row(year, 380, quoted));
		}
		LeagueCoverageSnapshot coverage = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.SERIE_A,
				HistoricalQuoteSource.MARKET_AVERAGE,
				new HistoricalAhCoverageReport(
						Instant.parse("2026-08-16T00:00:00Z"), HistoricalSource.FOOTBALL_DATA_UK, rows));
		String markdown = SerieAReplicationReportFormatter.format(coverage, baseline, edge);
		assertThat(markdown).contains("# Baseline 004 – Serie A Replication");
		assertThat(markdown).contains("Competition: SERIE_A");
		assertThat(markdown).contains("not Tippmix");
		assertThat(markdown).contains("decayHalfLifeDays = 180");
		assertThat(markdown).contains("Zero-tuning");
		assertThat(markdown).contains("No Serie-A-specific");
		assertThat(markdown).contains("Missing evaluation seasons:");
		assertThat(markdown).contains("Evaluation matches missing MARKET_AVERAGE: 2");
		assertThat(markdown).contains("| 2020/21 | evaluation | 380 | 379 |");
		assertThat(markdown).contains("| 2021/22 | evaluation | 380 | 379 |");
		assertThat(markdown).doesNotContain("adopt this threshold");
		assertThat(markdown).doesNotContain("therefore bet Serie A");
	}

	private static HistoricalAhLeagueSeasonCoverage row(int startYear, int matches, int quoted) {
		BigDecimal rate = matches == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(quoted).divide(BigDecimal.valueOf(matches), DiagnosticMath.MATH);
		return new HistoricalAhLeagueSeasonCoverage(
				CanonicalCompetition.SERIE_A,
				new FootballSeason(startYear, startYear + 1),
				matches,
				quoted,
				rate,
				quoted == 0 ? null : HistoricalQuoteSource.MARKET_AVERAGE,
				rate,
				List.of(new HistoricalAhQuoteSourceCoverage(
						HistoricalQuoteSource.MARKET_AVERAGE, matches, quoted, rate)));
	}
}
