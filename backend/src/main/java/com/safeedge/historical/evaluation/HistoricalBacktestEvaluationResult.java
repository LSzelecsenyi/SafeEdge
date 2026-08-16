package com.safeedge.historical.evaluation;

import com.safeedge.backtest.BacktestResult;

public record HistoricalBacktestEvaluationResult(
		HistoricalWalkForwardDataset dataset, BacktestResult backtest) {

	public HistoricalBacktestEvaluationResult {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		if (backtest == null) {
			throw new IllegalArgumentException("backtest is required");
		}
	}
}
