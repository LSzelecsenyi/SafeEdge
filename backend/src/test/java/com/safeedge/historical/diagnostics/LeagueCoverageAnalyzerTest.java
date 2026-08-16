package com.safeedge.historical.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeagueCoverageAnalyzerTest {

	@Test
	void reportsMissingEvaluationSeasonWithoutAlteringYears() {
		HistoricalAhCoverageReport report = coverage(
				row(CanonicalCompetition.BUNDESLIGA, 2014, 306, 0),
				row(CanonicalCompetition.BUNDESLIGA, 2019, 306, 306),
				row(CanonicalCompetition.PREMIER_LEAGUE, 2021, 380, 380));
		LeagueCoverageSnapshot snapshot = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.BUNDESLIGA, HistoricalQuoteSource.MARKET_AVERAGE, report);
		assertThat(snapshot.competition()).isEqualTo(CanonicalCompetition.BUNDESLIGA);
		assertThat(snapshot.missingWarmupStartYears()).containsExactly(2015, 2016, 2017, 2018);
		assertThat(snapshot.missingEvaluationStartYears()).containsExactly(2020, 2021, 2022, 2023);
		assertThat(snapshot.expectedEvaluationStartYears()).containsExactly(2019, 2020, 2021, 2022, 2023);
		assertThat(snapshot.seasons()).extracting(LeagueSeasonCoverageRow::seasonStartYear).containsExactly(2014, 2019);
		assertThat(snapshot.seasons().getFirst().matchesWithSelectedQuote()).isZero();
		assertThat(snapshot.seasons().get(1).selectedQuoteCoverageRate()).isEqualByComparingTo("1");
	}

	@Test
	void completeBundesligaWindowIncludes2021() {
		List<HistoricalAhLeagueSeasonCoverage> rows = new ArrayList<>();
		for (int year = 2014; year <= 2023; year++) {
			int quoted = year >= 2019 ? 306 : 0;
			rows.add(row(CanonicalCompetition.BUNDESLIGA, year, 306, quoted));
		}
		LeagueCoverageSnapshot snapshot = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.BUNDESLIGA,
				HistoricalQuoteSource.MARKET_AVERAGE,
				new HistoricalAhCoverageReport(Instant.parse("2026-08-16T00:00:00Z"), HistoricalSource.FOOTBALL_DATA_UK, rows));
		assertThat(snapshot.missingWarmupStartYears()).isEmpty();
		assertThat(snapshot.missingEvaluationStartYears()).isEmpty();
		assertThat(snapshot.evaluationMatchCount()).isEqualTo(1530);
		assertThat(snapshot.evaluationMatchesWithSelectedQuote()).isEqualTo(1530);
		assertThat(snapshot.warmupMatchCount()).isEqualTo(1530);
		assertThat(snapshot.seasons())
				.filteredOn(LeagueSeasonCoverageRow::evaluationWindow)
				.extracting(LeagueSeasonCoverageRow::seasonStartYear)
				.contains(2021);
	}

	@Test
	void emptyDatabaseReportsEveryExpectedSeasonMissing() {
		LeagueCoverageSnapshot snapshot = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.BUNDESLIGA,
				HistoricalQuoteSource.MARKET_AVERAGE,
				new HistoricalAhCoverageReport(
						Instant.parse("2026-08-16T00:00:00Z"), HistoricalSource.FOOTBALL_DATA_UK, List.of()));
		assertThat(snapshot.seasons()).isEmpty();
		assertThat(snapshot.missingWarmupStartYears()).containsExactly(2014, 2015, 2016, 2017, 2018);
		assertThat(snapshot.missingEvaluationStartYears()).containsExactly(2019, 2020, 2021, 2022, 2023);
	}

	@Test
	void reportsSerieAMissingQuotesWithoutTreatingSeasonAsMissing() {
		List<HistoricalAhLeagueSeasonCoverage> rows = new ArrayList<>();
		for (int year = 2014; year <= 2023; year++) {
			int quoted = year >= 2019 ? (year == 2020 || year == 2021 ? 379 : 380) : 0;
			rows.add(row(CanonicalCompetition.SERIE_A, year, 380, quoted));
		}
		LeagueCoverageSnapshot snapshot = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.SERIE_A,
				HistoricalQuoteSource.MARKET_AVERAGE,
				new HistoricalAhCoverageReport(
						Instant.parse("2026-08-16T00:00:00Z"), HistoricalSource.FOOTBALL_DATA_UK, rows));
		assertThat(snapshot.competition()).isEqualTo(CanonicalCompetition.SERIE_A);
		assertThat(snapshot.missingWarmupStartYears()).isEmpty();
		assertThat(snapshot.missingEvaluationStartYears()).isEmpty();
		assertThat(snapshot.warmupMatchCount()).isEqualTo(1900);
		assertThat(snapshot.evaluationMatchCount()).isEqualTo(1900);
		assertThat(snapshot.evaluationMatchesWithSelectedQuote()).isEqualTo(1898);
		assertThat(snapshot.evaluationMatchesMissingSelectedQuote()).isEqualTo(2);
		assertThat(snapshot.seasons())
				.filteredOn(row -> row.seasonStartYear() == 2020 || row.seasonStartYear() == 2021)
				.extracting(LeagueSeasonCoverageRow::matchesWithSelectedQuote)
				.containsExactly(379, 379);
	}

	@Test
	void doesNotMixPremierLeagueRowsIntoBundesligaCoverage() {
		HistoricalAhCoverageReport report = coverage(
				row(CanonicalCompetition.PREMIER_LEAGUE, 2019, 380, 380),
				row(CanonicalCompetition.BUNDESLIGA, 2019, 306, 300));
		LeagueCoverageSnapshot snapshot = LeagueCoverageAnalyzer.analyze(
				CanonicalCompetition.BUNDESLIGA, HistoricalQuoteSource.MARKET_AVERAGE, report);
		assertThat(snapshot.seasons()).hasSize(1);
		assertThat(snapshot.seasons().getFirst().totalMatches()).isEqualTo(306);
		assertThat(snapshot.seasons().getFirst().matchesWithSelectedQuote()).isEqualTo(300);
	}

	private static HistoricalAhCoverageReport coverage(HistoricalAhLeagueSeasonCoverage... rows) {
		return new HistoricalAhCoverageReport(
				Instant.parse("2026-08-16T00:00:00Z"), HistoricalSource.FOOTBALL_DATA_UK, List.of(rows));
	}

	private static HistoricalAhLeagueSeasonCoverage row(
			CanonicalCompetition competition, int startYear, int matches, int quoted) {
		BigDecimal rate = matches == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(quoted).divide(BigDecimal.valueOf(matches), DiagnosticMath.MATH);
		return new HistoricalAhLeagueSeasonCoverage(
				competition,
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
