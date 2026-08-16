package com.safeedge.historical.footballdata.dto;

import java.util.Map;

/**
 * One football-data.co.uk CSV row. Optional AH columns are absent when the
 * header or cell is missing. Column names stay in this DTO only.
 */
public record FootballDataCsvRow(
		int sourceRowNumber,
		String div,
		String date,
		String time,
		String homeTeam,
		String awayTeam,
		String fthg,
		String ftag,
		Map<String, String> optionalColumns) {

	public FootballDataCsvRow {
		if (optionalColumns == null) {
			optionalColumns = Map.of();
		}
		else {
			optionalColumns = Map.copyOf(optionalColumns);
		}
	}

	public String optional(String column) {
		return optionalColumns.get(column);
	}

}
