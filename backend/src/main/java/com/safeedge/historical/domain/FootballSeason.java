package com.safeedge.historical.domain;

/**
 * Winter-calendar football season. {@code 2023/24} is {@code (2023, 2024)}.
 * Summer-calendar leagues are out of scope.
 */
public record FootballSeason(int startYear, int endYear) {

	public FootballSeason {
		if (endYear != startYear + 1) {
			throw new HistoricalDataException("Season endYear must be startYear + 1");
		}
		if (startYear < 1990 || startYear > 2100) {
			throw new HistoricalDataException("Season startYear is out of range: " + startYear);
		}
	}

	public String displayValue() {
		String end = String.valueOf(endYear);
		return startYear + "/" + end.substring(end.length() - 2);
	}

}
