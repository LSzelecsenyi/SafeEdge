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

class RegularizedDixonColesFootballProbabilityModelTest {

	private static final CanonicalCompetition PL = CanonicalCompetition.PREMIER_LEAGUE;
	private static final CanonicalCompetition BL = CanonicalCompetition.BUNDESLIGA;
	private static final LocalDate TARGET_DATE = LocalDate.of(2024, 6, 1);
	private static final ProbabilityModelV2Config MIN1_NO_DC =
			new ProbabilityModelV2Config(180, 10, 1, BigDecimal.ZERO, false);
	private static final ProbabilityModelV2Config MIN1_SHRINK5_NO_DC =
			new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("5"), false);

	@Test
	void shrinkageZeroAndDixonColesOffMatchesV1() {
		List<ProbabilityTrainingMatch> training = averageLeagueHistory();
		ProbabilityPrediction v1 = new PoissonFootballProbabilityModel(new ProbabilityModelConfig(180, 10, 1))
				.predict(training, target("H", "A"));
		ProbabilityPrediction v2 =
				new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC).predict(training, target("H", "A"));
		assertThat(v2.homeExpectedGoals()).isEqualByComparingTo(v1.homeExpectedGoals());
		assertThat(v2.awayExpectedGoals()).isEqualByComparingTo(v1.awayExpectedGoals());
		assertThat(v2.scoreDistribution()).isEqualTo(v1.scoreDistribution());
		assertThat(v2.status()).isEqualTo(v1.status());
	}

	@Test
	void largeShrinkagePullsLambdaTowardLeagueAverage() {
		List<ProbabilityTrainingMatch> strong = strongHomeHistory();
		BigDecimal raw = new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC)
				.predict(strong, target("H", "A"))
				.homeExpectedGoals();
		BigDecimal shrunk = new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("50"), false))
				.predict(strong, target("H", "A"))
				.homeExpectedGoals();
		BigDecimal leagueLike = new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC)
				.predict(averageLeagueHistory(), target("H", "A"))
				.homeExpectedGoals();
		assertThat(raw).isGreaterThan(leagueLike);
		assertThat(shrunk).isLessThan(raw);
		assertThat(shrunk).isGreaterThan(leagueLike);
	}

	@Test
	void recentMatchMattersMoreThanOldMatch() {
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC);
		ProbabilityTrainingMatch oldHigh = match(PL, "H", "A", TARGET_DATE.minusDays(180), 10, 1);
		ProbabilityTrainingMatch recentLow = match(PL, "H", "A", TARGET_DATE.minusDays(1), 0, 1);
		ProbabilityTrainingMatch oldLow = match(PL, "H", "A", TARGET_DATE.minusDays(180), 0, 1);
		ProbabilityTrainingMatch recentHigh = match(PL, "H", "A", TARGET_DATE.minusDays(1), 10, 1);
		BigDecimal lambdaRecentHeavy = model.predict(List.of(oldHigh, recentLow), target("H", "A")).homeExpectedGoals();
		BigDecimal lambdaOldHeavy = model.predict(List.of(oldLow, recentHigh), target("H", "A")).homeExpectedGoals();
		assertThat(lambdaRecentHeavy).isLessThan(lambdaOldHeavy);
	}

	@Test
	void leagueAverageTeamStaysNearStrengthOne() {
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC, recorder);
		assertThat(model.predict(averageLeagueHistory(), target("H", "A")).available()).isTrue();
		DixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.homeAttackStrength()).isCloseTo(BigDecimal.ONE, within(new BigDecimal("0.05")));
		assertThat(snapshot.awayAttackStrength()).isCloseTo(BigDecimal.ONE, within(new BigDecimal("0.05")));
	}

	@Test
	void strongTeamStaysAboveOneAfterShrinkage() {
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC, recorder)
				.predict(strongHomeHistory(), target("H", "A"));
		assertThat(recorder.snapshots().getFirst().homeAttackStrength()).isGreaterThan(BigDecimal.ONE);
	}

	@Test
	void weakTeamStaysBelowOneAfterShrinkage() {
		List<ProbabilityTrainingMatch> weak = new ArrayList<>(averageTeamsInLargeLeague());
		for (int i = 0; i < 6; i++) {
			weak.add(match(PL, "H", "Z" + i, TARGET_DATE.minusDays(3L + i), 0, 1));
		}
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC, recorder)
				.predict(weak, target("H", "A"));
		assertThat(recorder.snapshots().getFirst().homeAttackStrength()).isLessThan(BigDecimal.ONE);
		assertThat(recorder.snapshots().getFirst().homeAttackStrength()).isGreaterThan(BigDecimal.ZERO);
	}

	@Test
	void lambdaIsFiniteAndPositive() {
		ProbabilityPrediction prediction = new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC)
				.predict(averageLeagueHistory(), target("H", "A"));
		assertThat(prediction.homeExpectedGoals()).isPositive();
		assertThat(prediction.awayExpectedGoals()).isPositive();
		assertThat(Double.isFinite(prediction.homeExpectedGoals().doubleValue())).isTrue();
		assertThat(Double.isFinite(prediction.awayExpectedGoals().doubleValue())).isTrue();
	}

	@Test
	void distributionIsNonNegativeAndSumsToOne() {
		ProbabilityPrediction prediction = new RegularizedDixonColesFootballProbabilityModel(
						ProbabilityModelV2Config.defaults())
				.predict(averageLeagueHistory(), target("H", "A"));
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : prediction.scoreDistribution().entries()) {
			assertThat(entry.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
			sum = sum.add(entry.probability());
		}
		assertThat(sum).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void dixonColesChangesLowScoreMassRelativeToIndependent() {
		List<ProbabilityTrainingMatch> training = drawHeavyHistory();
		ProbabilityPrediction independent =
				new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC).predict(training, target("H", "A"));
		ProbabilityPrediction corrected = new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, BigDecimal.ZERO, true))
				.predict(training, target("H", "A"));
		assertThat(cell(corrected, 0, 0)).isNotEqualByComparingTo(cell(independent, 0, 0));
	}

	@Test
	void repeatedPredictionIsDeterministic() {
		List<ProbabilityTrainingMatch> training = drawHeavyHistory();
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config.defaults());
		assertThat(model.predict(training, target("H", "A"))).isEqualTo(model.predict(training, target("H", "A")));
	}

	@Test
	void targetMatchScoreDoesNotAffectItsOwnPrediction() {
		List<ProbabilityTrainingMatch> history = averageLeagueHistory();
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config.defaults());
		assertThat(model.predict(concat(history, match(PL, "H", "A", TARGET_DATE, 0, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
		assertThat(model.predict(concat(history, match(PL, "H", "A", TARGET_DATE, 7, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void sameDateResultsAreExcluded() {
		List<ProbabilityTrainingMatch> history = averageLeagueHistory();
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config.defaults());
		assertThat(model.predict(concat(history, match(PL, "X", "Y", TARGET_DATE, 8, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void futureMatchDoesNotAffectEarlierPredictionOrRho() {
		List<ProbabilityTrainingMatch> history = drawHeavyHistory();
		DixonColesFitRecorder first = new DixonColesFitRecorder();
		DixonColesFitRecorder second = new DixonColesFitRecorder();
		ProbabilityModelV2Config config = new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("5"), true);
		ProbabilityPrediction without = new RegularizedDixonColesFootballProbabilityModel(config, first)
				.predict(history, target("H", "A"));
		ProbabilityPrediction withFuture = new RegularizedDixonColesFootballProbabilityModel(config, second)
				.predict(concat(history, match(PL, "H", "A", TARGET_DATE.plusDays(20), 0, 0)), target("H", "A"));
		assertThat(withFuture).isEqualTo(without);
		assertThat(second.snapshots().getFirst().rho()).isEqualByComparingTo(first.snapshots().getFirst().rho());
	}

	@Test
	void insufficientHistoryIsPreservedDespiteShrinkage() {
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(ProbabilityModelV2Config.defaults());
		List<ProbabilityTrainingMatch> training = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			training.add(match(PL, "H", "X" + i, TARGET_DATE.minusDays(i * 7L), 1, 0));
		}
		for (int i = 1; i <= 5; i++) {
			training.add(match(PL, "Y" + i, "A", TARGET_DATE.minusDays(i * 7L + 1), 1, 0));
		}
		ProbabilityPrediction prediction = model.predict(training, target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(prediction.scoreDistribution()).isNull();
	}

	@Test
	void otherCompetitionDoesNotCreateLeagueHistory() {
		RegularizedDixonColesFootballProbabilityModel model =
				new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC);
		ProbabilityPrediction prediction =
				model.predict(List.of(match(BL, "H", "A", TARGET_DATE.minusDays(7), 2, 1)), target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.NO_LEAGUE_HISTORY);
	}

	@Test
	void zeroShrinkageSnapshotRawEqualsShrunk() {
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		new RegularizedDixonColesFootballProbabilityModel(MIN1_NO_DC, recorder)
				.predict(strongHomeHistory(), target("H", "A"));
		DixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.homeAttackStrength()).isEqualByComparingTo(snapshot.rawHomeAttackStrength());
		assertThat(snapshot.homeDefenceStrength()).isEqualByComparingTo(snapshot.rawHomeDefenceStrength());
		assertThat(snapshot.awayAttackStrength()).isEqualByComparingTo(snapshot.rawAwayAttackStrength());
		assertThat(snapshot.awayDefenceStrength()).isEqualByComparingTo(snapshot.rawAwayDefenceStrength());
	}

	@Test
	void shrinkageMovesStrengthTowardOneVersusRaw() {
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		new RegularizedDixonColesFootballProbabilityModel(MIN1_SHRINK5_NO_DC, recorder)
				.predict(strongHomeHistory(), target("H", "A"));
		DixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.rawHomeAttackStrength()).isGreaterThan(snapshot.homeAttackStrength());
		assertThat(snapshot.homeAttackStrength()).isGreaterThan(BigDecimal.ONE);
		assertThat(snapshot.homeAttackStrength().subtract(BigDecimal.ONE).abs())
				.isLessThan(snapshot.rawHomeAttackStrength().subtract(BigDecimal.ONE).abs());
	}

	@Test
	void v1ModelRemainsUnchangedByV2Presence() {
		List<ProbabilityTrainingMatch> training = averageLeagueHistory();
		ProbabilityPrediction v1 = new PoissonFootballProbabilityModel(new ProbabilityModelConfig(180, 10, 1))
				.predict(training, target("H", "A"));
		assertThat(v1.homeExpectedGoals()).isCloseTo(new BigDecimal("1.5"), within(new BigDecimal("0.0000001")));
		assertThat(v1.awayExpectedGoals()).isCloseTo(new BigDecimal("1.0"), within(new BigDecimal("0.0000001")));
	}

	@Test
	void fittedRhoDoesNotUseFutureOrSameDateScores() {
		DixonColesFitRecorder recorder = new DixonColesFitRecorder();
		RegularizedDixonColesFootballProbabilityModel model = new RegularizedDixonColesFootballProbabilityModel(
				new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("5"), true), recorder);
		List<ProbabilityTrainingMatch> history = drawHeavyHistory();
		model.predict(
				concat(
						concat(history, match(PL, "X", "Y", TARGET_DATE, 0, 0)),
						match(PL, "X", "Y", TARGET_DATE.plusDays(1), 0, 0)),
				target("H", "A"));
		DixonColesFitRecorder baseline = new DixonColesFitRecorder();
		new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, new BigDecimal("5"), true), baseline)
				.predict(history, target("H", "A"));
		assertThat(recorder.snapshots().getFirst().rho()).isEqualByComparingTo(baseline.snapshots().getFirst().rho());
		assertThat(recorder.snapshots().getFirst().rhoFitObservations())
				.isEqualTo(baseline.snapshots().getFirst().rhoFitObservations());
	}

	private static BigDecimal cell(ProbabilityPrediction prediction, int home, int away) {
		for (ScoreProbability entry : prediction.scoreDistribution().entries()) {
			if (entry.score().homeGoals() == home && entry.score().awayGoals() == away) {
				return entry.probability();
			}
		}
		throw new AssertionError("missing cell");
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

	private static List<ProbabilityTrainingMatch> strongHomeHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>(averageTeamsInLargeLeague());
		for (int i = 0; i < 6; i++) {
			matches.add(match(PL, "H", "Z" + i, TARGET_DATE.minusDays(3L + i), 5, 0));
		}
		for (int i = 0; i < 12; i++) {
			matches.add(match(PL, "Q" + i, "A", TARGET_DATE.minusDays(20L + i), i % 2 == 0 ? 2 : 1, 1));
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> drawHeavyHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>(averageTeamsInLargeLeague());
		for (int i = 0; i < 12; i++) {
			matches.add(match(PL, "H", "A", TARGET_DATE.minusDays(4L + i), 0, 0));
		}
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
