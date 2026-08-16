package com.safeedge.backtest;

import static com.safeedge.backtest.BacktestFixtures.DAY;
import static com.safeedge.backtest.BacktestFixtures.NEXT_DAY;
import static com.safeedge.backtest.BacktestFixtures.OWNER;
import static com.safeedge.backtest.BacktestFixtures.STARTING;
import static com.safeedge.backtest.BacktestFixtures.T10;
import static com.safeedge.backtest.BacktestFixtures.T1030;
import static com.safeedge.backtest.BacktestFixtures.T11;
import static com.safeedge.backtest.BacktestFixtures.T1130;
import static com.safeedge.backtest.BacktestFixtures.T12;
import static com.safeedge.backtest.BacktestFixtures.T1201;
import static com.safeedge.backtest.BacktestFixtures.T1230;
import static com.safeedge.backtest.BacktestFixtures.T13;
import static com.safeedge.backtest.BacktestFixtures.T14;
import static com.safeedge.backtest.BacktestFixtures.T18;
import static com.safeedge.backtest.BacktestFixtures.T1830;
import static com.safeedge.backtest.BacktestFixtures.asianHome;
import static com.safeedge.backtest.BacktestFixtures.draw;
import static com.safeedge.backtest.BacktestFixtures.flatTwoPercent;
import static com.safeedge.backtest.BacktestFixtures.highCapacityFlat;
import static com.safeedge.backtest.BacktestFixtures.homeZero;
import static com.safeedge.backtest.BacktestFixtures.leagueCapConfig;
import static com.safeedge.backtest.BacktestFixtures.loss;
import static com.safeedge.backtest.BacktestFixtures.money;
import static com.safeedge.backtest.BacktestFixtures.reductionConfig;
import static com.safeedge.backtest.BacktestFixtures.request;
import static com.safeedge.backtest.BacktestFixtures.vaultOnFlat;
import static com.safeedge.backtest.BacktestFixtures.win;
import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.bankroll.BankrollAccountingEngine;
import com.safeedge.bankroll.BankrollState;
import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.PortfolioExposure;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import com.safeedge.strategy.StrategyDecision;
import com.safeedge.strategy.StrategyDecisionReason;
import com.safeedge.strategy.StrategyDecisionStatus;
import com.safeedge.strategy.StrategyEngine;
import com.safeedge.strategy.StrategyPreset;
import com.safeedge.strategy.StrategyPresetFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BacktestEngineTest {

	private final BacktestEngine engine = new BacktestEngine();
	private final StrategyEngine strategyEngine = new StrategyEngine();
	private final PayoutCalculator payoutCalculator = new PayoutCalculator();
	private final BankrollAccountingEngine accountingEngine = new BankrollAccountingEngine();

	@Nested
	class ZeroAndSingleBets {

		@Test
		void zeroOpportunitiesLeavesInitialBankrollAndZeroRoi() {
			BacktestResult result = engine.run(request(flatTwoPercent(), List.of(), List.of()));
			assertThat(result.counts().opportunitiesProcessed()).isZero();
			assertThat(result.counts().betsAccepted()).isZero();
			assertThat(result.acceptedBetResults()).isEmpty();
			assertThat(result.equityCurve()).hasSize(1);
			assertThat(result.finalActiveBankroll()).isEqualByComparingTo(STARTING);
			assertThat(result.finalVaultBalance()).isEqualByComparingTo("0");
			assertThat(result.metrics().roi()).isEqualByComparingTo("0");
			assertThat(result.metrics().averageOdds()).isEqualByComparingTo("0");
			assertThat(result.metrics().averageStake()).isEqualByComparingTo("0");
			assertThat(result.metrics().averageEdge()).isEqualByComparingTo("0");
			assertProfitInvariant(result);
		}

		@Test
		void oneAcceptedWin() {
			HistoricalBettingOpportunity opportunity = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(opportunity),
					List.of(win("e1", T18))));
			BacktestBetResult bet = result.acceptedBetResults().getFirst();
			assertThat(bet.stake()).isEqualByComparingTo("2000");
			assertThat(bet.settlementResult()).isEqualTo(SettlementResult.WIN);
			assertThat(bet.returnAmount()).isEqualByComparingTo("4000");
			assertThat(bet.profit()).isEqualByComparingTo("2000");
			assertThat(result.counts().wins()).isEqualTo(1);
			assertThat(result.finalTotalEquity()).isEqualByComparingTo("102000");
			assertThat(result.metrics().roi()).isEqualByComparingTo("1");
			assertProfitInvariant(result);
		}

		@Test
		void oneAcceptedLoss() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
					List.of(loss("e1", T18))));
			BacktestBetResult bet = result.acceptedBetResults().getFirst();
			assertThat(bet.settlementResult()).isEqualTo(SettlementResult.LOSS);
			assertThat(bet.returnAmount()).isEqualByComparingTo("0");
			assertThat(bet.profit()).isEqualByComparingTo("-2000");
			assertThat(result.counts().losses()).isEqualTo(1);
			assertThat(result.finalTotalEquity()).isEqualByComparingTo("98000");
			assertThat(result.metrics().roi()).isEqualByComparingTo("-1");
			assertProfitInvariant(result);
		}

		@Test
		void strategyEngineRejectionIsCountedSeparately() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.01")),
					List.of(win("e1", T18))));
			assertThat(result.counts().betsAccepted()).isZero();
			assertThat(result.counts().opportunitiesRejected()).isEqualTo(1);
			assertThat(result.counts().opportunitiesSkippedByBetLimit()).isZero();
			assertThat(result.counts().opportunitiesSkippedByDrawdownPause()).isZero();
			assertThat(result.rejectionReasonCounts())
					.containsEntry(StrategyDecisionReason.EDGE_BELOW_MINIMUM, 1L);
			assertThat(result.equityCurve()).hasSize(1);
			assertThat(result.finalTotalEquity()).isEqualByComparingTo(STARTING);
		}
	}

	@Nested
	class SettlementOutcomes {

		@Test
		void halfWinPushAndHalfLossAreCountedSeparately() {
			HistoricalBettingOpportunity halfWin = asianHome("hw", "e-hw", "l-hw", DAY, T10, "0.25", "2.00", "0.05");
			HistoricalBettingOpportunity push = homeZero("p", "e-p", "l-p", T1030, "2.00", "0.05");
			HistoricalBettingOpportunity halfLoss = asianHome("hl", "e-hl", "l-hl", DAY, T11, "-0.25", "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(halfWin, push, halfLoss),
					List.of(draw("e-hw", T12), draw("e-p", T13), draw("e-hl", T14))));
			assertThat(result.counts().halfWins()).isEqualTo(1);
			assertThat(result.counts().pushes()).isEqualTo(1);
			assertThat(result.counts().halfLosses()).isEqualTo(1);
			assertThat(result.acceptedBetResults())
					.extracting(BacktestBetResult::settlementResult)
					.containsExactly(SettlementResult.HALF_WIN, SettlementResult.PUSH, SettlementResult.HALF_LOSS);
			assertThat(result.acceptedBetResults().get(0).profit()).isEqualByComparingTo("1000");
			assertThat(result.acceptedBetResults().get(1).profit()).isEqualByComparingTo("0");
			assertThat(result.acceptedBetResults().get(2).profit()).isEqualByComparingTo("-1000");
			assertProfitInvariant(result);
		}

		@Test
		void multipleBetsOnTheSameEventUseTheSameResult() {
			HistoricalBettingOpportunity first = homeZero("a", "shared", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "shared", "l1", T1030, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(first, second),
					List.of(win("shared", T18))));
			assertThat(result.acceptedBetResults()).hasSize(2);
			assertThat(result.acceptedBetResults())
					.extracting(BacktestBetResult::settlementResult)
					.containsOnly(SettlementResult.WIN);
			assertThat(result.acceptedBetResults())
					.extracting(BacktestBetResult::eventId)
					.containsOnly("shared");
		}
	}

	@Nested
	class Exposure {

		@Test
		void sameMatchExposureCapsTheSecondOpenStake() {
			HistoricalBettingOpportunity first = homeZero("a", "event-x", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "event-x", "l1", T11, "2.00", "0.05");
			StrategyDecision expectedSecond = strategyEngine.decide(
					flatTwoPercent(),
					second.opportunity(),
					BankrollState.initial(OWNER, STARTING),
					new PortfolioExposure(money("2000"), money("2000"), money("2000")));
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(first, second),
					List.of(win("event-x", T18))));
			assertThat(result.acceptedBetResults().get(0).stake()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("1000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo(expectedSecond.stake());
			assertThat(result.acceptedBetResults().get(1).strategyDecisionReasons())
					.contains(StrategyDecisionReason.MATCH_EXPOSURE_CAPPED);
		}

		@Test
		void leagueExposureAggregatesOpenStakesAndReleasesAfterSettlement() {
			HistoricalBettingOpportunity first = homeZero("a", "e1", "league-a", T10, "2.00", "0.05");
			HistoricalBettingOpportunity overlapping = homeZero("b", "e2", "league-a", T1030, "2.00", "0.05");
			HistoricalBettingOpportunity afterRelease = homeZero("c", "e3", "league-a", T13, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					leagueCapConfig(),
					List.of(first, overlapping, afterRelease),
					List.of(win("e1", T12), win("e2", T14), win("e3", T18))));
			assertThat(result.acceptedBetResults().get(0).stake()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("1000");
			assertThat(result.acceptedBetResults().get(1).strategyDecisionReasons())
					.contains(StrategyDecisionReason.LEAGUE_EXPOSURE_CAPPED);
			assertThat(result.acceptedBetResults().get(2).stake()).isEqualByComparingTo("2040");
			assertThat(result.acceptedBetResults().get(2).strategyDecisionReasons())
					.doesNotContain(StrategyDecisionReason.LEAGUE_EXPOSURE_CAPPED);
		}

		@Test
		void dailyExposureIsNotReleasedWhenTheFirstBetSettlesTheSameDay() {
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity sameDay = homeZero("b", "e2", "l2", T1201, "2.00", "0.05");
			HistoricalBettingOpportunity nextDay = homeZero("c", "e3", "l3", NEXT_DAY, T1830, "2.00", "0.05");
			StrategyConfig dailyCap = BacktestFixtures.flat(
					false,
					"0",
					"0.02",
					"0.02",
					"0.03",
					"0.03",
					"0.03",
					"0.03",
					"0.10",
					"0.15",
					"0.50",
					"0.20");
			BankrollState afterFirstWin = accountingEngine.applyPayout(
					BankrollState.initial(OWNER, STARTING),
					SettlementResult.WIN,
					payoutCalculator.calculate(SettlementResult.WIN, money("2.00"), money("2000")),
					dailyCap,
					"a",
					T12).state();
			StrategyDecision expectedSameDay = strategyEngine.decide(
					dailyCap,
					sameDay.opportunity(),
					afterFirstWin,
					new PortfolioExposure(BigDecimal.ZERO, BigDecimal.ZERO, money("2000")));
			BacktestResult result = engine.run(request(
					dailyCap,
					List.of(first, sameDay, nextDay),
					List.of(win("e1", T12), win("e2", T14), win("e3", Instant.parse("2026-08-17T20:00:00Z")))));
			assertThat(result.acceptedBetResults().get(0).stake()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo(expectedSameDay.stake());
			assertThat(expectedSameDay.stake()).isLessThan(money("2040"));
			assertThat(result.acceptedBetResults().get(1).strategyDecisionReasons())
					.contains(StrategyDecisionReason.DAILY_EXPOSURE_CAPPED);
			assertThat(result.acceptedBetResults().get(2).stake()).isPositive();
			assertThat(result.acceptedBetResults().get(2).strategyDecisionReasons())
					.doesNotContain(StrategyDecisionReason.DAILY_EXPOSURE_CAPPED);
		}
	}

	@Nested
	class PointInTime {

		@Test
		void settlementAtDecisionTimeSettlesBeforeTheNewDecision() {
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "e2", "l2", T12, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(first, second),
					List.of(win("e1", T12), win("e2", T18))));
			assertThat(result.acceptedBetResults().get(0).profit()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("2040");
			assertThat(result.acceptedBetResults().get(1).activeBankrollAfterSettlement())
					.isEqualByComparingTo(result.finalActiveBankroll());
		}

		@Test
		void laterOpportunitySeesUpdatedBankrollAfterEarlierSettlement() {
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "e2", "l2", T1201, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(first, second),
					List.of(win("e1", T12), win("e2", T18))));
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("2040");
		}

		@Test
		void futureWinDoesNotChangeBankrollForAnEarlierDecision() {
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "e2", "l2", T11, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(first, second),
					List.of(win("e1", T18), win("e2", T1830))));
			assertThat(result.acceptedBetResults().get(0).stake()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("2000");
			assertThat(result.acceptedBetResults().get(1).profit()).isEqualByComparingTo("2000");
		}
	}

	@Nested
	class DrawdownAndVault {

		@Test
		void drawdownReductionReducesLaterStake() {
			StrategyConfig config = reductionConfig();
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "e2", "l2", T1201, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					config,
					List.of(first, second),
					List.of(loss("e1", T12), win("e2", T18))));
			assertThat(result.acceptedBetResults().get(0).stake()).isEqualByComparingTo("10000");
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo("4500");
			assertThat(result.acceptedBetResults().get(1).strategyDecisionReasons())
					.contains(StrategyDecisionReason.DRAWDOWN_REDUCTION_APPLIED);
		}

		@Test
		void drawdownStopLatchesEvenIfAnOpenBetLaterRecoversBankroll() {
			StrategyConfig config = highCapacityFlat("0.10", "0.10", "0.10");
			HistoricalBettingOpportunity openWinner = homeZero("a", "e-win", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity loser = homeZero("b", "e-loss", "l2", T1030, "2.00", "0.05");
			HistoricalBettingOpportunity pauseTrigger = homeZero("c", "e-pause", "l3", T1130, "2.00", "0.05");
			HistoricalBettingOpportunity afterRecovery = homeZero("d", "e-later", "l4", T1830, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					config,
					List.of(openWinner, loser, pauseTrigger, afterRecovery),
					List.of(win("e-win", T18), loss("e-loss", T11), win("e-pause", T14), win("e-later", Instant.parse("2026-08-17T12:00:00Z")))));
			assertThat(result.pausedByDrawdown()).isTrue();
			assertThat(result.counts().betsAccepted()).isEqualTo(2);
			assertThat(result.acceptedBetResults())
					.extracting(BacktestBetResult::opportunityId)
					.containsExactly("b", "a");
			assertThat(result.counts().opportunitiesSkippedByDrawdownPause()).isEqualTo(2);
			assertThat(result.finalTotalEquity()).isEqualByComparingTo(STARTING);
		}

		@Test
		void vaultSweepUsesAccountingEngineAndDoesNotInflateLaterStakeBase() {
			StrategyConfig config = vaultOnFlat();
			HistoricalBettingOpportunity first = homeZero("a", "e1", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity second = homeZero("b", "e2", "l2", T1201, "2.00", "0.05");
			PayoutResult firstPayout = payoutCalculator.calculate(SettlementResult.WIN, money("2.00"), money("2000"));
			BankrollState afterFirst = accountingEngine.applyPayout(
					BankrollState.initial(OWNER, STARTING),
					SettlementResult.WIN,
					firstPayout,
					config,
					"a",
					T12).state();
			StrategyDecision expectedSecond = strategyEngine.decide(
					config,
					second.opportunity(),
					afterFirst,
					PortfolioExposure.none());
			BacktestResult result = engine.run(request(
					config,
					List.of(first, second),
					List.of(win("e1", T12), win("e2", T18))));
			assertThat(result.acceptedBetResults().get(0).vaultBalanceAfterSettlement())
					.isEqualByComparingTo(afterFirst.vaultBalance());
			assertThat(result.finalVaultBalance()).isPositive();
			assertThat(result.acceptedBetResults().get(1).stake()).isEqualByComparingTo(expectedSecond.stake());
			assertThat(expectedSecond.stake()).isLessThan(money("2040"));
			assertProfitInvariant(result);
		}
	}

	@Nested
	class MaxAcceptedBets {

		@Test
		void maxAcceptedBetsTakesTheFirstNAcceptedChronologicallyNotHindsightBest() {
			HistoricalBettingOpportunity a = homeZero("a", "e-a", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity b = homeZero("b", "e-b", "l1", T1030, "2.00", "0.05");
			HistoricalBettingOpportunity c = homeZero("c", "e-c", "l1", T11, "2.00", "0.05");
			HistoricalBettingOpportunity d = homeZero("d", "e-d", "l1", T1130, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(a, b, c, d),
					List.of(loss("e-a", T12), loss("e-b", T13), win("e-c", T14), win("e-d", T18)),
					2));
			assertThat(result.acceptedBetResults())
					.extracting(BacktestBetResult::opportunityId)
					.containsExactly("a", "b");
			assertThat(result.counts().opportunitiesSkippedByBetLimit()).isEqualTo(2);
			assertThat(result.counts().betsAccepted()).isEqualTo(2);
			assertThat(result.counts().losses()).isEqualTo(2);
			assertThat(result.counts().wins()).isZero();
		}

		@Test
		void maxAcceptedBetsStillSettlesAlreadyOpenBets() {
			HistoricalBettingOpportunity a = homeZero("a", "e-a", "l1", T10, "2.00", "0.05");
			HistoricalBettingOpportunity b = homeZero("b", "e-b", "l1", T1030, "2.00", "0.05");
			HistoricalBettingOpportunity c = homeZero("c", "e-c", "l1", T1230, "2.00", "0.05");
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(a, b, c),
					List.of(win("e-a", T12), win("e-b", T14), win("e-c", T18)),
					2));
			assertThat(result.acceptedBetResults()).hasSize(2);
			assertThat(result.acceptedBetResults())
					.allMatch(bet -> bet.settlementResult() == SettlementResult.WIN);
			assertThat(result.counts().opportunitiesSkippedByBetLimit()).isEqualTo(1);
			assertThat(result.equityCurve()).hasSize(3);
			assertProfitInvariant(result);
		}
	}

	@Nested
	class MetricsAndDeterminism {

		@Test
		void equityCurveRecordsInitialPointAndEachSettlement() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05"), homeZero("b", "e2", "l2", T11, "2.00", "0.05")),
					List.of(win("e1", T12), loss("e2", T13))));
			assertThat(result.equityCurve()).hasSize(3);
			assertThat(result.equityCurve().getFirst().timestamp()).isEqualTo(T10);
			assertThat(result.equityCurve().getFirst().totalEquity()).isEqualByComparingTo(STARTING);
			assertThat(result.equityCurve().get(1).timestamp()).isEqualTo(T12);
			assertThat(result.equityCurve().get(2).timestamp()).isEqualTo(T13);
		}

		@Test
		void maxDrawdownRatesComeFromBankrollStateAfterEachSettlement() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
					List.of(loss("e1", T18))));
			assertThat(result.metrics().maxActiveDrawdownRate()).isEqualByComparingTo("0.02");
			assertThat(result.metrics().maxTotalEquityDrawdownRate()).isEqualByComparingTo("0.02");
			assertThat(result.equityCurve().getFirst().activeDrawdownRate()).isEqualByComparingTo("0");
		}

		@Test
		void longestLosingStreakCountsNegativeProfitAndResetsOnPushOrWin() {
			StrategyConfig config = highCapacityFlat("0.02", "0.02", "0.50");
			BacktestResult result = engine.run(request(
					config,
					List.of(
							homeZero("l1", "e1", "lg", T10, "2.00", "0.05"),
							homeZero("l2", "e2", "lg", T1030, "2.00", "0.05"),
							homeZero("w", "e3", "lg", T11, "2.00", "0.05"),
							homeZero("l3", "e4", "lg", T1130, "2.00", "0.05"),
							asianHome("hl", "e5", "lg", DAY, T12, "-0.25", "2.00", "0.05"),
							homeZero("p", "e6", "lg", T1230, "2.00", "0.05"),
							homeZero("l4", "e7", "lg", T13, "2.00", "0.05")),
					List.of(
							loss("e1", Instant.parse("2026-08-16T10:20:00Z")),
							loss("e2", Instant.parse("2026-08-16T10:50:00Z")),
							win("e3", Instant.parse("2026-08-16T11:20:00Z")),
							loss("e4", Instant.parse("2026-08-16T11:50:00Z")),
							draw("e5", Instant.parse("2026-08-16T12:20:00Z")),
							draw("e6", Instant.parse("2026-08-16T12:50:00Z")),
							loss("e7", T14))));
			assertThat(result.metrics().longestLosingStreak()).isEqualTo(2);
			assertThat(result.counts().losses()).isEqualTo(4);
			assertThat(result.counts().halfLosses()).isEqualTo(1);
			assertThat(result.counts().pushes()).isEqualTo(1);
			assertThat(result.counts().wins()).isEqualTo(1);
		}

		@Test
		void profitEqualsFinalEquityMinusStartingBankrollAndSumOfSettledProfits() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(
							homeZero("a", "e1", "l1", T10, "2.00", "0.05"),
							homeZero("b", "e2", "l2", T11, "2.00", "0.05"),
							homeZero("c", "e3", "l3", T12, "2.00", "0.05")),
					List.of(win("e1", T13), loss("e2", T14), draw("e3", T18))));
			assertProfitInvariant(result);
			assertThat(result.metrics().totalReturn()).isEqualByComparingTo(
					result.acceptedBetResults().stream()
							.map(BacktestBetResult::returnAmount)
							.reduce(BigDecimal.ZERO, BigDecimal::add));
			assertThat(result.metrics().totalStake()).isEqualByComparingTo(
					result.acceptedBetResults().stream()
							.map(BacktestBetResult::stake)
							.reduce(BigDecimal.ZERO, BigDecimal::add));
		}

		@Test
		void roiIsProfitOverStake() {
			BacktestResult result = engine.run(request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05")),
					List.of(win("e1", T18))));
			assertThat(result.metrics().roi()).isEqualByComparingTo("1");
			assertThat(result.metrics().averageOdds()).isEqualByComparingTo("2");
			assertThat(result.metrics().averageStake()).isEqualByComparingTo("2000");
			assertThat(result.metrics().averageEdge()).isEqualByComparingTo("0.05");
		}

		@Test
		void identicalRequestsAreDeterministic() {
			BacktestRequest req = request(
					flatTwoPercent(),
					List.of(homeZero("a", "e1", "l1", T10, "2.00", "0.05"), homeZero("b", "e2", "l2", T11, "2.00", "0.05")),
					List.of(win("e1", T12), loss("e2", T18)));
			assertThat(engine.run(req)).isEqualTo(engine.run(req));
		}

		@Test
		void differentStrategyConfigsOnTheSameSequenceProduceIndependentResults() {
			List<HistoricalBettingOpportunity> opportunities = List.of(
					homeZero("a", "e1", "l1", T10, "2.00", "0.05"),
					homeZero("b", "e2", "l2", T11, "2.00", "0.05"));
			List<HistoricalEventResult> results = List.of(win("e1", T12), win("e2", T18));
			StrategyConfig defensive = new StrategyPresetFactory().configFor(StrategyPreset.DEFENSIVE);
			StrategyConfig flat = new StrategyPresetFactory().configFor(StrategyPreset.FLAT_STAKE);
			BacktestResult defensiveResult = engine.run(request(defensive, opportunities, results));
			BacktestResult flatResult = engine.run(request(flat, opportunities, results));
			assertThat(defensive.stakingMode()).isEqualTo(StakingMode.FRACTIONAL_KELLY);
			assertThat(flat.stakingMode()).isEqualTo(StakingMode.FLAT_STAKE);
			assertThat(defensiveResult.acceptedBetResults().getFirst().stake())
					.isNotEqualByComparingTo(flatResult.acceptedBetResults().getFirst().stake());
			assertThat(engine.run(request(defensive, opportunities, results))).isEqualTo(defensiveResult);
		}
	}

	private static void assertProfitInvariant(BacktestResult result) {
		BigDecimal fromEquity = result.finalTotalEquity().subtract(result.startingBankroll());
		BigDecimal fromBets = result.acceptedBetResults().stream()
				.map(BacktestBetResult::profit)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(result.metrics().totalProfit()).isEqualByComparingTo(fromEquity);
		assertThat(result.metrics().totalProfit()).isEqualByComparingTo(fromBets);
	}

}
