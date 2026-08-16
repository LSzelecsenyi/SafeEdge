package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalImportResult;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.footballdata.importer.FootballDataHistoricalImportManager;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.nio.charset.StandardCharsets;
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
class HistoricalFootballDataImportServiceTest {

	@Autowired
	private FootballDataHistoricalImportManager importManager;

	@Autowired
	private HistoricalMatchRepository matchRepository;

	@Autowired
	private HistoricalAhOfferRepository offerRepository;

	@Test
	void importsMatchesAndQuotesIdempotentlyAndSkipsBadQuotes() {
		String csv = fixture("sample-e0.csv");
		FootballSeason season = new FootballSeason(2023, 2024);
		HistoricalImportResult first = importManager.importCsv(
				CanonicalCompetition.PREMIER_LEAGUE, season, csv, "mmz4281/2324/E0.csv");
		assertThat(first.source()).isEqualTo(HistoricalSource.FOOTBALL_DATA_UK);
		assertThat(first.matchesInserted()).isEqualTo(8);
		assertThat(first.quotesInserted()).isPositive();
		assertThat(first.quotesSkippedIncomplete()).isPositive();
		assertThat(first.quotesSkippedInvalidOdds()).isPositive();
		assertThat(first.quotesSkippedInvalidLine()).isPositive();

		HistoricalMatchEntity everton = matchRepository
				.findBySourceAndCanonicalCompetitionAndSeasonStartYearAndSeasonEndYearAndMatchDateAndSourceHomeTeamNameAndSourceAwayTeamName(
						HistoricalSource.FOOTBALL_DATA_UK,
						CanonicalCompetition.PREMIER_LEAGUE,
						2023,
						2024,
						LocalDate.of(2023, 8, 14),
						"Everton",
						"Fulham")
				.orElseThrow();
		assertThat(everton.getHomeGoals()).isEqualTo(1);
		assertThat(offerRepository.findByHistoricalMatch_IdAndQuoteSourceAndObservationType(
				everton.getId(),
				HistoricalQuoteSource.BET365,
				com.safeedge.historical.domain.HistoricalObservationType.PRE_MATCH_SNAPSHOT)).isEmpty();
		assertThat(everton.getKickoffUtc()).isNull();
		assertThat(everton.getSource()).isEqualTo(HistoricalSource.FOOTBALL_DATA_UK);
		assertThat(everton.getSource()).isNotEqualTo(com.safeedge.event.domain.BettingProviders.TIPPMIX);

		HistoricalImportResult second = importManager.importCsv(
				CanonicalCompetition.PREMIER_LEAGUE, season, csv, "mmz4281/2324/E0.csv");
		assertThat(second.matchesInserted()).isZero();
		assertThat(matchRepository.count()).isEqualTo(8);

		String corrected = csv.replace("Arsenal,Nott'm Forest,2,1", "Arsenal,Nott'm Forest,3,1")
				.replace("-1.00,-1.00,1.90,2.00", "-1.00,-1.00,1.91,2.00");
		HistoricalImportResult third = importManager.importCsv(
				CanonicalCompetition.PREMIER_LEAGUE, season, corrected, "mmz4281/2324/E0.csv");
		assertThat(third.matchesUpdated()).isEqualTo(1);
		assertThat(third.quotesUpdated()).isPositive();
		HistoricalMatchEntity arsenal = matchRepository
				.findBySourceAndCanonicalCompetitionAndSeasonStartYearAndSeasonEndYearAndMatchDateAndSourceHomeTeamNameAndSourceAwayTeamName(
						HistoricalSource.FOOTBALL_DATA_UK,
						CanonicalCompetition.PREMIER_LEAGUE,
						2023,
						2024,
						LocalDate.of(2023, 8, 12),
						"Arsenal",
						"Nott'm Forest")
				.orElseThrow();
		assertThat(arsenal.getHomeGoals()).isEqualTo(3);
		HistoricalAhOfferEntity bet365 = offerRepository
				.findByHistoricalMatch_IdAndQuoteSourceAndObservationType(
						arsenal.getId(),
						HistoricalQuoteSource.BET365,
						com.safeedge.historical.domain.HistoricalObservationType.PRE_MATCH_SNAPSHOT)
				.orElseThrow();
		assertThat(bet365.getHomeOdds()).isEqualByComparingTo("1.91");
		assertThat(bet365.getHomeHandicapLine()).isEqualByComparingTo("-1");
		assertThat(matchRepository.count()).isEqualTo(8);
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
