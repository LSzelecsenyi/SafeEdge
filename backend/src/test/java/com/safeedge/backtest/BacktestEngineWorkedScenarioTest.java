package com.safeedge.backtest;

import static com.safeedge.backtest.BacktestFixtures.OWNER;
import static com.safeedge.backtest.BacktestFixtures.STARTING;
import static com.safeedge.backtest.BacktestFixtures.T10;
import static com.safeedge.backtest.BacktestFixtures.T1030;
import static com.safeedge.backtest.BacktestFixtures.T12;
import static com.safeedge.backtest.BacktestFixtures.T1205;
import static com.safeedge.backtest.BacktestFixtures.T13;
import static com.safeedge.backtest.BacktestFixtures.T14;
import static com.safeedge.backtest.BacktestFixtures.homeZero;
import static com.safeedge.backtest.BacktestFixtures.leagueCapConfig;
import static com.safeedge.backtest.BacktestFixtures.loss;
import static com.safeedge.backtest.BacktestFixtures.money;
import static com.safeedge.backtest.BacktestFixtures.request;
import static com.safeedge.backtest.BacktestFixtures.win;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.bankroll.BankrollState;
import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.PortfolioExposure;
import com.safeedge.strategy.StrategyDecision;
import com.safeedge.strategy.StrategyDecisionReason;
import com.safeedge.strategy.StrategyEngine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class BacktestEngineWorkedScenarioTest {

	private final BacktestEngine engine = new BacktestEngine();
	private final StrategyEngine strategyEngine = new StrategyEngine();
	private final PayoutCalculator payoutCalculator = new PayoutCalculator();

	@Test
	void chronologicalLeagueExposureAndSettlementUpdateBankrollForLaterDecisions() {
		var config = leagueCapConfig();
		HistoricalBettingOpportunity a = homeZero("A", "event-a", "league-1", T10, "2.00", "0.05");
		HistoricalBettingOpportunity b = homeZero("B", "event-b", "league-1", T1030, "2.00", "0.05");
		HistoricalBettingOpportunity c = homeZero("C", "event-c", "league-1", T1205, "2.00", "0.05");
		BankrollState initial = BankrollState.initial(OWNER, STARTING);

		StrategyDecision decisionA = strategyEngine.decide(
				config, a.opportunity(), initial, PortfolioExposure.none());
		StrategyDecision decisionB = strategyEngine.decide(
				config,
				b.opportunity(),
				initial,
				new PortfolioExposure(BigDecimal.ZERO, money("2000"), money("2000")));

		BacktestResult result = engine.run(request(
				config,
				List.of(a, b, c),
				List.of(win("event-a", T12), loss("event-b", T13), win("event-c", T14))));

		assertThat(decisionA.stake()).isEqualByComparingTo("2000");
		assertThat(decisionB.stake()).isEqualByComparingTo("1000");
		assertThat(decisionB.reasons()).contains(StrategyDecisionReason.LEAGUE_EXPOSURE_CAPPED);

		BacktestBetResult betA = result.acceptedBetResults().stream()
				.filter(bet -> bet.opportunityId().equals("A"))
				.findFirst()
				.orElseThrow();
		assertThat(betA.stake()).isEqualByComparingTo(decisionA.stake());
		assertThat(betA.settlementResult()).isEqualTo(SettlementResult.WIN);
		assertThat(betA.profit()).isEqualByComparingTo(
				payoutCalculator.calculate(SettlementResult.WIN, money("2.00"), betA.stake()).profit());
		assertThat(betA.activeBankrollAfterSettlement()).isEqualByComparingTo("102000");
		assertThat(betA.vaultBalanceAfterSettlement()).isEqualByComparingTo("0");

		BacktestBetResult betB = result.acceptedBetResults().stream()
				.filter(bet -> bet.opportunityId().equals("B"))
				.findFirst()
				.orElseThrow();
		assertThat(betB.stake()).isEqualByComparingTo(decisionB.stake());
		assertThat(betB.settlementResult()).isEqualTo(SettlementResult.LOSS);
		assertThat(betB.profit()).isEqualByComparingTo("-1000");
		assertThat(betB.activeBankrollAfterSettlement()).isEqualByComparingTo("101000");

		BankrollState afterA = BankrollState.initial(OWNER, STARTING);
		afterA = new BankrollState(
				afterA.ownerId(),
				money("102000"),
				BigDecimal.ZERO,
				money("2000"),
				money("2000"),
				money("102000"),
				money("102000"));
		StrategyDecision decisionC = strategyEngine.decide(
				config,
				c.opportunity(),
				afterA,
				new PortfolioExposure(BigDecimal.ZERO, money("1000"), money("3000")));
		BacktestBetResult betC = result.acceptedBetResults().stream()
				.filter(bet -> bet.opportunityId().equals("C"))
				.findFirst()
				.orElseThrow();
		assertThat(betC.opportunityId()).isEqualTo("C");
		assertThat(betC.stake()).isEqualByComparingTo("2040");
		assertThat(betC.stake()).isEqualByComparingTo(decisionC.stake());
		assertThat(betC.settlementResult()).isEqualTo(SettlementResult.WIN);
		assertThat(betC.profit()).isEqualByComparingTo("2040");
		assertThat(betC.activeBankrollAfterSettlement()).isEqualByComparingTo("103040");

		assertThat(result.counts().betsAccepted()).isEqualTo(3);
		assertThat(result.counts().wins()).isEqualTo(2);
		assertThat(result.counts().losses()).isEqualTo(1);
		assertThat(result.finalActiveBankroll()).isEqualByComparingTo("103040");
		assertThat(result.finalTotalEquity()).isEqualByComparingTo("103040");
		assertThat(result.metrics().totalStake()).isEqualByComparingTo("5040");
		assertThat(result.metrics().totalProfit()).isEqualByComparingTo("3040");
		assertThat(result.metrics().totalProfit())
				.isEqualByComparingTo(result.finalTotalEquity().subtract(result.startingBankroll()));
		assertThat(result.equityCurve()).hasSize(4);
		assertThat(result.pausedByDrawdown()).isFalse();
	}

}
