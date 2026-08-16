package com.safeedge.historical.repository;

import com.safeedge.historical.domain.CanonicalCompetition;

public record HistoricalLeagueSeasonCount(
		CanonicalCompetition competition, Integer seasonStartYear, Integer seasonEndYear, long matchCount) {
}
