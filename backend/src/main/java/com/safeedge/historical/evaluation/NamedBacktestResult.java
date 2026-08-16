package com.safeedge.historical.evaluation;

import com.safeedge.backtest.BacktestResult;
import com.safeedge.strategy.StrategyConfig;

public record NamedBacktestResult(String name, StrategyConfig config, BacktestResult result) {

	public NamedBacktestResult {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("strategy name is required");
		}
		if (config == null) {
			throw new IllegalArgumentException("strategy config is required");
		}
		if (result == null) {
			throw new IllegalArgumentException("backtest result is required");
		}
	}
}
