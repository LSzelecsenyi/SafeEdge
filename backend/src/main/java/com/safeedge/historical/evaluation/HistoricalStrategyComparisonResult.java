package com.safeedge.historical.evaluation;

import java.util.List;

public record HistoricalStrategyComparisonResult(
		HistoricalWalkForwardDataset dataset, List<NamedBacktestResult> strategyResults) {

	public HistoricalStrategyComparisonResult {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		strategyResults = List.copyOf(strategyResults == null ? List.of() : strategyResults);
	}
}
