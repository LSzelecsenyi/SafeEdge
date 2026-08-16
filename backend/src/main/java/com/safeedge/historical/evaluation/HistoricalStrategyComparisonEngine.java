package com.safeedge.historical.evaluation;

import com.safeedge.backtest.BacktestEngine;
import com.safeedge.backtest.BacktestRequest;
import com.safeedge.backtest.BacktestResult;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs {@link BacktestEngine} against an already-prepared walk-forward dataset.
 * Does not rebuild probability predictions or candidates.
 */
public final class HistoricalStrategyComparisonEngine {

	private final BacktestEngine backtestEngine;

	public HistoricalStrategyComparisonEngine() {
		this(new BacktestEngine());
	}

	public HistoricalStrategyComparisonEngine(BacktestEngine backtestEngine) {
		if (backtestEngine == null) {
			throw new IllegalArgumentException("backtestEngine is required");
		}
		this.backtestEngine = backtestEngine;
	}

	public HistoricalBacktestEvaluationResult evaluate(
			HistoricalWalkForwardDataset dataset,
			BigDecimal startingBankroll,
			StrategyConfig strategyConfig,
			Integer maxAcceptedBets) {
		BacktestResult result = backtestEngine.run(request(dataset, startingBankroll, strategyConfig, maxAcceptedBets));
		return new HistoricalBacktestEvaluationResult(dataset, result);
	}

	public HistoricalStrategyComparisonResult compare(
			HistoricalWalkForwardDataset dataset,
			BigDecimal startingBankroll,
			List<NamedStrategyConfig> strategies,
			Integer maxAcceptedBets) {
		if (strategies == null || strategies.isEmpty()) {
			throw new IllegalArgumentException("at least one strategy is required");
		}
		List<NamedBacktestResult> results = new ArrayList<>(strategies.size());
		for (NamedStrategyConfig named : strategies) {
			if (named == null) {
				throw new IllegalArgumentException("strategies must not contain null");
			}
			BacktestResult result =
					backtestEngine.run(request(dataset, startingBankroll, named.config(), maxAcceptedBets));
			results.add(new NamedBacktestResult(named.name(), named.config(), result));
		}
		return new HistoricalStrategyComparisonResult(dataset, results);
	}

	private static BacktestRequest request(
			HistoricalWalkForwardDataset dataset,
			BigDecimal startingBankroll,
			StrategyConfig strategyConfig,
			Integer maxAcceptedBets) {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		return new BacktestRequest(
				HistoricalWalkForwardIdentities.SIMULATION_OWNER,
				startingBankroll,
				strategyConfig,
				dataset.opportunities(),
				dataset.eventResults(),
				maxAcceptedBets);
	}
}
