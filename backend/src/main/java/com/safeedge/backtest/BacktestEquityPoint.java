package com.safeedge.backtest;

import java.math.BigDecimal;
import java.time.Instant;

public record BacktestEquityPoint(
		Instant timestamp,
		BigDecimal activeBankroll,
		BigDecimal vaultBalance,
		BigDecimal totalEquity,
		BigDecimal activeDrawdownRate,
		BigDecimal totalEquityDrawdownRate) {

	public BacktestEquityPoint {
		if (timestamp == null) {
			throw new BacktestException("timestamp is required");
		}
		activeBankroll = requireMoney(activeBankroll, "activeBankroll");
		vaultBalance = requireMoney(vaultBalance, "vaultBalance");
		totalEquity = requireMoney(totalEquity, "totalEquity");
		activeDrawdownRate = requireMoney(activeDrawdownRate, "activeDrawdownRate");
		totalEquityDrawdownRate = requireMoney(totalEquityDrawdownRate, "totalEquityDrawdownRate");
	}

	private static BigDecimal requireMoney(BigDecimal value, String name) {
		if (value == null) {
			throw new BacktestException(name + " is required");
		}
		return value.stripTrailingZeros();
	}

}
