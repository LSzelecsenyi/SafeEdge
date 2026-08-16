package com.safeedge.historical.features;

import com.safeedge.historical.domain.HistoricalDataException;
import java.math.BigDecimal;

/**
 * Point-in-time pre-match football-performance features. Averages are null when
 * the corresponding sample is empty (unknown), never a synthetic zero. The
 * current match result is not represented here.
 */
public record PreMatchFeatures(
		int homeTeamMatchesPlayed,
		int awayTeamMatchesPlayed,
		int homeLast5Matches,
		BigDecimal homeLast5GoalsForPerMatch,
		BigDecimal homeLast5GoalsAgainstPerMatch,
		BigDecimal homeLast5PointsPerMatch,
		int awayLast5Matches,
		BigDecimal awayLast5GoalsForPerMatch,
		BigDecimal awayLast5GoalsAgainstPerMatch,
		BigDecimal awayLast5PointsPerMatch,
		int homeLast10Matches,
		BigDecimal homeLast10GoalsForPerMatch,
		BigDecimal homeLast10GoalsAgainstPerMatch,
		int awayLast10Matches,
		BigDecimal awayLast10GoalsForPerMatch,
		BigDecimal awayLast10GoalsAgainstPerMatch,
		int homeTeamLast5HomeMatches,
		BigDecimal homeTeamLast5HomeGoalsForPerMatch,
		BigDecimal homeTeamLast5HomeGoalsAgainstPerMatch,
		int awayTeamLast5AwayMatches,
		BigDecimal awayTeamLast5AwayGoalsForPerMatch,
		BigDecimal awayTeamLast5AwayGoalsAgainstPerMatch,
		int leagueMatchesObserved,
		BigDecimal leagueHomeGoalsPerMatch,
		BigDecimal leagueAwayGoalsPerMatch,
		BigDecimal leagueTotalGoalsPerMatch) {

	public PreMatchFeatures {
		requireNonNegative(homeTeamMatchesPlayed, "homeTeamMatchesPlayed");
		requireNonNegative(awayTeamMatchesPlayed, "awayTeamMatchesPlayed");
		requireNonNegative(homeLast5Matches, "homeLast5Matches");
		requireNonNegative(awayLast5Matches, "awayLast5Matches");
		requireNonNegative(homeLast10Matches, "homeLast10Matches");
		requireNonNegative(awayLast10Matches, "awayLast10Matches");
		requireNonNegative(homeTeamLast5HomeMatches, "homeTeamLast5HomeMatches");
		requireNonNegative(awayTeamLast5AwayMatches, "awayTeamLast5AwayMatches");
		requireNonNegative(leagueMatchesObserved, "leagueMatchesObserved");
	}

	private static void requireNonNegative(int value, String name) {
		if (value < 0) {
			throw new HistoricalDataException(name + " must be >= 0");
		}
	}

	public boolean missingTeamHistory() {
		return homeTeamMatchesPlayed == 0 || awayTeamMatchesPlayed == 0;
	}

	public boolean fullLast5History() {
		return homeLast5Matches == 5 && awayLast5Matches == 5;
	}

	public boolean fullLast10History() {
		return homeLast10Matches == 10 && awayLast10Matches == 10;
	}
}
