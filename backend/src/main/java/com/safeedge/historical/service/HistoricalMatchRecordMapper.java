package com.safeedge.historical.service;

import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.historical.repository.HistoricalMatchEntity;
import com.safeedge.settlement.MatchScore;

final class HistoricalMatchRecordMapper {

	private HistoricalMatchRecordMapper() {
	}

	static HistoricalMatchRecord fromEntity(HistoricalMatchEntity entity) {
		return new HistoricalMatchRecord(
				entity.getSource(),
				entity.getCanonicalCompetition(),
				new FootballSeason(entity.getSeasonStartYear(), entity.getSeasonEndYear()),
				entity.getMatchDate(),
				entity.getKickoffUtc(),
				entity.getSourceHomeTeamName(),
				entity.getSourceAwayTeamName(),
				new MatchScore(entity.getHomeGoals(), entity.getAwayGoals()),
				entity.getSourceRowNumber(),
				entity.getId());
	}
}
