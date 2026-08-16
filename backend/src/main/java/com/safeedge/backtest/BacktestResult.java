package com.safeedge.backtest;

import com.safeedge.strategy.StrategyDecisionReason;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BacktestResult(
		BigDecimal startingBankroll,
		BigDecimal finalActiveBankroll,
		BigDecimal finalVaultBalance,
		BigDecimal finalTotalEquity,
		boolean pausedByDrawdown,
		BacktestCounts counts,
		BacktestMetrics metrics,
		List<BacktestBetResult> acceptedBetResults,
		List<BacktestEquityPoint> equityCurve,
		Map<StrategyDecisionReason, Long> rejectionReasonCounts) {

	public BacktestResult {
		startingBankroll = requireMoney(startingBankroll, "startingBankroll");
		finalActiveBankroll = requireMoney(finalActiveBankroll, "finalActiveBankroll");
		finalVaultBalance = requireMoney(finalVaultBalance, "finalVaultBalance");
		finalTotalEquity = requireMoney(finalTotalEquity, "finalTotalEquity");
		if (counts == null) {
			throw new BacktestException("counts are required");
		}
		if (metrics == null) {
			throw new BacktestException("metrics are required");
		}
		if (acceptedBetResults == null) {
			throw new BacktestException("acceptedBetResults are required");
		}
		if (equityCurve == null) {
			throw new BacktestException("equityCurve is required");
		}
		if (rejectionReasonCounts == null) {
			throw new BacktestException("rejectionReasonCounts are required");
		}
		acceptedBetResults = List.copyOf(acceptedBetResults);
		equityCurve = List.copyOf(equityCurve);
		rejectionReasonCounts = Map.copyOf(rejectionReasonCounts);
	}

	private static BigDecimal requireMoney(BigDecimal value, String name) {
		if (value == null) {
			throw new BacktestException(name + " is required");
		}
		return value.stripTrailingZeros();
	}

}
