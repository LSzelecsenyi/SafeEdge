package com.safeedge.candidate;

import static com.safeedge.candidate.CandidateFixtures.CONTEXT;
import static com.safeedge.candidate.CandidateFixtures.ODDS_125;
import static com.safeedge.candidate.CandidateFixtures.ODDS_200;
import static com.safeedge.candidate.CandidateFixtures.asianHomeLine;
import static com.safeedge.candidate.CandidateFixtures.binaryHomeWin;
import static com.safeedge.candidate.CandidateFixtures.selectionOf;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.bankroll.BankrollState;
import com.safeedge.bankroll.OwnerId;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.strategy.PortfolioExposure;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import com.safeedge.strategy.StrategyDecision;
import com.safeedge.strategy.StrategyDecisionReason;
import com.safeedge.strategy.StrategyDecisionStatus;
import com.safeedge.strategy.StrategyEngine;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CandidateEngineStrategyIntegrationTest {

	private static final OwnerId OWNER = new OwnerId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));

	private final CandidateEngine candidateEngine = new CandidateEngine();
	private final StrategyEngine strategyEngine = new StrategyEngine();

	@Test
	void strategyEngineConsumesCandidateOpportunityWithoutRewritingValue() {
		BettingMarket market = asianHomeLine("0", ODDS_125);
		CandidateEvaluation evaluation = candidateEngine.evaluate(
				market,
				selectionOf(market, SelectionType.HOME),
				ODDS_125,
				binaryHomeWin("0.90"),
				CONTEXT);
		assertThat(evaluation.opportunity().edge()).isEqualByComparingTo(evaluation.expectedReturnRate());
		assertThat(evaluation.expectedReturnRate()).isEqualByComparingTo("0.125");
		StrategyDecision decision = strategyEngine.decide(
				flatTwoPercent("0.03"),
				evaluation.opportunity(),
				BankrollState.initial(OWNER, new BigDecimal("100000")),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(decision.expectedReturnRate()).isEqualByComparingTo(evaluation.expectedReturnRate());
		assertThat(decision.stake()).isEqualByComparingTo("2000");
	}

	@Test
	void fractionalKellyRejectsZeroEvThroughExistingGuardrail() {
		BettingMarket market = asianHomeLine("0", ODDS_200);
		CandidateEvaluation evaluation = candidateEngine.evaluate(
				market,
				selectionOf(market, SelectionType.HOME),
				ODDS_200,
				binaryHomeWin("0.50"),
				CONTEXT);
		assertThat(evaluation.status()).isEqualTo(CandidateValueStatus.ZERO_EV);
		StrategyDecision decision = strategyEngine.decide(
				kellyWithMinimumEdge("0"),
				evaluation.opportunity(),
				BankrollState.initial(OWNER, new BigDecimal("100000")),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.NON_POSITIVE_EXPECTED_RETURN);
	}

	@Test
	void flatStakeAppliesMinimumEdgeWithoutKelly() {
		BettingMarket market = asianHomeLine("0", ODDS_200);
		CandidateEvaluation belowMinimum = candidateEngine.evaluate(
				market,
				selectionOf(market, SelectionType.HOME),
				ODDS_200,
				binaryHomeWin("0.51"),
				CONTEXT);
		assertThat(belowMinimum.expectedReturnRate()).isEqualByComparingTo("0.02");
		assertThat(belowMinimum.status()).isEqualTo(CandidateValueStatus.POSITIVE_EV);
		StrategyDecision rejected = strategyEngine.decide(
				flatTwoPercent("0.03"),
				belowMinimum.opportunity(),
				BankrollState.initial(OWNER, new BigDecimal("100000")),
				PortfolioExposure.none());
		assertThat(rejected.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(rejected.reasons()).containsExactly(StrategyDecisionReason.EDGE_BELOW_MINIMUM);
		assertThat(rejected.fullKellyFraction()).isNull();

		CandidateEvaluation aboveMinimum = candidateEngine.evaluate(
				market,
				selectionOf(market, SelectionType.HOME),
				ODDS_200,
				binaryHomeWin("0.60"),
				CONTEXT);
		assertThat(aboveMinimum.expectedReturnRate()).isGreaterThan(new BigDecimal("0.03"));
		StrategyDecision accepted = strategyEngine.decide(
				flatTwoPercent("0.03"),
				aboveMinimum.opportunity(),
				BankrollState.initial(OWNER, new BigDecimal("100000")),
				PortfolioExposure.none());
		assertThat(accepted.status()).isEqualTo(StrategyDecisionStatus.ACCEPTED);
		assertThat(accepted.fullKellyFraction()).isNull();
	}

	@Test
	void negativeEvFailsMinimumEdgeBeforeKellyWhenThresholdIsPositive() {
		BettingMarket market = asianHomeLine("0", ODDS_200);
		CandidateEvaluation evaluation = candidateEngine.evaluate(
				market,
				selectionOf(market, SelectionType.HOME),
				ODDS_200,
				binaryHomeWin("0.40"),
				CONTEXT);
		assertThat(evaluation.status()).isEqualTo(CandidateValueStatus.NEGATIVE_EV);
		StrategyDecision decision = strategyEngine.decide(
				kellyWithMinimumEdge("0.03"),
				evaluation.opportunity(),
				BankrollState.initial(OWNER, new BigDecimal("100000")),
				PortfolioExposure.none());
		assertThat(decision.status()).isEqualTo(StrategyDecisionStatus.REJECTED);
		assertThat(decision.reasons()).containsExactly(StrategyDecisionReason.EDGE_BELOW_MINIMUM);
	}

	private static StrategyConfig flatTwoPercent(String minimumEdge) {
		return new StrategyConfig(
				false,
				BigDecimal.ZERO,
				StakingMode.FLAT_STAKE,
				null,
				new BigDecimal("0.02"),
				new BigDecimal("0.02"),
				new BigDecimal(minimumEdge),
				new BigDecimal("0.03"),
				new BigDecimal("0.05"),
				new BigDecimal("0.10"),
				new BigDecimal("0.10"),
				new BigDecimal("0.15"),
				new BigDecimal("0.50"),
				new BigDecimal("0.20"));
	}

	private static StrategyConfig kellyWithMinimumEdge(String minimumEdge) {
		return new StrategyConfig(
				false,
				BigDecimal.ZERO,
				StakingMode.FRACTIONAL_KELLY,
				new BigDecimal("0.25"),
				null,
				new BigDecimal("0.02"),
				new BigDecimal(minimumEdge),
				new BigDecimal("0.03"),
				new BigDecimal("0.05"),
				new BigDecimal("0.10"),
				new BigDecimal("0.10"),
				new BigDecimal("0.15"),
				new BigDecimal("0.50"),
				new BigDecimal("0.20"));
	}

}
