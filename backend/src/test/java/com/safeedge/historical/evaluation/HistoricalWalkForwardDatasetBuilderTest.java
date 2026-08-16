package com.safeedge.historical.evaluation;

import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.AWAY_ODDS;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.HOME_ODDS;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.LINE_ZERO;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.MIN1;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.S22;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.S23;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.eval2023;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.eval2023NoWarmup;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.match;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.quote;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.quotes;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.side;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.strongHomeWarmup;
import static com.safeedge.historical.evaluation.HistoricalWalkForwardFixtures.venueWarmup;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.candidate.CandidateEngine;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.probability.FootballProbabilityModel;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.probability.ProbabilityModelV2Config;
import com.safeedge.probability.ProbabilityPrediction;
import com.safeedge.probability.ProbabilityPredictionStatus;
import com.safeedge.probability.RegularizedDixonColesFootballProbabilityModel;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HistoricalWalkForwardDatasetBuilderTest {

	private final HistoricalWalkForwardDatasetBuilder builder = new HistoricalWalkForwardDatasetBuilder();

	@Test
	void targetPredictionIgnoresItsOwnAndFutureResults() {
		LocalDate evalDate = LocalDate.of(2023, 8, 20);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		HistoricalMatchRecord targetBlowout = match(S23, "H", "A", evalDate, 8, 0, 10);
		HistoricalMatchRecord future = match(S23, "H", "A", evalDate.plusDays(14), 0, 8, 11);
		matches.add(targetBlowout);
		matches.add(future);
		Map<String, HistoricalAhQuoteSnapshot> quotes = quotes(
				quote(targetBlowout, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));
		HistoricalWalkForwardDataset withFuture = builder.build(matches, quotes, eval2023(HistoricalQuoteSource.PINNACLE));

		HistoricalMatchRecord targetQuiet = match(S23, "H", "A", evalDate, 0, 0, 10);
		List<HistoricalMatchRecord> withoutFuture = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		withoutFuture.add(targetQuiet);
		HistoricalWalkForwardDataset isolated = builder.build(
				withoutFuture,
				quotes(quote(targetQuiet, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));

		BigDecimal homeEdgeWithFuture = side(withFuture, targetBlowout, SelectionType.HOME).opportunity().edge();
		BigDecimal homeEdgeIsolated = side(isolated, targetQuiet, SelectionType.HOME).opportunity().edge();
		assertThat(homeEdgeWithFuture).isEqualByComparingTo(homeEdgeIsolated);
		assertThat(withFuture.stats().matchesEvaluated()).isEqualTo(2);
		assertThat(isolated.stats().matchesEvaluated()).isEqualTo(1);
	}

	@Test
	void sameDateMatchesDoNotTrainEachOtherAndNextDayCanSeeBoth() {
		LocalDate jan1 = LocalDate.of(2023, 1, 1);
		LocalDate jan2 = LocalDate.of(2023, 1, 2);
		List<HistoricalMatchRecord> warmup = new ArrayList<>();
		warmup.addAll(venueWarmup(S22, "A", "B", jan1, 1));
		warmup.addAll(venueWarmup(S22, "C", "D", jan1.minusDays(1), 3));
		warmup.addAll(venueWarmup(S22, "E", "F", jan1.minusDays(2), 5));
		HistoricalMatchRecord ab = match(S23, "A", "B", jan1, 8, 0, 20);
		HistoricalMatchRecord cd = match(S23, "C", "D", jan1, 1, 1, 21);
		HistoricalMatchRecord ef = match(S23, "E", "F", jan2, 0, 0, 22);
		Map<String, HistoricalAhQuoteSnapshot> allQuotes = quotes(
				quote(ab, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS),
				quote(cd, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS),
				quote(ef, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));

		List<HistoricalMatchRecord> withAb = new ArrayList<>(warmup);
		withAb.add(ab);
		withAb.add(cd);
		withAb.add(ef);
		HistoricalWalkForwardDataset bothJan1 = builder.build(withAb, allQuotes, eval2023(HistoricalQuoteSource.PINNACLE));

		List<HistoricalMatchRecord> withoutAb = new ArrayList<>(warmup);
		withoutAb.add(cd);
		withoutAb.add(ef);
		HistoricalWalkForwardDataset noAb = builder.build(
				withoutAb,
				quotes(
						quote(cd, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS),
						quote(ef, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));

		assertThat(side(bothJan1, cd, SelectionType.HOME).opportunity().edge())
				.isEqualByComparingTo(side(noAb, cd, SelectionType.HOME).opportunity().edge());
		assertThat(side(bothJan1, cd, SelectionType.AWAY).opportunity().edge())
				.isEqualByComparingTo(side(noAb, cd, SelectionType.AWAY).opportunity().edge());
		assertThat(side(bothJan1, ef, SelectionType.HOME).opportunity().edge())
				.isNotEqualByComparingTo(side(noAb, ef, SelectionType.HOME).opportunity().edge());
	}

	@Test
	void priorSeasonWarmupMakesFirstEvaluationPredictionAvailable() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(venueWarmup(S22, "H", "A", evalDate, 1));
		matches.add(eval);
		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));

		HistoricalWalkForwardDataset warmed =
				builder.build(matches, quotes, eval2023(HistoricalQuoteSource.PINNACLE));
		assertThat(warmed.stats().predictionsAvailable()).isEqualTo(1);
		assertThat(warmed.stats().candidatesGenerated()).isEqualTo(2);
		assertThat(warmed.stats().matchesEvaluated()).isEqualTo(1);
		assertThat(warmed.stats().matchesLoaded()).isEqualTo(3);

		HistoricalWalkForwardDataset cold =
				builder.build(matches, quotes, eval2023NoWarmup(HistoricalQuoteSource.PINNACLE));
		assertThat(cold.stats().matchesLoaded()).isEqualTo(1);
		assertThat(cold.stats().predictionsAvailable()).isZero();
		assertThat(cold.stats().candidatesGenerated()).isZero();
		assertThat(cold.stats().matchesSkippedNoLeagueHistory() + cold.stats().matchesSkippedInsufficientHistory())
				.isEqualTo(1);
	}

	@Test
	void usesOnlySelectedQuoteSourceAndSkipsWhenMissing() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(venueWarmup(S22, "H", "A", evalDate, 1));
		matches.add(eval);
		HistoricalAhQuoteSnapshot bet365 = quote(
				eval, HistoricalQuoteSource.BET365, new BigDecimal("-0.25"), new BigDecimal("1.90"), new BigDecimal("2.00"));
		HistoricalWalkForwardDataset skipped = builder.build(
				matches, quotes(bet365), eval2023(HistoricalQuoteSource.PINNACLE));
		assertThat(skipped.stats().predictionsAvailable()).isEqualTo(1);
		assertThat(skipped.stats().predictionsWithSelectedAhQuote()).isZero();
		assertThat(skipped.stats().matchesSkippedMissingQuote()).isEqualTo(1);
		assertThat(skipped.opportunities()).isEmpty();
		assertThat(skipped.stats().logLossObservations() + skipped.stats().logLossMissingFromGrid()).isEqualTo(1);

		HistoricalWalkForwardDataset used = builder.build(
				matches, quotes(bet365), eval2023(HistoricalQuoteSource.BET365));
		assertThat(used.stats().predictionsWithSelectedAhQuote()).isEqualTo(1);
		assertThat(side(used, eval, SelectionType.HOME).opportunity().odds()).isEqualByComparingTo("1.90");
		assertThat(side(used, eval, SelectionType.HOME).selection().line()).isEqualByComparingTo("-0.25");
		assertThat(side(used, eval, SelectionType.AWAY).opportunity().odds()).isEqualByComparingTo("2.00");
		assertThat(side(used, eval, SelectionType.AWAY).selection().line()).isEqualByComparingTo("0.25");
	}

	@Test
	void homeAndAwayCandidatesUseHistoricalOddsAndCandidateEngineEdge() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 2, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.add(eval);
		HistoricalWalkForwardDataset dataset = builder.build(
				matches,
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));
		HistoricalBettingOpportunity home = side(dataset, eval, SelectionType.HOME);
		HistoricalBettingOpportunity away = side(dataset, eval, SelectionType.AWAY);
		assertThat(home.market().provider()).isEqualTo(HistoricalSource.FOOTBALL_DATA_UK.name());
		assertThat(home.market().provider()).isNotEqualTo("TIPPMIX");
		assertThat(home.opportunity().odds()).isEqualByComparingTo(HOME_ODDS);
		assertThat(away.opportunity().odds()).isEqualByComparingTo(AWAY_ODDS);
		assertThat(home.selection().line()).isEqualByComparingTo(LINE_ZERO);
		assertThat(away.selection().line()).isEqualByComparingTo(LINE_ZERO.negate());
		assertThat(home.opportunity().edge()).isPositive();
		assertThat(away.opportunity().edge()).isNegative();
		assertThat(dataset.stats().positiveEvCandidates()).isGreaterThanOrEqualTo(1);
		assertThat(dataset.stats().negativeEvCandidates()).isGreaterThanOrEqualTo(1);
		assertThat(dataset.eventResults()).hasSize(1);
		HistoricalEventResult result = dataset.eventResults().getFirst();
		assertThat(result.eventId()).isEqualTo(home.opportunity().eventId());
		assertThat(result.finalScore()).isEqualTo(new MatchScore(2, 0));
		assertThat(home.opportunity().toString()).doesNotContain("2-0");
	}

	@Test
	void includesNegativeEvCandidatesAndKeepsIdentityOrder() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord first = match(S23, "H", "A", evalDate, 1, 0, 10);
		HistoricalMatchRecord second = match(S23, "C", "D", evalDate, 0, 0, 11);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.addAll(venueWarmup(S22, "C", "D", evalDate, 4));
		matches.add(second);
		matches.add(first);
		HistoricalWalkForwardDataset dataset = builder.build(
				matches,
				quotes(
						quote(first, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS),
						quote(second, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));
		assertThat(dataset.opportunities()).hasSize(4);
		assertThat(dataset.opportunities().get(0).opportunity().eventId())
				.isEqualTo(HistoricalWalkForwardIdentities.eventId(first));
		assertThat(dataset.opportunities().get(0).selection().selectionType()).isEqualTo(SelectionType.HOME);
		assertThat(dataset.opportunities().get(1).selection().selectionType()).isEqualTo(SelectionType.AWAY);
		assertThat(dataset.opportunities().get(2).opportunity().eventId())
				.isEqualTo(HistoricalWalkForwardIdentities.eventId(second));
		assertThat(dataset.stats().negativeEvCandidates()).isGreaterThan(0);
		assertThat(dataset.opportunities())
				.extracting(opportunity -> opportunity.opportunity().edge())
				.anySatisfy(edge -> assertThat(edge).isNegative());
		assertThat(dataset.opportunities().get(0).decisionAt())
				.isEqualTo(dataset.opportunities().get(2).decisionAt());
		assertThat(dataset.opportunities().get(0).decisionAt())
				.isEqualTo(HistoricalSyntheticChronology.decisionAt(evalDate));
		assertThat(dataset.eventResults().getFirst().settlementAt())
				.isEqualTo(HistoricalSyntheticChronology.settlementAt(evalDate));
	}

	@Test
	void unavailableModelProducesNoCandidateAndNoFakeEdge() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 1);
		HistoricalWalkForwardDataset dataset = builder.build(
				List.of(eval),
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023NoWarmup(HistoricalQuoteSource.PINNACLE));
		assertThat(dataset.opportunities()).isEmpty();
		assertThat(dataset.eventResults()).isEmpty();
		assertThat(dataset.stats().candidatesGenerated()).isZero();
		assertThat(dataset.stats().predictionsAvailable()).isZero();
		assertThat(dataset.stats().matchesSkippedNoLeagueHistory()).isEqualTo(1);
	}

	@Test
	void isDeterministicForTheSameFacts() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 2, 1, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.add(eval);
		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));
		WalkForwardEvaluationRequest request = eval2023(HistoricalQuoteSource.PINNACLE);
		HistoricalWalkForwardDataset first = builder.build(matches, quotes, request);
		HistoricalWalkForwardDataset second = builder.build(matches, quotes, request);
		assertThat(second).isEqualTo(first);
	}

	@Test
	void doesNotEvaluateWarmupSeasonsAsBets() {
		LocalDate warmupDate = LocalDate.of(2022, 12, 1);
		HistoricalMatchRecord warmupEvalWouldBe = match(S22, "H", "A", warmupDate, 1, 0, 1);
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>();
		matches.add(warmupEvalWouldBe);
		matches.addAll(venueWarmup(S22, "H", "A", evalDate, 2));
		matches.add(eval);
		HistoricalWalkForwardDataset dataset = builder.build(
				matches,
				quotes(
						quote(warmupEvalWouldBe, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS),
						quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));
		assertThat(dataset.opportunities())
				.allMatch(opportunity ->
						opportunity.opportunity().eventId().equals(HistoricalWalkForwardIdentities.eventId(eval)));
	}

	@Test
	void requestStoresExplicitModelConfigWithoutSelectingABestSetting() {
		WalkForwardEvaluationRequest request = new WalkForwardEvaluationRequest(
				CanonicalCompetition.PREMIER_LEAGUE,
				2014,
				2018,
				2023,
				HistoricalQuoteSource.PINNACLE,
				new ProbabilityModelConfig(90, 8, 3));
		assertThat(request.modelConfig()).isEqualTo(new ProbabilityModelConfig(90, 8, 3));
		assertThat(request.modelConfig()).isNotEqualTo(ProbabilityModelConfig.defaults());
		assertThat(MIN1.minimumTeamMatches()).isEqualTo(1);
	}

	@Test
	void injectedUnregularizedV2MatchesDefaultV1Builder() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 2, 1, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.add(eval);
		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));
		WalkForwardEvaluationRequest request = eval2023(HistoricalQuoteSource.PINNACLE);
		HistoricalWalkForwardDatasetBuilder v2Unregularized = new HistoricalWalkForwardDatasetBuilder(
				new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, BigDecimal.ZERO, false)),
				new CandidateEngine());
		assertThat(v2Unregularized.build(matches, quotes, request)).isEqualTo(builder.build(matches, quotes, request));
	}

	@Test
	void injectedV2ModelCanChangeCandidatesWithoutChangingCandidateEngine() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 2, 1, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(strongHomeWarmup(S22, evalDate, 1));
		matches.add(eval);
		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));
		WalkForwardEvaluationRequest request = eval2023(HistoricalQuoteSource.PINNACLE);
		HistoricalWalkForwardDatasetBuilder v2 = new HistoricalWalkForwardDatasetBuilder(
				new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("5"), true)),
				new CandidateEngine());
		HistoricalWalkForwardDataset v1Dataset = builder.build(matches, quotes, request);
		HistoricalWalkForwardDataset v2Dataset = v2.build(matches, quotes, request);
		assertThat(v2Dataset.stats().predictionsAvailable()).isEqualTo(v1Dataset.stats().predictionsAvailable());
		assertThat(v2Dataset.opportunities()).hasSameSizeAs(v1Dataset.opportunities());
	}

	@Test
	void buildWithPredictionsKeepsTheSameDatasetAndCapturesAvailablePredictionsWithoutQuotes() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(venueWarmup(S22, "H", "A", evalDate, 1));
		matches.add(eval);
		WalkForwardEvaluationRequest request = eval2023(HistoricalQuoteSource.PINNACLE);
		HistoricalWalkForwardBuildOutput withoutQuote =
				builder.buildWithPredictions(matches, Map.of(), request);
		assertThat(withoutQuote.dataset()).isEqualTo(builder.build(matches, Map.of(), request));
		assertThat(withoutQuote.dataset().opportunities()).isEmpty();
		assertThat(withoutQuote.predictions()).hasSize(1);
		assertThat(withoutQuote.predictions().getFirst().eventId())
				.isEqualTo(HistoricalWalkForwardIdentities.eventId(eval));
		assertThat(withoutQuote.predictions().getFirst().actualScore()).isEqualTo(new MatchScore(1, 0));
		assertThat(withoutQuote.predictions().getFirst().season()).isEqualTo(S23);

		Map<String, HistoricalAhQuoteSnapshot> quotes =
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS));
		HistoricalWalkForwardBuildOutput withQuote = builder.buildWithPredictions(matches, quotes, request);
		assertThat(withQuote.dataset()).isEqualTo(builder.build(matches, quotes, request));
		assertThat(withQuote.dataset().opportunities()).hasSize(2);
		assertThat(withQuote.predictions()).hasSize(1);
	}

	@Test
	void fittingFailedSkipsWithoutFallingBackOrInventingADistribution() {
		LocalDate evalDate = LocalDate.of(2023, 8, 12);
		HistoricalMatchRecord eval = match(S23, "H", "A", evalDate, 1, 0, 10);
		List<HistoricalMatchRecord> matches = new ArrayList<>(venueWarmup(S22, "H", "A", evalDate, 1));
		matches.add(eval);
		FootballProbabilityModel failing = (trainingData, target) -> ProbabilityPrediction.unavailable(
				ProbabilityPredictionStatus.FITTING_FAILED, trainingData.size(), 0, 0);
		HistoricalWalkForwardDatasetBuilder v3 = new HistoricalWalkForwardDatasetBuilder(failing, new CandidateEngine());
		HistoricalWalkForwardDataset dataset = v3.build(
				matches,
				quotes(quote(eval, HistoricalQuoteSource.PINNACLE, LINE_ZERO, HOME_ODDS, AWAY_ODDS)),
				eval2023(HistoricalQuoteSource.PINNACLE));
		assertThat(dataset.opportunities()).isEmpty();
		assertThat(dataset.stats().predictionsAvailable()).isZero();
		assertThat(dataset.stats().matchesSkippedFittingFailed()).isEqualTo(1);
	}
}
