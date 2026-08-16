package com.safeedge.candidate;

import static com.safeedge.candidate.CandidateFixtures.CONTEXT;
import static com.safeedge.candidate.CandidateFixtures.ODDS_125;
import static com.safeedge.candidate.CandidateFixtures.ODDS_200;
import static com.safeedge.candidate.CandidateFixtures.asianHomeLine;
import static com.safeedge.candidate.CandidateFixtures.awayPlusOneShape;
import static com.safeedge.candidate.CandidateFixtures.binaryHomeWin;
import static com.safeedge.candidate.CandidateFixtures.doubleChance;
import static com.safeedge.candidate.CandidateFixtures.european;
import static com.safeedge.candidate.CandidateFixtures.score;
import static com.safeedge.candidate.CandidateFixtures.selectionOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CandidateEngineTest {

	private final CandidateEngine engine = new CandidateEngine();

	@Nested
	class AsianHandicapDistributions {

		@Test
		void awayPlusOneProducesWinPushLossWithoutHalves() {
			BettingMarket market = asianHomeLine("-1", ODDS_200);
			BettingSelection away = selectionOf(market, SelectionType.AWAY);
			CandidateEvaluation evaluation = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			assertDistribution(evaluation, "0.65", "0", "0.20", "0", "0.15");
		}

		@Test
		void awayPlusOneQuarterProducesHalfWinOnOneGoalDefeat() {
			BettingMarket market = asianHomeLine("-1.25", ODDS_200);
			BettingSelection away = selectionOf(market, SelectionType.AWAY);
			CandidateEvaluation evaluation = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			assertDistribution(evaluation, "0.65", "0.20", "0", "0", "0.15");
		}

		@Test
		void awayPlusThreeQuartersProducesHalfLossOnOneGoalDefeat() {
			BettingMarket market = asianHomeLine("-0.75", ODDS_200);
			BettingSelection away = selectionOf(market, SelectionType.AWAY);
			CandidateEvaluation evaluation = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			assertDistribution(evaluation, "0.65", "0", "0", "0.20", "0.15");
		}
	}

	@Nested
	class OtherMarkets {

		@Test
		void europeanHandicapHasNoHalfOrPushMass() {
			BettingMarket market = european("0", ODDS_200);
			BettingSelection home = selectionOf(market, SelectionType.HOME);
			ScoreProbabilityDistribution scores = CandidateFixtures.distribution(
					score(1, 0, "0.40"),
					score(0, 0, "0.30"),
					score(0, 1, "0.30"));
			CandidateEvaluation evaluation = engine.evaluate(market, home, ODDS_200, scores, CONTEXT);
			assertDistribution(evaluation, "0.40", "0", "0", "0", "0.60");
		}

		@Test
		void doubleChanceUsesSettlementEngineWinLossOnly() {
			BettingMarket market = doubleChance(ODDS_200);
			BettingSelection homeOrDraw = selectionOf(market, SelectionType.HOME_OR_DRAW);
			ScoreProbabilityDistribution scores = CandidateFixtures.distribution(
					score(1, 0, "0.40"),
					score(0, 0, "0.30"),
					score(0, 1, "0.30"));
			CandidateEvaluation evaluation = engine.evaluate(market, homeOrDraw, ODDS_200, scores, CONTEXT);
			assertDistribution(evaluation, "0.70", "0", "0", "0", "0.30");
		}
	}

	@Nested
	class ExpectedReturn {

		@Test
		void positiveExpectedReturnMatchesCanonicalNetProfit() {
			BettingMarket market = asianHomeLine("0", ODDS_125);
			CandidateEvaluation evaluation = engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					ODDS_125,
					binaryHomeWin("0.90"),
					CONTEXT);
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("0.125");
			assertThat(evaluation.opportunity().edge()).isEqualByComparingTo("0.125");
			assertThat(evaluation.status()).isEqualTo(CandidateValueStatus.POSITIVE_EV);
		}

		@Test
		void zeroExpectedReturnIsNotPositive() {
			BettingMarket market = asianHomeLine("0", ODDS_200);
			CandidateEvaluation evaluation = engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					ODDS_200,
					binaryHomeWin("0.50"),
					CONTEXT);
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("0");
			assertThat(evaluation.status()).isEqualTo(CandidateValueStatus.ZERO_EV);
		}

		@Test
		void negativeExpectedReturnIsClassifiedWithoutStrategyGates() {
			BettingMarket market = asianHomeLine("0", ODDS_200);
			CandidateEvaluation evaluation = engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					ODDS_200,
					binaryHomeWin("0.40"),
					CONTEXT);
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("-0.20");
			assertThat(evaluation.status()).isEqualTo(CandidateValueStatus.NEGATIVE_EV);
		}

		@Test
		void pushContributesZeroNotALoss() {
			BettingMarket market = asianHomeLine("0", ODDS_125);
			ScoreProbabilityDistribution scores = CandidateFixtures.distribution(
					score(1, 0, "0.70"),
					score(0, 0, "0.20"),
					score(0, 1, "0.10"));
			CandidateEvaluation evaluation = engine.evaluate(
					market, selectionOf(market, SelectionType.HOME), ODDS_125, scores, CONTEXT);
			assertDistribution(evaluation, "0.70", "0", "0.20", "0", "0.10");
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("0.075");
		}

		@Test
		void halfWinUsesHalfOfWinNetReturn() {
			BettingMarket market = asianHomeLine("-1.25", ODDS_125);
			ScoreProbabilityDistribution homeByOne = CandidateFixtures.distribution(score(1, 0, "1"));
			CandidateEvaluation evaluation = engine.evaluate(
					market, selectionOf(market, SelectionType.AWAY), ODDS_125, homeByOne, CONTEXT);
			assertThat(evaluation.settlementProbabilityDistribution().halfWinProbability())
					.isEqualByComparingTo("1");
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("0.125");
		}

		@Test
		void halfLossUsesMinusOneHalf() {
			BettingMarket market = asianHomeLine("-0.75", ODDS_125);
			ScoreProbabilityDistribution homeByOne = CandidateFixtures.distribution(score(1, 0, "1"));
			CandidateEvaluation evaluation = engine.evaluate(
					market, selectionOf(market, SelectionType.AWAY), ODDS_125, homeByOne, CONTEXT);
			assertThat(evaluation.settlementProbabilityDistribution().halfLossProbability())
					.isEqualByComparingTo("1");
			assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("-0.5");
		}
	}

	@Nested
	class OutputAndValidation {

		@Test
		void impliedProbabilityReferenceIsOneOverOddsAndNotUsedAsModel() {
			BettingMarket market = asianHomeLine("0", ODDS_125);
			CandidateEvaluation evaluation = engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					ODDS_125,
					binaryHomeWin("0.90"),
					CONTEXT);
			assertThat(evaluation.impliedProbabilityReference()).isEqualByComparingTo("0.8");
			assertThat(evaluation.opportunity().settlementProbabilities().winProbability())
					.isEqualByComparingTo("0.90");
			assertThat(evaluation.impliedProbabilityReference())
					.isNotEqualByComparingTo(evaluation.opportunity().settlementProbabilities().winProbability());
		}

		@Test
		void bettingOpportunityCarriesOddsEdgeAndSettlementDistribution() {
			BettingMarket market = asianHomeLine("0", ODDS_125);
			CandidateEvaluation evaluation = engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					ODDS_125,
					binaryHomeWin("0.90"),
					CONTEXT);
			BettingOpportunity opportunity = evaluation.opportunity();
			assertThat(opportunity.opportunityId()).isEqualTo("opp-1");
			assertThat(opportunity.eventId()).isEqualTo("event-1");
			assertThat(opportunity.leagueId()).isEqualTo("league-1");
			assertThat(opportunity.bettingDate()).isEqualTo(CONTEXT.bettingDate());
			assertThat(opportunity.odds()).isEqualByComparingTo(ODDS_125);
			assertThat(opportunity.edge()).isEqualByComparingTo(evaluation.expectedReturnRate());
			assertThat(opportunity.settlementProbabilities())
					.isEqualTo(evaluation.settlementProbabilityDistribution());
		}

		@Test
		void opportunityIsUsableAsHistoricalBettingOpportunityWithoutRecalculation() {
			BettingMarket market = asianHomeLine("-1", ODDS_200);
			BettingSelection away = selectionOf(market, SelectionType.AWAY);
			CandidateEvaluation evaluation = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			HistoricalBettingOpportunity historical = new HistoricalBettingOpportunity(
					evaluation.opportunity(),
					market,
					away,
					Instant.parse("2026-08-16T10:00:00Z"));
			assertThat(historical.opportunity()).isEqualTo(evaluation.opportunity());
			assertThat(historical.opportunity().odds()).isEqualByComparingTo(away.odds());
			assertThat(historical.opportunity().edge()).isEqualByComparingTo(evaluation.expectedReturnRate());
			assertThat(historical.opportunity().settlementProbabilities())
					.isEqualTo(evaluation.settlementProbabilityDistribution());
		}

		@Test
		void sameInputsAreDeterministic() {
			BettingMarket market = asianHomeLine("-1.25", ODDS_200);
			BettingSelection away = selectionOf(market, SelectionType.AWAY);
			CandidateEvaluation first = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			CandidateEvaluation second = engine.evaluate(market, away, ODDS_200, awayPlusOneShape(), CONTEXT);
			assertThat(first).isEqualTo(second);
		}

		@Test
		void oddsMustBeGreaterThanOne() {
			BettingMarket market = asianHomeLine("0", ODDS_200);
			assertThatThrownBy(() -> engine.evaluate(
					market,
					selectionOf(market, SelectionType.HOME),
					BigDecimal.ONE,
					binaryHomeWin("0.60"),
					CONTEXT))
					.isInstanceOf(CandidateException.class)
					.hasMessageContaining("observedOdds");
		}

		@Test
		void nullRequiredInputsAreRejected() {
			BettingMarket market = asianHomeLine("0", ODDS_200);
			BettingSelection home = selectionOf(market, SelectionType.HOME);
			assertThatThrownBy(() -> engine.evaluate(null, home, ODDS_200, binaryHomeWin("0.60"), CONTEXT))
					.isInstanceOf(CandidateException.class);
			assertThatThrownBy(() -> engine.evaluate(market, null, ODDS_200, binaryHomeWin("0.60"), CONTEXT))
					.isInstanceOf(CandidateException.class);
			assertThatThrownBy(() -> engine.evaluate(market, home, null, binaryHomeWin("0.60"), CONTEXT))
					.isInstanceOf(CandidateException.class);
			assertThatThrownBy(() -> engine.evaluate(market, home, ODDS_200, null, CONTEXT))
					.isInstanceOf(CandidateException.class);
			assertThatThrownBy(() -> engine.evaluate(market, home, ODDS_200, binaryHomeWin("0.60"), null))
					.isInstanceOf(CandidateException.class);
		}
	}

	private static void assertDistribution(
			CandidateEvaluation evaluation,
			String win,
			String halfWin,
			String push,
			String halfLoss,
			String loss) {
		SettlementProbabilityDistribution distribution = evaluation.settlementProbabilityDistribution();
		assertThat(distribution.winProbability()).isEqualByComparingTo(win);
		assertThat(distribution.halfWinProbability()).isEqualByComparingTo(halfWin);
		assertThat(distribution.pushProbability()).isEqualByComparingTo(push);
		assertThat(distribution.halfLossProbability()).isEqualByComparingTo(halfLoss);
		assertThat(distribution.lossProbability()).isEqualByComparingTo(loss);
		assertThat(evaluation.opportunity().settlementProbabilities()).isEqualTo(distribution);
	}

}
