package com.safeedge.historical.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.PostgresTestcontainersConfig;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.HistoricalWalkForwardIdentities;
import com.safeedge.historical.evaluation.WalkForwardEvaluationRequest;
import com.safeedge.historical.repository.HistoricalAhOfferEntity;
import com.safeedge.historical.repository.HistoricalAhOfferRepository;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.historical.repository.HistoricalMatchRepository;
import com.safeedge.probability.ProbabilityModelConfig;
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
class HistoricalWalkForwardEvaluationServiceTest {

	private static final Instant FIXED = Instant.parse("2026-08-16T16:00:00Z");
	private static final ProbabilityModelConfig MIN1 = new ProbabilityModelConfig(180, 10, 1);

	@Autowired
	private HistoricalWalkForwardEvaluationService evaluationService;

	@Autowired
	private HistoricalMatchRepository matchRepository;

	@Autowired
	private HistoricalAhOfferRepository offerRepository;

	@Test
	void loadsPriorSeasonAsWarmupAndUsesOnlySelectedQuoteSource() {
		LocalDate warmupHome = LocalDate.of(2023, 5, 1);
		LocalDate warmupAway = LocalDate.of(2023, 5, 8);
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		persistMatch(CanonicalCompetition.BUNDESLIGA, 2022, "H", "X", warmupHome, 3, 0, 2);
		persistMatch(CanonicalCompetition.BUNDESLIGA, 2022, "Y", "A", warmupAway, 0, 3, 3);
		HistoricalMatchEntity eval = persistMatch(CanonicalCompetition.BUNDESLIGA, 2023, "H", "A", evalDate, 1, 0, 4);
		persistQuote(eval, HistoricalQuoteSource.BET365, new BigDecimal("1.80"), new BigDecimal("2.10"));
		persistQuote(eval, HistoricalQuoteSource.PINNACLE, new BigDecimal("1.95"), new BigDecimal("1.95"));

		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				CanonicalCompetition.BUNDESLIGA,
				2022,
				2023,
				2023,
				HistoricalQuoteSource.PINNACLE,
				MIN1);
		HistoricalWalkForwardDataset dataset = evaluationService.buildDataset(request);
		assertThat(dataset.stats().matchesLoaded()).isEqualTo(3);
		assertThat(dataset.stats().matchesEvaluated()).isEqualTo(1);
		assertThat(dataset.stats().predictionsAvailable()).isEqualTo(1);
		assertThat(dataset.stats().predictionsWithSelectedAhQuote()).isEqualTo(1);
		assertThat(dataset.stats().quoteSource()).isEqualTo(HistoricalQuoteSource.PINNACLE);
		assertThat(dataset.opportunities()).hasSize(2);
		assertThat(dataset.opportunities().getFirst().market().provider())
				.isEqualTo(HistoricalWalkForwardIdentities.PROVIDER);
		assertThat(dataset.opportunities().getFirst().market().provider()).isNotEqualTo("TIPPMIX");
		assertThat(dataset.opportunities().getFirst().selection().selectionType()).isEqualTo(SelectionType.HOME);
		assertThat(dataset.opportunities().getFirst().opportunity().odds()).isEqualByComparingTo("1.95");
		assertThat(dataset.opportunities().get(1).selection().selectionType()).isEqualTo(SelectionType.AWAY);
		assertThat(dataset.opportunities().get(1).opportunity().odds()).isEqualByComparingTo("1.95");

		WalkForwardEvaluationRequest cold = new WalkForwardEvaluationRequest(
				CanonicalCompetition.BUNDESLIGA,
				2023,
				2023,
				2023,
				HistoricalQuoteSource.PINNACLE,
				MIN1);
		HistoricalWalkForwardDataset withoutWarmup = evaluationService.buildDataset(cold);
		assertThat(withoutWarmup.stats().matchesLoaded()).isEqualTo(1);
		assertThat(withoutWarmup.stats().predictionsAvailable()).isZero();
		assertThat(withoutWarmup.opportunities()).isEmpty();
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
		match.setSourceFile("walk-forward-fixture.csv");
		match.setSourceRowNumber(sourceRowNumber);
		match.setCreatedAt(FIXED);
		match.setUpdatedAt(FIXED);
		return matchRepository.save(match);
	}

	private void persistQuote(
			HistoricalMatchEntity match, HistoricalQuoteSource quoteSource, BigDecimal homeOdds, BigDecimal awayOdds) {
		HistoricalAhOfferEntity offer = new HistoricalAhOfferEntity();
		offer.setHistoricalMatch(match);
		offer.setSource(HistoricalSource.FOOTBALL_DATA_UK);
		offer.setQuoteSource(quoteSource);
		offer.setHomeHandicapLine(new BigDecimal("-0.25"));
		offer.setHomeOdds(homeOdds);
		offer.setAwayOdds(awayOdds);
		offer.setObservationType(HistoricalObservationType.PRE_MATCH_SNAPSHOT);
		offer.setSourceLineColumn("PAH");
		offer.setSourceHomeOddsColumn("PAHH");
		offer.setSourceAwayOddsColumn("PAHA");
		offer.setCreatedAt(FIXED);
		offer.setUpdatedAt(FIXED);
		offerRepository.save(offer);
	}
}
