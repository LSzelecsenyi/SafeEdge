package com.safeedge.historical.footballdata.client;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.footballdata.mapper.FootballDataLeague;

/**
 * Builds football-data.co.uk CSV paths. Season {@code 2023/24} becomes
 * {@code 2324}; league codes stay in {@link FootballDataLeague}.
 */
public final class FootballDataPaths {

	private FootballDataPaths() {
	}

	public static String csvPath(FootballDataLeague league, FootballSeason season) {
		if (league == null) {
			throw new IllegalArgumentException("league is required");
		}
		if (season == null) {
			throw new IllegalArgumentException("season is required");
		}
		return "/mmz4281/" + seasonCode(season) + "/" + league.code() + ".csv";
	}

	public static String seasonCode(FootballSeason season) {
		return twoDigit(season.startYear()) + twoDigit(season.endYear());
	}

	private static String twoDigit(int year) {
		String value = String.valueOf(year);
		return value.substring(value.length() - 2);
	}

}
