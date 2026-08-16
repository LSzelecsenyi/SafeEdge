package com.safeedge.backtest;

import com.safeedge.bankroll.AccountingResult;
import com.safeedge.bankroll.BankrollAccountingEngine;
import com.safeedge.bankroll.BankrollState;
import com.safeedge.settlement.PayoutCalculator;
import com.safeedge.settlement.PayoutResult;
import com.safeedge.settlement.SettlementEngine;
import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.PortfolioExposure;
import com.safeedge.strategy.StrategyDecision;
import com.safeedge.strategy.StrategyDecisionReason;
import com.safeedge.strategy.StrategyEngine;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure, deterministic replay of a prepared historical opportunity stream.
 *
 * <p>Look-ahead guardrail: {@link HistoricalEventResult#finalScore()} is consumed
 * only when an open bet reaches {@code settlementAt}. Future results never enter
 * {@link StrategyEngine} decisions.
 *
 * <p>Settlement-before-decision: before evaluating an opportunity at {@code T},
 * every open bet with {@code settlementAt <= T} is settled first (equal timestamps
 * settle first). Multiple bets at the same settlement time are settled in
 * acceptance order.
 *
 * <p>Daily exposure is cumulative accepted stake on {@link BettingOpportunity#bettingDate()},
 * not current open risk. Settlements do not release that day's budget.
 *
 * <p>{@code PAUSED_DRAWDOWN} latches for the rest of the run. Later bankroll
 * recovery does not resume betting.
 */
public final class BacktestEngine {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final Instant EMPTY_RUN_START = Instant.EPOCH;

	private final StrategyEngine strategyEngine;
	private final SettlementEngine settlementEngine;
	private final PayoutCalculator payoutCalculator;
	private final BankrollAccountingEngine bankrollAccountingEngine;

	public BacktestEngine() {
		this(new StrategyEngine(), new SettlementEngine(), new PayoutCalculator(), new BankrollAccountingEngine());
	}

	public BacktestEngine(
			StrategyEngine strategyEngine,
			SettlementEngine settlementEngine,
			PayoutCalculator payoutCalculator,
			BankrollAccountingEngine bankrollAccountingEngine) {
		if (strategyEngine == null) {
			throw new BacktestException("strategyEngine is required");
		}
		if (settlementEngine == null) {
			throw new BacktestException("settlementEngine is required");
		}
		if (payoutCalculator == null) {
			throw new BacktestException("payoutCalculator is required");
		}
		if (bankrollAccountingEngine == null) {
			throw new BacktestException("bankrollAccountingEngine is required");
		}
		this.strategyEngine = strategyEngine;
		this.settlementEngine = settlementEngine;
		this.payoutCalculator = payoutCalculator;
		this.bankrollAccountingEngine = bankrollAccountingEngine;
	}

	public BacktestResult run(BacktestRequest request) {
		if (request == null) {
			throw new BacktestException("Backtest request is required");
		}
		Map<String, HistoricalEventResult> resultsByEventId = resultsByEventId(request.eventResults());
		RunState state = new RunState(BankrollState.initial(request.ownerId(), request.startingBankroll()));
		Instant initialTimestamp = request.opportunities().isEmpty()
				? EMPTY_RUN_START
				: request.opportunities().getFirst().decisionAt();
		state.recordEquity(initialTimestamp);

		for (HistoricalBettingOpportunity historical : request.opportunities()) {
			state.opportunitiesProcessed++;
			settleDue(state, request, resultsByEventId, historical.decisionAt());
			if (state.pausedByDrawdown) {
				state.opportunitiesSkippedByDrawdownPause++;
				continue;
			}
			if (request.maxAcceptedBets() != null && state.betsAccepted >= request.maxAcceptedBets()) {
				state.opportunitiesSkippedByBetLimit++;
				continue;
			}
			BettingOpportunity opportunity = historical.opportunity();
			PortfolioExposure exposure = deriveExposure(state, opportunity);
			StrategyDecision decision = strategyEngine.decide(
					request.strategyConfig(),
					opportunity,
					state.bankroll,
					exposure);
			switch (decision.status()) {
				case ACCEPTED -> openBet(state, historical, decision, resultsByEventId);
				case REJECTED -> reject(state, decision);
				case PAUSED_DRAWDOWN -> {
					state.pausedByDrawdown = true;
					state.opportunitiesSkippedByDrawdownPause++;
				}
			}
		}
		settleRemaining(state, request, resultsByEventId);
		return state.toResult(request.startingBankroll());
	}

	private void openBet(
			RunState state,
			HistoricalBettingOpportunity historical,
			StrategyDecision decision,
			Map<String, HistoricalEventResult> resultsByEventId) {
		HistoricalEventResult result = resultsByEventId.get(historical.opportunity().eventId());
		state.acceptanceSeq++;
		state.openBets.add(new OpenBet(state.acceptanceSeq, historical, decision, result.settlementAt()));
		state.betsAccepted++;
		state.totalStake = state.totalStake.add(decision.stake());
		state.sumOdds = state.sumOdds.add(historical.opportunity().odds());
		state.sumEdge = state.sumEdge.add(historical.opportunity().edge());
		LocalDate bettingDate = historical.opportunity().bettingDate();
		state.acceptedStakeByDay.merge(bettingDate, decision.stake(), BigDecimal::add);
	}

	private static void reject(RunState state, StrategyDecision decision) {
		state.opportunitiesRejected++;
		for (StrategyDecisionReason reason : decision.reasons()) {
			state.rejectionReasonCounts.merge(reason, 1L, Long::sum);
		}
	}

	private void settleDue(
			RunState state,
			BacktestRequest request,
			Map<String, HistoricalEventResult> resultsByEventId,
			Instant atOrBefore) {
		List<OpenBet> due = state.openBets.stream()
				.filter(bet -> !bet.settlementAt().isAfter(atOrBefore))
				.sorted(SETTLEMENT_ORDER)
				.toList();
		for (OpenBet bet : due) {
			settle(state, request, resultsByEventId, bet);
			state.openBets.remove(bet);
		}
	}

	private void settleRemaining(
			RunState state,
			BacktestRequest request,
			Map<String, HistoricalEventResult> resultsByEventId) {
		List<OpenBet> remaining = new ArrayList<>(state.openBets);
		remaining.sort(SETTLEMENT_ORDER);
		for (OpenBet bet : remaining) {
			settle(state, request, resultsByEventId, bet);
		}
		state.openBets.clear();
	}

	private void settle(
			RunState state,
			BacktestRequest request,
			Map<String, HistoricalEventResult> resultsByEventId,
			OpenBet bet) {
		HistoricalEventResult eventResult = resultsByEventId.get(bet.opportunity().eventId());
		SettlementResult settlementResult = settlementEngine.settle(
				bet.historical().market(),
				bet.historical().selection(),
				eventResult.finalScore());
		PayoutResult payout = payoutCalculator.calculate(
				settlementResult,
				bet.opportunity().odds(),
				bet.stake());
		AccountingResult accounting = bankrollAccountingEngine.applyPayout(
				state.bankroll,
				settlementResult,
				payout,
				request.strategyConfig(),
				bet.opportunity().opportunityId(),
				bet.settlementAt());
		state.bankroll = accounting.state();
		state.trackDrawdowns();
		state.trackLosingStreak(payout.profit());
		state.countSettlement(settlementResult);
		state.totalReturn = state.totalReturn.add(payout.returnAmount());
		state.totalProfit = state.totalProfit.add(payout.profit());
		state.acceptedBetResults.add(new BacktestBetResult(
				bet.opportunity().opportunityId(),
				bet.opportunity().eventId(),
				bet.opportunity().leagueId(),
				bet.opportunity().bettingDate(),
				bet.historical().decisionAt(),
				bet.settlementAt(),
				bet.opportunity().odds(),
				bet.opportunity().edge(),
				bet.stake(),
				bet.decision().reasons(),
				settlementResult,
				payout.returnAmount(),
				payout.profit(),
				state.bankroll.activeBankroll(),
				state.bankroll.vaultBalance(),
				state.bankroll.totalEquity()));
		state.recordEquity(bet.settlementAt());
	}

	/**
	 * Match and league amounts are currently open stakes. Daily amount is
	 * cumulative accepted stake on the opportunity's {@code bettingDate} and is
	 * not reduced by same-day settlement.
	 */
	private static PortfolioExposure deriveExposure(RunState state, BettingOpportunity opportunity) {
		BigDecimal match = BigDecimal.ZERO;
		BigDecimal league = BigDecimal.ZERO;
		for (OpenBet open : state.openBets) {
			if (open.opportunity().eventId().equals(opportunity.eventId())) {
				match = match.add(open.stake());
			}
			if (open.opportunity().leagueId().equals(opportunity.leagueId())) {
				league = league.add(open.stake());
			}
		}
		BigDecimal daily = state.acceptedStakeByDay.getOrDefault(opportunity.bettingDate(), BigDecimal.ZERO);
		return new PortfolioExposure(match, league, daily);
	}

	private static Map<String, HistoricalEventResult> resultsByEventId(List<HistoricalEventResult> eventResults) {
		Map<String, HistoricalEventResult> byEventId = new HashMap<>();
		for (HistoricalEventResult result : eventResults) {
			byEventId.put(result.eventId(), result);
		}
		return Map.copyOf(byEventId);
	}

	private static final Comparator<OpenBet> SETTLEMENT_ORDER =
			Comparator.comparing(OpenBet::settlementAt).thenComparingInt(OpenBet::acceptanceOrder);

	private record OpenBet(
			int acceptanceOrder,
			HistoricalBettingOpportunity historical,
			StrategyDecision decision,
			Instant settlementAt) {

		BettingOpportunity opportunity() {
			return historical.opportunity();
		}

		BigDecimal stake() {
			return decision.stake();
		}
	}

	private static final class RunState {
		private BankrollState bankroll;
		private final List<OpenBet> openBets = new ArrayList<>();
		private final Map<LocalDate, BigDecimal> acceptedStakeByDay = new HashMap<>();
		private final List<BacktestBetResult> acceptedBetResults = new ArrayList<>();
		private final List<BacktestEquityPoint> equityCurve = new ArrayList<>();
		private final EnumMap<StrategyDecisionReason, Long> rejectionReasonCounts =
				new EnumMap<>(StrategyDecisionReason.class);
		private int opportunitiesProcessed;
		private int betsAccepted;
		private int opportunitiesRejected;
		private int opportunitiesSkippedByBetLimit;
		private int opportunitiesSkippedByDrawdownPause;
		private int wins;
		private int halfWins;
		private int pushes;
		private int halfLosses;
		private int losses;
		private int acceptanceSeq;
		private boolean pausedByDrawdown;
		private BigDecimal maxActiveDrawdownRate;
		private BigDecimal maxTotalEquityDrawdownRate;
		private int currentLosingStreak;
		private int longestLosingStreak;
		private BigDecimal totalStake = BigDecimal.ZERO;
		private BigDecimal totalReturn = BigDecimal.ZERO;
		private BigDecimal totalProfit = BigDecimal.ZERO;
		private BigDecimal sumOdds = BigDecimal.ZERO;
		private BigDecimal sumEdge = BigDecimal.ZERO;

		private RunState(BankrollState bankroll) {
			this.bankroll = bankroll;
			this.maxActiveDrawdownRate = bankroll.activeDrawdownRate();
			this.maxTotalEquityDrawdownRate = bankroll.totalEquityDrawdownRate();
		}

		private void recordEquity(Instant timestamp) {
			equityCurve.add(new BacktestEquityPoint(
					timestamp,
					bankroll.activeBankroll(),
					bankroll.vaultBalance(),
					bankroll.totalEquity(),
					bankroll.activeDrawdownRate(),
					bankroll.totalEquityDrawdownRate()));
		}

		private void trackDrawdowns() {
			if (bankroll.activeDrawdownRate().compareTo(maxActiveDrawdownRate) > 0) {
				maxActiveDrawdownRate = bankroll.activeDrawdownRate();
			}
			if (bankroll.totalEquityDrawdownRate().compareTo(maxTotalEquityDrawdownRate) > 0) {
				maxTotalEquityDrawdownRate = bankroll.totalEquityDrawdownRate();
			}
		}

		/**
		 * Losing streak is consecutive settled bets with {@code profit < 0}
		 * (LOSS and HALF_LOSS). WIN, HALF_WIN, and PUSH reset the streak.
		 */
		private void trackLosingStreak(BigDecimal profit) {
			if (profit.compareTo(BigDecimal.ZERO) < 0) {
				currentLosingStreak++;
				if (currentLosingStreak > longestLosingStreak) {
					longestLosingStreak = currentLosingStreak;
				}
			}
			else {
				currentLosingStreak = 0;
			}
		}

		private void countSettlement(SettlementResult settlementResult) {
			switch (settlementResult) {
				case WIN -> wins++;
				case HALF_WIN -> halfWins++;
				case PUSH -> pushes++;
				case HALF_LOSS -> halfLosses++;
				case LOSS -> losses++;
			}
		}

		private BacktestResult toResult(BigDecimal startingBankroll) {
			int accepted = betsAccepted;
			BigDecimal averageOdds = average(sumOdds, accepted);
			BigDecimal averageStake = average(totalStake, accepted);
			BigDecimal averageEdge = average(sumEdge, accepted);
			BigDecimal roi = totalStake.compareTo(BigDecimal.ZERO) == 0
					? BigDecimal.ZERO
					: totalProfit.divide(totalStake, MATH);
			return new BacktestResult(
					startingBankroll,
					bankroll.activeBankroll(),
					bankroll.vaultBalance(),
					bankroll.totalEquity(),
					pausedByDrawdown,
					new BacktestCounts(
							opportunitiesProcessed,
							betsAccepted,
							opportunitiesRejected,
							opportunitiesSkippedByBetLimit,
							opportunitiesSkippedByDrawdownPause,
							wins,
							halfWins,
							pushes,
							halfLosses,
							losses),
					new BacktestMetrics(
							totalStake,
							totalReturn,
							totalProfit,
							roi,
							maxActiveDrawdownRate,
							maxTotalEquityDrawdownRate,
							longestLosingStreak,
							averageOdds,
							averageStake,
							averageEdge),
					acceptedBetResults,
					equityCurve,
					rejectionReasonCounts);
		}

		private static BigDecimal average(BigDecimal sum, int count) {
			if (count == 0) {
				return BigDecimal.ZERO;
			}
			return sum.divide(BigDecimal.valueOf(count), MATH);
		}
	}

}
