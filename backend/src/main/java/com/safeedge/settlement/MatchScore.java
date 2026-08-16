package com.safeedge.settlement;

public record MatchScore(int homeGoals, int awayGoals) {

	public MatchScore {
		if (homeGoals < 0 || awayGoals < 0) {
			throw new SettlementException("Match score goals must be non-negative");
		}
	}

}
