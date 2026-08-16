package com.safeedge.historical.evaluation;

import java.util.List;

/**
 * Dataset plus the available prediction snapshots from the same walk-forward
 * pass. {@link #dataset()} is the unchanged candidate/result contract.
 */
public record HistoricalWalkForwardBuildOutput(
		HistoricalWalkForwardDataset dataset, List<HistoricalPredictionSnapshot> predictions) {

	public HistoricalWalkForwardBuildOutput {
		if (dataset == null) {
			throw new IllegalArgumentException("dataset is required");
		}
		predictions = List.copyOf(predictions == null ? List.of() : predictions);
	}
}
