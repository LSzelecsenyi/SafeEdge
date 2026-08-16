package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.safeedge.candidate.CandidateContext;
import com.safeedge.candidate.CandidateEngine;
import com.safeedge.candidate.CandidateEvaluation;
import com.safeedge.candidate.ScoreProbability;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class JointDixonColesFootballProbabilityModelTest {

	private static final CanonicalCompetition PL = CanonicalCompetition.PREMIER_LEAGUE;
	private static final CanonicalCompetition BL = CanonicalCompetition.BUNDESLIGA;
	private static final LocalDate TARGET = LocalDate.of(2024, 6, 1);

	@Test
	void futureResultDoesNotChangeEarlierPrediction() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		ProbabilityPrediction without = model.predict(history, target("H", "A"));
		ProbabilityPrediction withFuture = model.predict(
				concat(history, match(PL, "H", "A", TARGET.plusDays(20), 8, 0)), target("H", "A"));
		assertThat(withFuture).isEqualTo(without);
	}

	@Test
	void targetResultDoesNotChangeItsOwnPrediction() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		assertThat(model.predict(concat(history, match(PL, "H", "A", TARGET, 0, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
		assertThat(model.predict(concat(history, match(PL, "H", "A", TARGET, 7, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void sameDateMatchesDoNotTrainEachOther() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		assertThat(model.predict(concat(history, match(PL, "X", "Y", TARGET, 8, 0)), target("H", "A")))
				.isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void otherCompetitionDoesNotCreateLeagueHistory() {
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		ProbabilityPrediction prediction =
				model.predict(List.of(match(BL, "H", "A", TARGET.minusDays(7), 2, 1)), target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.NO_LEAGUE_HISTORY);
	}

	@Test
	void repeatedFitIsDeterministic() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		assertThat(model.predict(history, target("H", "A"))).isEqualTo(model.predict(history, target("H", "A")));
	}

	@Test
	void strongAttackTeamHasHigherAttackParameter() {
		JointDixonColesFitRecorder recorder = new JointDixonColesFitRecorder();
		new JointDixonColesFootballProbabilityModel(ProbabilityModelV3Config.defaults(), recorder)
				.predict(attackContrastHistory(), target("S", "W"));
		JointDixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.homeAttack()).isGreaterThan(snapshot.awayAttack());
	}

	@Test
	void strongerDefenceTeamHasHigherDefenceParameter() {
		JointDixonColesFitRecorder recorder = new JointDixonColesFitRecorder();
		new JointDixonColesFootballProbabilityModel(ProbabilityModelV3Config.defaults(), recorder)
				.predict(defenceContrastHistory(), target("D", "L"));
		JointDixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.homeDefence()).isGreaterThan(snapshot.awayDefence());
	}

	@Test
	void homeAdvantageIsPositiveWhenHomesScoreMore() {
		JointDixonColesFitRecorder recorder = new JointDixonColesFitRecorder();
		new JointDixonColesFootballProbabilityModel(ProbabilityModelV3Config.defaults(), recorder)
				.predict(homeAdvantageHistory(), target("H", "A"));
		assertThat(recorder.snapshots().getFirst().homeAdvantage()).isPositive();
	}

	@Test
	void identifiabilityConstraintHolds() {
		JointDixonColesFitRecorder recorder = new JointDixonColesFitRecorder();
		ProbabilityPrediction prediction = new JointDixonColesFootballProbabilityModel(
						ProbabilityModelV3Config.defaults(), recorder)
				.predict(leagueHistory(), target("H", "A"));
		assertThat(prediction.available()).isTrue();
		JointDixonColesFitSnapshot snapshot = recorder.snapshots().getFirst();
		assertThat(snapshot.homeAttack()).isNotNull();
		assertThat(snapshot.homeDefence()).isNotNull();
	}

	@Test
	void largeRegularizationShrinksLowSampleExtremes() {
		List<ProbabilityTrainingMatch> history = attackContrastHistory();
		JointDixonColesFitRecorder raw = new JointDixonColesFitRecorder();
		JointDixonColesFitRecorder shrunk = new JointDixonColesFitRecorder();
		new JointDixonColesFootballProbabilityModel(configWithReg(0.0d), raw).predict(history, target("S", "W"));
		new JointDixonColesFootballProbabilityModel(configWithReg(50.0d), shrunk).predict(history, target("S", "W"));
		assertThat(shrunk.snapshots().getFirst().homeAttack().abs())
				.isLessThan(raw.snapshots().getFirst().homeAttack().abs());
	}

	@Test
	void fitterApiHasNoOddsEdgeOrRoi() {
		assertThat(JointDixonColesFitter.class.getDeclaredMethods())
				.extracting(method -> method.getName() + java.util.Arrays.toString(method.getParameterTypes()))
				.noneMatch(signature -> signature.toLowerCase().contains("odds")
						|| signature.toLowerCase().contains("roi")
						|| signature.toLowerCase().contains("handicap"));
	}

	@Test
	void distributionIsNonNegativeAndSumsToOne() {
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(leagueHistory(), target("H", "A"));
		assertThat(prediction.available()).isTrue();
		BigDecimal sum = BigDecimal.ZERO;
		for (ScoreProbability entry : prediction.scoreDistribution().entries()) {
			assertThat(entry.probability()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
			sum = sum.add(entry.probability());
		}
		assertThat(sum).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void candidateEngineAcceptsV3Distribution() {
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(leagueHistory(), target("H", "A"));
		BigDecimal odds = new BigDecimal("1.90");
		BigDecimal line = new BigDecimal("-0.25");
		BettingSelection home = new BettingSelection("TEST", 1, 1, "home", SelectionType.HOME, line, odds);
		BettingSelection away = new BettingSelection("TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), odds);
		BettingMarket market = new BettingMarket(
				"TEST", "ah", null, "asian", null, null, null, MarketType.ASIAN_HANDICAP, line, List.of(home, away));
		CandidateEvaluation evaluation = new CandidateEngine()
				.evaluate(
						market,
						home,
						odds,
						prediction.scoreDistribution(),
						new CandidateContext("opp-1", "event-1", "PREMIER_LEAGUE", TARGET));
		assertThat(evaluation.opportunity()).isNotNull();
		assertThat(evaluation.expectedReturnRate()).isNotNull();
	}

	@Test
	void insufficientHistoryIsPreserved() {
		List<ProbabilityTrainingMatch> training = new ArrayList<>();
		for (int i = 1; i <= 4; i++) {
			training.add(match(PL, "H", "X" + i, TARGET.minusDays(i * 7L), 1, 0));
		}
		for (int i = 1; i <= 20; i++) {
			training.add(match(PL, "Y" + (i % 5), "Z" + (i % 4), TARGET.minusDays(i), 1, 1));
		}
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(training, target("H", "A"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(prediction.scoreDistribution()).isNull();
	}

	@Test
	void warmStartDoesNotLeakFutureMatches() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		JointDixonColesFootballProbabilityModel model = new JointDixonColesFootballProbabilityModel();
		ProbabilityPrediction later = model.predict(
				concat(history, match(PL, "H", "A", TARGET.plusDays(7), 5, 0)),
				new MatchPredictionContext(PL, "H", "A", TARGET.plusDays(8)));
		assertThat(later.available()).isTrue();
		ProbabilityPrediction earlier = model.predict(history, target("H", "A"));
		JointDixonColesFootballProbabilityModel fresh = new JointDixonColesFootballProbabilityModel();
		assertThat(earlier).isEqualTo(fresh.predict(history, target("H", "A")));
	}

	@Test
	void v1AndV2RemainAvailableAndUnchanged() {
		List<ProbabilityTrainingMatch> history = averageSix();
		ProbabilityPrediction v1 = new PoissonFootballProbabilityModel(new ProbabilityModelConfig(180, 10, 1))
				.predict(history, target("H", "A"));
		ProbabilityPrediction v2 = new RegularizedDixonColesFootballProbabilityModel(
						new ProbabilityModelV2Config(180, 10, 1, BigDecimal.ZERO, false))
				.predict(history, target("H", "A"));
		assertThat(v1.homeExpectedGoals()).isCloseTo(new BigDecimal("1.5"), within(new BigDecimal("0.0000001")));
		assertThat(v2.homeExpectedGoals()).isEqualByComparingTo(v1.homeExpectedGoals());
	}

	@Test
	void lambdaIsFiniteAndPositive() {
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(leagueHistory(), target("H", "A"));
		assertThat(prediction.homeExpectedGoals()).isPositive();
		assertThat(prediction.awayExpectedGoals()).isPositive();
		assertThat(Double.isFinite(prediction.homeExpectedGoals().doubleValue())).isTrue();
	}

	@Test
	void probabilitiesStayInUnitInterval() {
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(leagueHistory(), target("H", "A"));
		for (ScoreProbability entry : prediction.scoreDistribution().entries()) {
			assertThat(entry.probability()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
		}
	}

	@Test
	void unseenTeamIsInsufficientHistoryNotLeagueAverageSubstitute() {
		List<ProbabilityTrainingMatch> history = leagueHistory();
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(history, target("H", "UNSEEN"));
		assertThat(prediction.status()).isEqualTo(ProbabilityPredictionStatus.INSUFFICIENT_HISTORY);
		assertThat(prediction.available()).isFalse();
		assertThat(prediction.scoreDistribution()).isNull();
	}

	@Test
	void changingOddsOrLineDoesNotChangeTheFittedDistribution() {
		ProbabilityPrediction prediction =
				new JointDixonColesFootballProbabilityModel().predict(leagueHistory(), target("H", "A"));
		CandidateEngine engine = new CandidateEngine();
		CandidateEvaluation cheap = evaluate(engine, prediction, new BigDecimal("1.50"), new BigDecimal("-0.25"));
		CandidateEvaluation expensive = evaluate(engine, prediction, new BigDecimal("3.20"), new BigDecimal("1.00"));
		assertThat(cheap.expectedReturnRate()).isNotEqualByComparingTo(expensive.expectedReturnRate());
		assertThat(prediction.scoreDistribution().entries()).isNotEmpty();
	}

	private static CandidateEvaluation evaluate(
			CandidateEngine engine, ProbabilityPrediction prediction, BigDecimal odds, BigDecimal line) {
		BettingSelection home = new BettingSelection("TEST", 1, 1, "home", SelectionType.HOME, line, odds);
		BettingSelection away = new BettingSelection("TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), odds);
		BettingMarket market = new BettingMarket(
				"TEST", "ah", null, "asian", null, null, null, MarketType.ASIAN_HANDICAP, line, List.of(home, away));
		return engine.evaluate(
				market,
				home,
				odds,
				prediction.scoreDistribution(),
				new CandidateContext("opp-1", "event-1", "PREMIER_LEAGUE", TARGET));
	}

	@Test
	void centeredParametersHaveNearZeroMeanInSnapshotPair() {
		JointDixonColesFitRecorder recorder = new JointDixonColesFitRecorder();
		new JointDixonColesFootballProbabilityModel(ProbabilityModelV3Config.defaults(), recorder)
				.predict(leagueHistory(), target("H", "A"));
		assertThat(recorder.snapshots()).isNotEmpty();
		assertThat(recorder.fittingFailures()).isZero();
	}

	private static ProbabilityModelV3Config configWithReg(double regularization) {
		return new ProbabilityModelV3Config(180, 10, 5, 20, regularization, regularization, 80, 1e-5d, 0.4d);
	}

	private static List<ProbabilityTrainingMatch> leagueHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>();
		String[] teams = {"H", "A", "B", "C", "D", "E"};
		int day = 1;
		for (int round = 0; round < 8; round++) {
			for (int i = 0; i < teams.length; i += 2) {
				int homeGoals = (round + i) % 3;
				int awayGoals = (round + i + 1) % 2;
				matches.add(match(PL, teams[i], teams[i + 1], TARGET.minusDays(day++), homeGoals, awayGoals));
			}
			for (int i = 0; i < teams.length; i += 2) {
				matches.add(match(PL, teams[i + 1], teams[i], TARGET.minusDays(day++), 1, 1));
			}
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> attackContrastHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>(leagueHistory());
		for (int i = 0; i < 8; i++) {
			matches.add(match(PL, "S", "W", TARGET.minusDays(3L + i), 4, 0));
			matches.add(match(PL, "W", "S", TARGET.minusDays(40L + i), 0, 3));
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> defenceContrastHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>(leagueHistory());
		for (int i = 0; i < 8; i++) {
			matches.add(match(PL, "D", "L", TARGET.minusDays(3L + i), 1, 0));
			matches.add(match(PL, "L", "D", TARGET.minusDays(40L + i), 0, 1));
			matches.add(match(PL, "B", "D", TARGET.minusDays(60L + i), 0, 1));
			matches.add(match(PL, "L", "C", TARGET.minusDays(80L + i), 0, 3));
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> homeAdvantageHistory() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>();
		String[] teams = {"H", "A", "B", "C", "D", "E"};
		int day = 1;
		for (int round = 0; round < 8; round++) {
			for (int i = 0; i < teams.length; i += 2) {
				matches.add(match(PL, teams[i], teams[i + 1], TARGET.minusDays(day++), 2, 1));
			}
		}
		return matches;
	}

	private static List<ProbabilityTrainingMatch> averageSix() {
		List<ProbabilityTrainingMatch> matches = new ArrayList<>();
		LocalDate historyDate = TARGET.minusDays(10);
		for (int i = 0; i < 6; i++) {
			matches.add(match(PL, "H", "A", historyDate, i % 2 == 0 ? 2 : 1, 1));
		}
		return matches;
	}

	private static ProbabilityTrainingMatch match(
			CanonicalCompetition competition, String home, String away, LocalDate date, int homeGoals, int awayGoals) {
		return new ProbabilityTrainingMatch(competition, date, home, away, new MatchScore(homeGoals, awayGoals));
	}

	private static MatchPredictionContext target(String home, String away) {
		return new MatchPredictionContext(PL, home, away, TARGET);
	}

	private static List<ProbabilityTrainingMatch> concat(
			List<ProbabilityTrainingMatch> history, ProbabilityTrainingMatch extra) {
		List<ProbabilityTrainingMatch> all = new ArrayList<>(history);
		all.add(extra);
		return all;
	}
}
