package com.safeedge.backtest;

import com.safeedge.settlement.SettlementResult;
import com.safeedge.strategy.StrategyDecisionReason;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BacktestBetResult(
		String opportunityId,
		String eventId,
		String leagueId,
		LocalDate bettingDate,
		Instant decisionAt,
		Instant settlementAt,
		BigDecimal odds,
		BigDecimal edge,
		BigDecimal stake,
		List<StrategyDecisionReason> strategyDecisionReasons,
		SettlementResult settlementResult,
		BigDecimal returnAmount,
		BigDecimal profit,
		BigDecimal activeBankrollAfterSettlement,
		BigDecimal vaultBalanceAfterSettlement,
		BigDecimal totalEquityAfterSettlement) {

	public BacktestBetResult {
		if (opportunityId == null || opportunityId.isBlank()) {
			throw new BacktestException("opportunityId is required");
		}
		if (eventId == null || eventId.isBlank()) {
			throw new BacktestException("eventId is required");
		}
		if (leagueId == null || leagueId.isBlank()) {
			throw new BacktestException("leagueId is required");
		}
		if (bettingDate == null) {
			throw new BacktestException("bettingDate is required");
		}
		if (decisionAt == null) {
			throw new BacktestException("decisionAt is required");
		}
		if (settlementAt == null) {
			throw new BacktestException("settlementAt is required");
		}
		if (odds == null) {
			throw new BacktestException("odds is required");
		}
		if (edge == null) {
			throw new BacktestException("edge is required");
		}
		if (stake == null) {
			throw new BacktestException("stake is required");
		}
		if (strategyDecisionReasons == null) {
			throw new BacktestException("strategyDecisionReasons are required");
		}
		if (settlementResult == null) {
			throw new BacktestException("settlementResult is required");
		}
		if (returnAmount == null) {
			throw new BacktestException("returnAmount is required");
		}
		if (profit == null) {
			throw new BacktestException("profit is required");
		}
		if (activeBankrollAfterSettlement == null) {
			throw new BacktestException("activeBankrollAfterSettlement is required");
		}
		if (vaultBalanceAfterSettlement == null) {
			throw new BacktestException("vaultBalanceAfterSettlement is required");
		}
		if (totalEquityAfterSettlement == null) {
			throw new BacktestException("totalEquityAfterSettlement is required");
		}
		odds = odds.stripTrailingZeros();
		edge = edge.stripTrailingZeros();
		stake = stake.stripTrailingZeros();
		returnAmount = returnAmount.stripTrailingZeros();
		profit = profit.stripTrailingZeros();
		activeBankrollAfterSettlement = activeBankrollAfterSettlement.stripTrailingZeros();
		vaultBalanceAfterSettlement = vaultBalanceAfterSettlement.stripTrailingZeros();
		totalEquityAfterSettlement = totalEquityAfterSettlement.stripTrailingZeros();
		strategyDecisionReasons = List.copyOf(strategyDecisionReasons);
	}

}
