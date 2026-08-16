package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.safeedge.candidate.ScoreProbability;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PoissonFootballProbabilityModelTest {

	private static final CanonicalCompetition PL = CanonicalCompetition.PREMIER_LEAGUE;
	private static final CanonicalCompetition BL = CanonicalCompetition.BUNDESLIGA;
	private static final LocalDate TARGET_DATE = LocalDate.of(2024, 6, 1);
	private static final ProbabilityModelConfig MIN1 = new ProbabilityModelConfig(180, 10, 1);
	private static final ProbabilityModelConfig MIN5 = ProbabilityModelConfig.defaults();

	private final PoissonFootballProbabilityModel model = new PoissonFootballProbabilityModel(MIN1);

	@Test
	void noLeagueHistoryIsUnavailable() {
		ProbabilityPrediction prediction = model.predict(List.of(), target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.NO_LEAGUE_HISTORY);
		assertThat(prediction.scoreDistribution()).isNull();
		assertThat(prediction.homeExpectedGoals()).isNull();
	}

	@Test
	void otherCompetitionDoesNotCreateLeagueHistory() {
		List<ProbabilityTrainingMatch> training = List.of(match(BL, "H", "A", TARGET_DATE.minusDays(7), 2, 1));
		ProbabilityPrediction prediction = model.predict(training, target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.NO_LEAGUE_HISTORY);
		assertThat(prediction.trainingMatchCount()).isZero();
	}

	@Test
	void insufficientHomeHistoryIsUnavailable() {
		PoissonFootballProbabilityModel strict = new PoissonFootballProbabilityModel(MIN5);
		List<ProbabilityTrainingMatch> training = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			training.add(match(PL, "H", "X" + i, TARGET_DATE.minusDays(i * 7L), 1, 0));
		}
		for (int i = 1; i <= 5; i++) {
			training.add(match(PL, "Y" + i, "A", TARGET_DATE.minusDays(i * 7L + 1), 1, 0));
		}
		ProbabilityPrediction prediction = strict.predict(training, target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(prediction.homeHistoryCount()).isEqualTo(4);
		assertThat(prediction.awayHistoryCount()).isEqualTo(5);
		assertThat(prediction.scoreDistribution()).isNull();
	}

	@Test
	void insufficientAwayHistoryIsUnavailable() {
		PoissonFootballProbabilityModel strict = new PoissonFootballProbabilityModel(MIN5);
		List<ProbabilityTrainingMatch> training = new ArrayList<>();
		for (int i = 1; i <= 5; i++) {
			training.add(match(PL, "H", "X" + i, TARGET_DATE.minusDays(i * 7L), 1, 0));
		}
		for (int i = 1; i <= 4; i++) {
			training.add(match(PL, "Y" + i, "A", TARGET_DATE.minusDays(i * 7L + 1), 1, 0));
		}
		ProbabilityPrediction prediction = strict.predict(training, target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(prediction.awayHistoryCount()).isEqualTo(4);
	}

	@Test
	void promotedTeamWithNoVenueHistoryIsUnavailable() {
		List<ProbabilityTrainingMatch> training = List.of(
				match(PL, "H", "X", TARGET_DATE.minusDays(7), 2, 1),
				match(PL, "Y", "A", TARGET_DATE.minusDays(14), 1, 0));
		ProbabilityPrediction missingHome =
				model.predict(List.of(match(PL, "Y", "A", TARGET_DATE.minusDays(14), 1, 0)), target("New", "A"));
		assertThat(missingHome.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(missingHome.homeHistoryCount()).isZero();
		ProbabilityPrediction missingAway =
				model.predict(List.of(match(PL, "H", "X", TARGET_DATE.minusDays(7), 2, 1)), target("H", "New"));
		assertThat(missingAway.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(missingAway.awayHistoryCount()).isZero();
		assertThat(model.predict(training, target("H", "A")).available()).isTrue();
	}

	@Test
	void targetMatchScoreDoesNotAffectItsOwnPrediction() {
		List<ProbabilityTrainingMatch> history = averageLeagueHistory();
		ProbabilityTrainingMatch asDraw = match(PL, "H", "A", TARGET_DATE, 0, 0);
		ProbabilityTrainingMatch asBlowout = match(PL, "H", "A", TARGET_DATE, 7, 0);
		ProbabilityPrediction without = model.predict(history, target("H", "A"));
		ProbabilityPrediction withDraw = model.predict(concat(history, asDraw), target("H", "A"));
		ProbabilityPrediction withBlowout = model.predict(concat(history, asBlowout), target("H", "A"));
		assertThat(withDraw).isEqualTo(without);
		assertThat(withBlowout).isEqualTo(without);
		assertThat(without.available()).isTrue();
	}

	@Test
	void sameDateResultsAreExcluded() {
		List<ProbabilityTrainingMatch> history = averageLeagueHistory();
		ProbabilityTrainingMatch sameDate = match(PL, "X", "Y", TARGET_DATE, 8, 0);
		assertThat(model.predict(concat(history, sameDate), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void futureMatchDoesNotAffectEarlierPrediction() {
		List<ProbabilityTrainingMatch> history = averageLeagueHistory();
		ProbabilityTrainingMatch future = match(PL, "H", "A", TARGET_DATE.plusDays(20), 9, 0);
		assertThat(model.predict(concat(history, future), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void previousSeasonHistoryIsAllowed() {
		List<ProbabilityTrainingMatch> training = List.of(
				match(PL, "H", "X", LocalDate.of(2023, 5, 20), 2, 1),
				match(PL, "Y", "A", LocalDate.of(2023, 5, 21), 1, 1));
		ProbabilityPrediction prediction =
				model.predict(training, new MatchPredictionContext(PL, "H", "A", LocalDate.of(2024, 8, 17)));
		assertThat(prediction.available()).isTrue();
		assertThat(prediction.trainingMatchCount()).isEqualTo(2);
	}

	@Test
	void recentMatchWeighsMoreThanOldMatch() {
		ProbabilityTrainingMatch oldHigh = match(PL, "H", "A", TARGET_DATE.minusDays(180), 10, 1);
		ProbabilityTrainingMatch recentLow = match(PL, "H", "A", TARGET_DATE.minusDays(1), 0, 1);
		ProbabilityTrainingMatch oldLow = match(PL, "H", "A", TARGET_DATE.minusDays(180), 0, 1);
		ProbabilityTrainingMatch recentHigh = match(PL, "H", "A", TARGET_DATE.minusDays(1), 10, 1);
		BigDecimal lambdaRecentHeavy = model.predict(List.of(oldHigh, recentLow), target("H", "A")).homeExpectedGoals();
		BigDecimal lambdaOldHeavy = model.predict(List.of(oldLow, recentHigh), target("H", "A")).homeExpectedGoals();
		assertThat(lambdaRecentHeavy).isLessThan(lambdaOldHeavy);
	}

	@Test
	void averageTeamsReproduceLeagueLambdas() {
		ProbabilityPrediction prediction = model.predict(averageLeagueHistory(), target("H", "A"));
		assertThat(prediction.homeExpectedGoals()).isCloseTo(new BigDecimal("1.5"), within(new BigDecimal("0.0000001")));
		assertThat(prediction.awayExpectedGoals()).isCloseTo(new BigDecimal("1.0"), within(new BigDecimal("0.0000001")));
	}

	@Test
	void strongHomeAttackRaisesHomeLambda() {
		List<ProbabilityTrainingMatch> baseline = averageTeamsInLargeLeague();
		List<ProbabilityTrainingMatch> strong = new ArrayList<>(baseline);
		strong.add(match(PL, "H", "Z", TARGET_DATE.minusDays(3), 5, 1));
		assertThat(model.predict(strong, target("H", "A")).homeExpectedGoals())
				.isGreaterThan(model.predict(baseline, target("H", "A")).homeExpectedGoals());
	}

	@Test
	void weakHomeAttackLowersHomeLambda() {
		List<ProbabilityTrainingMatch> baseline = averageTeamsInLargeLeague();
		List<ProbabilityTrainingMatch> weak = new ArrayList<>(baseline);
		weak.add(match(PL, "H", "Z", TARGET_DATE.minusDays(3), 0, 1));
		assertThat(model.predict(weak, target("H", "A")).homeExpectedGoals())
				.isLessThan(model.predict(baseline, target("H", "A")).homeExpectedGoals());
	}

	@Test
	void finiteGridIsRenormalizedToSumOne() {
		ProbabilityPrediction prediction = model.predict(averageLeagueHistory(), target("H", "A"));
		assertThat(prediction.capturedProbabilityMassBeforeNormalization())
				.isGreaterThan(new BigDecimal("0.999"));
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : prediction.scoreDistribution().entries()) {
			sum = sum.add(entry.probability());
		}
		assertThat(sum).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(prediction.scoreDistribution().entries()).hasSize(11 * 11);
	}

	@Test
	void repeatedPredictionIsDeterministic() {
		List<ProbabilityTrainingMatch> training = averageLeagueHistory();
		assertThat(model.predict(training, target("H", "A"))).isEqualTo(model.predict(training, target("H", "A")));
	}

	@Test
	void nullInputsAreRejected() {
		assertThatThrownBy(() -> model.predict(null, target("H", "A")))
				.isInstanceOf(ProbabilityModelException.class);
		assertThatThrownBy(() -> model.predict(List.of(), null)).isInstanceOf(ProbabilityModelException.class);
	}

	private static List<ProbabilityTrainingMatch> averageLeagueHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>();
		LocalDate historyDate = TARGET_DATE.minusDays(10);
		for (int i = 0; i < 6; i++) {
			int homeGoals = i % 2 == 0 ? 2 : 1;
			matches.add(match(PL, "H", "A", historyDate, homeGoals, 1));
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> averageTeamsInLargeLeague() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>();
		LocalDate historyDate = TARGET_DATE.minusDays(10);
		for (int i = 0; i < 20; i++) {
			matches.add(match(PL, "L" + i, "R" + i, historyDate, i % 2 == 0 ? 2 : 1, 1));
		}
		matches.addAll(averageLeagueHistory());
		return matches;
	}

	private static ProbabilityTrainingMatch match(
			CanonicalCompetition competition, String home, String away, LocalDate date, int homeGoals, int awayGoals) {
		return new ProbabilityTrainingMatch(competition, date, home, away, new MatchScore(homeGoals, awayGoals));
	}

	private static MatchPredictionContext target(String home, String away) {
		return new MatchPredictionContext(PL, home, away, TARGET_DATE);
	}

	private static List<ProbabilityTrainingMatch> concat(
			List<ProbabilityTrainingMatch> history, ProbabilityTrainingMatch extra) {
		List<ProbabilityTrainingMatch> all = new ArrayList<>(history);
		all.add(extra);
		return all;
	}
}
