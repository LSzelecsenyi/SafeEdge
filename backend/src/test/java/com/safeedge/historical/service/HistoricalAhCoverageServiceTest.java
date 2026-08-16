package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalAhCoverageReport;
import com.safeedge.historical.domain.HistoricalAhLeagueSeasonCoverage;
import com.safeedge.historical.domain.HistoricalAhQuoteSourceCoverage;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.footballdata.importer.FootballDataHistoricalImportManager;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class HistoricalAhCoverageServiceTest {

	private static final Instant FIXED = Instant.parse("2026-08-16T15:00:00Z");

	@Autowired
	private HistoricalAhCoverageService coverageService;

	@Autowired
	private HistoricalMatchRepository matchRepository;

	@Autowired
	private HistoricalAhOfferRepository offerRepository;

	@Autowired
	private FootballDataHistoricalImportManager importManager;

	@Test
	void anySourceCoverageCountsDistinctMatchesNotQuoteRows() {
		persistOverlappingQuotes();
		HistoricalAhLeagueSeasonCoverage row = coverage(CanonicalCompetition.PREMIER_LEAGUE, 2023);
		assertThat(row.totalMatches()).isEqualTo(10);
		assertThat(source(row, HistoricalQuoteSource.BET365).matchesWithQuote()).isEqualTo(8);
		assertThat(source(row, HistoricalQuoteSource.BET365).coverageRate()).isEqualByComparingTo("0.8");
		assertThat(source(row, HistoricalQuoteSource.PINNACLE).coverageRate()).isEqualByComparingTo("0.6");
		assertThat(source(row, HistoricalQuoteSource.MARKET_MAX).coverageRate()).isEqualByComparingTo("0.4");
		assertThat(source(row, HistoricalQuoteSource.MARKET_AVERAGE).matchesWithQuote()).isZero();
		assertThat(row.matchesWithAnyQuote()).isEqualTo(8);
		assertThat(row.anyQuoteCoverageRate()).isEqualByComparingTo("0.8");
		assertThat(row.bestQuoteSource()).isEqualTo(HistoricalQuoteSource.BET365);
	}

	@Test
	void bestQuoteSourceTieUsesEnumOrder() {
		for (int i = 0; i < 4; i++) {
			HistoricalMatchEntity match = persistMatch(CanonicalCompetition.LA_LIGA, 2022, i);
			if (i < 2) {
				persistQuote(match, HistoricalQuoteSource.BET365);
				persistQuote(match, HistoricalQuoteSource.PINNACLE);
			}
		}
		HistoricalAhLeagueSeasonCoverage row = coverage(CanonicalCompetition.LA_LIGA, 2022);
		assertThat(source(row, HistoricalQuoteSource.BET365).coverageRate())
				.isEqualByComparingTo(source(row, HistoricalQuoteSource.PINNACLE).coverageRate());
		assertThat(row.bestQuoteSource()).isEqualTo(HistoricalQuoteSource.BET365);
	}

	@Test
	void leagueSeasonWithNoPersistedMatchesIsOmitted() {
		persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2023, 0);
		HistoricalAhCoverageReport report = coverageService.report();
		assertThat(report.leagueSeasons())
				.extracting(HistoricalAhLeagueSeasonCoverage::competition)
				.containsExactly(CanonicalCompetition.PREMIER_LEAGUE);
		assertThat(report.leagueSeasons())
				.noneMatch(row -> row.competition() == CanonicalCompetition.LIGUE_1);
		assertThat(HistoricalAhCoverageService.coverageRate(1, 0)).isEqualByComparingTo("0");
	}

	@Test
	void groupsByLeagueAndSeasonSeparately() {
		persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2022, 0);
		HistoricalMatchEntity plLater = persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2023, 0);
		persistQuote(plLater, HistoricalQuoteSource.BET365);
		persistMatch(CanonicalCompetition.BUNDESLIGA, 2023, 0);
		persistMatch(CanonicalCompetition.BUNDESLIGA, 2023, 1);
		HistoricalAhCoverageReport report = coverageService.report();
		assertThat(report.leagueSeasons()).hasSize(3);
		HistoricalAhLeagueSeasonCoverage pl2022 = coverage(CanonicalCompetition.PREMIER_LEAGUE, 2022);
		HistoricalAhLeagueSeasonCoverage pl2023 = coverage(CanonicalCompetition.PREMIER_LEAGUE, 2023);
		HistoricalAhLeagueSeasonCoverage bl2023 = coverage(CanonicalCompetition.BUNDESLIGA, 2023);
		assertThat(pl2022.totalMatches()).isEqualTo(1);
		assertThat(pl2022.matchesWithAnyQuote()).isZero();
		assertThat(pl2023.totalMatches()).isEqualTo(1);
		assertThat(pl2023.anyQuoteCoverageRate()).isEqualByComparingTo("1");
		assertThat(bl2023.totalMatches()).isEqualTo(2);
		assertThat(bl2023.matchesWithAnyQuote()).isZero();
		assertThat(pl2023.totalMatches()).isNotEqualTo(bl2023.totalMatches());
	}

	@Test
	void coverageUsesImporterValidQuotesFromFixture() {
		String csv = fixture("sample-e0.csv");
		HistoricalImportResult imported = importManager.importCsv(
				CanonicalCompetition.PREMIER_LEAGUE,
				new FootballSeason(2023, 2024),
				csv,
				"mmz4281/2324/E0.csv");
		assertThat(imported.matchesInserted()).isEqualTo(8);
		HistoricalAhLeagueSeasonCoverage row = coverage(CanonicalCompetition.PREMIER_LEAGUE, 2023);
		assertThat(row.totalMatches()).isEqualTo(8);
		assertThat(source(row, HistoricalQuoteSource.BET365).matchesWithQuote()).isEqualTo(4);
		assertThat(source(row, HistoricalQuoteSource.PINNACLE).matchesWithQuote()).isEqualTo(4);
		assertThat(source(row, HistoricalQuoteSource.MARKET_MAX).matchesWithQuote()).isEqualTo(6);
		assertThat(source(row, HistoricalQuoteSource.MARKET_AVERAGE).matchesWithQuote()).isEqualTo(6);
		assertThat(row.matchesWithAnyQuote()).isEqualTo(6);
		assertThat(row.anyQuoteCoverageRate()).isEqualByComparingTo("0.75");
		assertThat(row.bestQuoteSource()).isEqualTo(HistoricalQuoteSource.MARKET_MAX);
	}

	private void persistOverlappingQuotes() {
		for (int i = 0; i < 10; i++) {
			HistoricalMatchEntity match = persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2023, i);
			if (i < 8) {
				persistQuote(match, HistoricalQuoteSource.BET365);
			}
			if (i < 6) {
				persistQuote(match, HistoricalQuoteSource.PINNACLE);
			}
			if (i < 4) {
				persistQuote(match, HistoricalQuoteSource.MARKET_MAX);
			}
		}
	}

	private HistoricalMatchEntity persistMatch(CanonicalCompetition competition, int startYear, int index) {
		HistoricalMatchEntity match = new HistoricalMatchEntity();
		match.setSource(HistoricalSource.FOOTBALL_DATA_UK);
		match.setSourceCompetitionCode(competition.name());
		match.setCanonicalCompetition(competition);
		match.setSeasonStartYear(startYear);
		match.setSeasonEndYear(startYear + 1);
		match.setMatchDate(LocalDate.of(startYear, 8, 1).plusDays(index));
		match.setSourceHomeTeamName("Home-" + competition + "-" + startYear + "-" + index);
		match.setSourceAwayTeamName("Away-" + competition + "-" + startYear + "-" + index);
		match.setHomeGoals(1);
		match.setAwayGoals(0);
		match.setSourceFile("fixture.csv");
		match.setSourceRowNumber(index + 2);
		match.setCreatedAt(FIXED);
		match.setUpdatedAt(FIXED);
		return matchRepository.save(match);
	}

	private void persistQuote(HistoricalMatchEntity match, HistoricalQuoteSource quoteSource) {
		HistoricalAhOfferEntity offer = new HistoricalAhOfferEntity();
		offer.setHistoricalMatch(match);
		offer.setSource(HistoricalSource.FOOTBALL_DATA_UK);
		offer.setQuoteSource(quoteSource);
		offer.setHomeHandicapLine(new BigDecimal("-0.25"));
		offer.setHomeOdds(new BigDecimal("1.90"));
		offer.setAwayOdds(new BigDecimal("1.95"));
		offer.setObservationType(HistoricalObservationType.PRE_MATCH_SNAPSHOT);
		offer.setSourceLineColumn("LINE");
		offer.setSourceHomeOddsColumn("HOME");
		offer.setSourceAwayOddsColumn("AWAY");
		offer.setCreatedAt(FIXED);
		offer.setUpdatedAt(FIXED);
		offerRepository.save(offer);
	}

	private HistoricalAhLeagueSeasonCoverage coverage(CanonicalCompetition competition, int startYear) {
		return coverageService.report().leagueSeasons().stream()
				.filter(row -> row.competition() == competition && row.season().startYear() == startYear)
				.findFirst()
				.orElseThrow();
	}

	private static HistoricalAhQuoteSourceCoverage source(
			HistoricalAhLeagueSeasonCoverage row, HistoricalQuoteSource quoteSource) {
		return row.sourceCoverages().stream()
				.filter(item -> item.quoteSource() == quoteSource)
				.findFirst()
				.orElseThrow();
	}

	private static String fixture(String name) {
		try {
			return new ClassPathResource("historical/footballdata/" + name).getContentAsString(StandardCharsets.UTF_8);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
