package com.safeedge.historical.features;

import java.util.List;

/**
 * In-memory feature dataset. No usability threshold is applied.
 */
public record HistoricalFeatureDataset(
		List<HistoricalModelRow> rows,
		int totalRows,
		int rowsWithFullLast5History,
		int rowsWithFullLast10History,
		int rowsWithMissingTeamHistory) {

	public HistoricalFeatureDataset {
		rows = List.copyOf(rows == null ? List.of() : rows);
	}

	static HistoricalFeatureDataset from(List<HistoricalModelRow> rows) {
		int fullLast5 = 0;
		int fullLast10 = 0;
		int missing = 0;
		for (HistoricalModelRow row : rows) {
			PreMatchFeatures features = row.features();
			if (features.fullLast5History()) {
				fullLast5++;
			}
			if (features.fullLast10History()) {
				fullLast10++;
			}
			if (features.missingTeamHistory()) {
				missing++;
			}
		}
		return new HistoricalFeatureDataset(rows, rows.size(), fullLast5, fullLast10, missing);
	}
}
