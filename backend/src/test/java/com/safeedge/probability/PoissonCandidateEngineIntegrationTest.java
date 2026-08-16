package com.safeedge.probability;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.candidate.CandidateContext;
import com.safeedge.candidate.CandidateEngine;
import com.safeedge.candidate.CandidateEvaluation;
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

class PoissonCandidateEngineIntegrationTest {

	@Test
	void poissonDistributionFeedsCandidateEngineAsianHandicap() {
		PoissonFootballProbabilityModel model =
				new PoissonFootballProbabilityModel(new ProbabilityModelConfig(180, 10, 1));
		List<ProbabilityTrainingMatch> training = new ArrayList<>();
		LocalDate targetDate = LocalDate.of(2024, 6, 1);
		LocalDate historyDate = targetDate.minusDays(10);
		for (int i = 0; i < 6; i++) {
			training.add(new ProbabilityTrainingMatch(
					CanonicalCompetition.PREMIER_LEAGUE,
					historyDate,
					"H",
					"A",
					new MatchScore(i % 2 == 0 ? 2 : 1, 1)));
		}
		ProbabilityPrediction prediction = model.predict(
				training, new MatchPredictionContext(CanonicalCompetition.PREMIER_LEAGUE, "H", "A", targetDate));
		assertThat(prediction.available()).isTrue();
		BigDecimal odds = new BigDecimal("1.90");
		BigDecimal line = new BigDecimal("-0.25");
		BettingSelection home = new BettingSelection("TEST", 1, 1, "home", SelectionType.HOME, line, odds);
		BettingSelection away = new BettingSelection("TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), odds);
		BettingMarket market = new BettingMarket(
				"TEST",
				"ah",
				null,
				"asian",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				line,
				List.of(home, away));
		CandidateEvaluation evaluation = new CandidateEngine()
				.evaluate(
						market,
						home,
						odds,
						prediction.scoreDistribution(),
						new CandidateContext("opp-1", "event-1", "PREMIER_LEAGUE", targetDate));
		assertThat(evaluation.opportunity()).isNotNull();
		assertThat(evaluation.expectedReturnRate()).isNotNull();
		assertThat(evaluation.settlementProbabilityDistribution()).isNotNull();
	}
}
