package com.safeedge.backtest;

import java.math.BigDecimal;

public record BacktestMetrics(
		BigDecimal totalStake,
		BigDecimal totalReturn,
		BigDecimal totalProfit,
		BigDecimal roi,
		BigDecimal maxActiveDrawdownRate,
		BigDecimal maxTotalEquityDrawdownRate,
		int longestLosingStreak,
		BigDecimal averageOdds,
		BigDecimal averageStake,
		BigDecimal averageEdge) {

	public BacktestMetrics {
		totalStake = requireMoney(totalStake, "totalStake");
		totalReturn = requireMoney(totalReturn, "totalReturn");
		totalProfit = requireNonNull(totalProfit, "totalProfit");
		roi = requireNonNull(roi, "roi");
		maxActiveDrawdownRate = requireMoney(maxActiveDrawdownRate, "maxActiveDrawdownRate");
		maxTotalEquityDrawdownRate = requireMoney(maxTotalEquityDrawdownRate, "maxTotalEquityDrawdownRate");
		if (longestLosingStreak < 0) {
			throw new BacktestException("longestLosingStreak cannot be negative");
		}
		averageOdds = requireMoney(averageOdds, "averageOdds");
		averageStake = requireMoney(averageStake, "averageStake");
		averageEdge = requireMoney(averageEdge, "averageEdge");
	}

	private static BigDecimal requireMoney(BigDecimal value, String name) {
		return requireNonNull(value, name);
	}

	private static BigDecimal requireNonNull(BigDecimal value, String name) {
		if (value == null) {
			throw new BacktestException(name + " is required");
		}
		return value.stripTrailingZeros();
	}

}
