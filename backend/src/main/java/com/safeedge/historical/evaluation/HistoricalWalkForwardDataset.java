package com.safeedge.historical.evaluation;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import java.util.List;

public record HistoricalWalkForwardDataset(
		WalkForwardBuildStats stats,
		List<HistoricalBettingOpportunity> opportunities,
		List<HistoricalEventResult> eventResults) {

	public HistoricalWalkForwardDataset {
		if (stats == null) {
			throw new IllegalArgumentException("stats are required");
		}
		opportunities = List.copyOf(opportunities == null ? List.of() : opportunities);
		eventResults = List.copyOf(eventResults == null ? List.of() : eventResults);
	}
}
