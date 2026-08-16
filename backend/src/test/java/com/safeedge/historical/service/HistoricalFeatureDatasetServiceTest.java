package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.features.HistoricalFeatureDataset;
import com.safeedge.historical.features.HistoricalModelRow;
import com.safeedge.historical.features.PreMatchFeatures;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(PostgresTestcontainersConfig.class)
@Transactional
class HistoricalFeatureDatasetServiceTest {

	private static final Instant FIXED = Instant.parse("2026-08-16T16:00:00Z");

	@Autowired
	private HistoricalFeatureDatasetService datasetService;

	@Autowired
	private HistoricalMatchRepository matchRepository;

	@Autowired
	private HistoricalAhOfferRepository offerRepository;

	@Test
	void loadsPersistedMatchesAndBuildsPointInTimeFeatures() {
		persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2023, "A", "B", LocalDate.of(2024, 1, 1), 4, 0, 2);
		persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2023, "A", "C", LocalDate.of(2024, 1, 8), 1, 1, 3);
		persistMatch(CanonicalCompetition.PREMIER_LEAGUE, 2024, "A", "D", LocalDate.of(2024, 8, 17), 0, 0, 4);
		HistoricalFeatureDataset dataset =
				datasetService.buildDataset(CanonicalCompetition.PREMIER_LEAGUE, 2023, 2024);
		assertThat(dataset.totalRows()).isEqualTo(3);
		HistoricalModelRow first = dataset.rows().getFirst();
		assertThat(first.features().homeLast5GoalsForPerMatch()).isNull();
		assertThat(first.target().homeGoals()).isEqualTo(4);
		HistoricalModelRow second = dataset.rows().get(1);
		assertThat(second.features().homeLast5GoalsForPerMatch()).isEqualByComparingTo("4");
		HistoricalModelRow third = dataset.rows().get(2);
		assertThat(third.season().startYear()).isEqualTo(2024);
		assertThat(third.features().homeTeamMatchesPlayed()).isEqualTo(2);
		assertThat(third.features().leagueMatchesObserved()).isZero();
		assertThat(third.features().leagueHomeGoalsPerMatch()).isNull();
	}

	@Test
	void ahQuotesDoNotChangeFeatureOutput() {
		HistoricalMatchEntity first =
				persistMatch(CanonicalCompetition.SERIE_A, 2023, "A", "B", LocalDate.of(2024, 1, 1), 2, 1, 2);
		HistoricalMatchEntity second =
				persistMatch(CanonicalCompetition.SERIE_A, 2023, "A", "C", LocalDate.of(2024, 1, 8), 0, 0, 3);
		HistoricalFeatureDataset withoutOdds = datasetService.buildDataset(CanonicalCompetition.SERIE_A, 2023, 2023);
		persistQuote(first);
		persistQuote(second);
		HistoricalFeatureDataset withOdds = datasetService.buildDataset(CanonicalCompetition.SERIE_A, 2023, 2023);
		assertThat(withOdds.rows()).hasSameSizeAs(withoutOdds.rows());
		for (int i = 0; i < withoutOdds.rows().size(); i++) {
			PreMatchFeatures expected = withoutOdds.rows().get(i).features();
			PreMatchFeatures actual = withOdds.rows().get(i).features();
			assertThat(actual).isEqualTo(expected);
		}
	}

	@Test
	void rejectsInvalidSeasonRange() {
		assertThatThrownBy(() -> datasetService.buildDataset(CanonicalCompetition.LA_LIGA, 2024, 2023))
				.isInstanceOf(HistoricalDataException.class)
				.hasMessageContaining("Season range");
	}

	private HistoricalMatchEntity persistMatch(
			CanonicalCompetition competition,
			int startYear,
			String home,
			String away,
			LocalDate date,
			int homeGoals,
			int awayGoals,
			int sourceRowNumber) {
		HistoricalMatchEntity match = new HistoricalMatchEntity();
		match.setSource(HistoricalSource.FOOTBALL_DATA_UK);
		match.setSourceCompetitionCode(competition.name());
		match.setCanonicalCompetition(competition);
		match.setSeasonStartYear(startYear);
		match.setSeasonEndYear(startYear + 1);
		match.setMatchDate(date);
		match.setSourceHomeTeamName(home);
		match.setSourceAwayTeamName(away);
		match.setHomeGoals(homeGoals);
		match.setAwayGoals(awayGoals);
		match.setSourceFile("features-fixture.csv");
		match.setSourceRowNumber(sourceRowNumber);
		match.setCreatedAt(FIXED);
		match.setUpdatedAt(FIXED);
		return matchRepository.save(match);
	}

	private void persistQuote(HistoricalMatchEntity match) {
		HistoricalAhOfferEntity offer = new HistoricalAhOfferEntity();
		offer.setHistoricalMatch(match);
		offer.setSource(HistoricalSource.FOOTBALL_DATA_UK);
		offer.setQuoteSource(HistoricalQuoteSource.BET365);
		offer.setHomeHandicapLine(new BigDecimal("-0.25"));
		offer.setHomeOdds(new BigDecimal("1.90"));
		offer.setAwayOdds(new BigDecimal("2.00"));
		offer.setObservationType(HistoricalObservationType.PRE_MATCH_SNAPSHOT);
		offer.setSourceLineColumn("B365AH");
		offer.setSourceHomeOddsColumn("B365AHH");
		offer.setSourceAwayOddsColumn("B365AHA");
		offer.setCreatedAt(FIXED);
		offer.setUpdatedAt(FIXED);
		offerRepository.save(offer);
	}
}
