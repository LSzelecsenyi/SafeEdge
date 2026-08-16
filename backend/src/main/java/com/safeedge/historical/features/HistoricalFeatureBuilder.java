package com.safeedge.historical.features;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pure point-in-time feature builder. Features for match M use only results
 * from earlier dates. Same-date matches are batched so date-only rows cannot
 * leak into each other. No Spring, Clock, persistence, or odds.
 */
public final class HistoricalFeatureBuilder {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final int LAST_5 = 5;
	private static final int LAST_10 = 10;

	public HistoricalFeatureDataset build(List<HistoricalMatchRecord> matches) {
		if (matches == null) {
			throw new HistoricalDataException("matches are required");
		}
		List<HistoricalMatchRecord> input = new ArrayList<>(matches.size());
		for (HistoricalMatchRecord match : matches) {
			if (match == null) {
				throw new HistoricalDataException("matches must not contain null");
			}
			input.add(match);
		}
		Map<BatchKey, List<HistoricalMatchRecord>> batches = new TreeMap<>();
		for (HistoricalMatchRecord match : input) {
			batches.computeIfAbsent(new BatchKey(match.competition(), match.matchDate()), ignored -> new ArrayList<>())
					.add(match);
		}
		HistoryState state = new HistoryState();
		List<HistoricalModelRow> rows = new ArrayList<>(input.size());
		for (List<HistoricalMatchRecord> batch : batches.values()) {
			batch.sort(WITHIN_DATE);
			for (HistoricalMatchRecord match : batch) {
				rows.add(toRow(match, state.featuresFor(match)));
			}
			for (HistoricalMatchRecord match : batch) {
				state.apply(match);
			}
		}
		return HistoricalFeatureDataset.from(rows);
	}

	private static HistoricalModelRow toRow(HistoricalMatchRecord match, PreMatchFeatures features) {
		return new HistoricalModelRow(
				match.source(),
				match.competition(),
				match.season(),
				match.matchDate(),
				match.homeTeam(),
				match.awayTeam(),
				match.sourceRowNumber(),
				match.persistenceId(),
				features,
				match.score());
	}

	private static final Comparator<HistoricalMatchRecord> WITHIN_DATE = Comparator
			.comparingInt(HistoricalMatchRecord::sourceRowNumber)
			.thenComparing(HistoricalMatchRecord::homeTeam)
			.thenComparing(HistoricalMatchRecord::awayTeam)
			.thenComparing(record -> record.persistenceId() == null ? Long.MIN_VALUE : record.persistenceId());

	private record BatchKey(CanonicalCompetition competition, LocalDate date) implements Comparable<BatchKey> {
		@Override
		public int compareTo(BatchKey other) {
			int byDate = date.compareTo(other.date);
			if (byDate != 0) {
				return byDate;
			}
			return competition.compareTo(other.competition);
		}
	}

	private record TeamKey(CanonicalCompetition competition, String teamName) {
	}

	private record LeagueKey(CanonicalCompetition competition, int seasonStartYear, int seasonEndYear) {
		private static LeagueKey of(CanonicalCompetition competition, FootballSeason season) {
			return new LeagueKey(competition, season.startYear(), season.endYear());
		}
	}

	private static final class HistoryState {
		private final Map<TeamKey, TeamHistory> teams = new HashMap<>();
		private final Map<LeagueKey, LeagueSeasonStats> leagues = new HashMap<>();

		private PreMatchFeatures featuresFor(HistoricalMatchRecord match) {
			TeamHistory home = teams.getOrDefault(new TeamKey(match.competition(), match.homeTeam()), TeamHistory.EMPTY);
			TeamHistory away = teams.getOrDefault(new TeamKey(match.competition(), match.awayTeam()), TeamHistory.EMPTY);
			LeagueSeasonStats league = leagues.getOrDefault(LeagueKey.of(match.competition(), match.season()), LeagueSeasonStats.EMPTY);
			Window overallHome5 = home.overall(LAST_5);
			Window overallAway5 = away.overall(LAST_5);
			Window overallHome10 = home.overall(LAST_10);
			Window overallAway10 = away.overall(LAST_10);
			Window homeVenue = home.homeOnly(LAST_5);
			Window awayVenue = away.awayOnly(LAST_5);
			return new PreMatchFeatures(
					home.size(),
					away.size(),
					overallHome5.size(),
					overallHome5.goalsForPerMatch(),
					overallHome5.goalsAgainstPerMatch(),
					overallHome5.pointsPerMatch(),
					overallAway5.size(),
					overallAway5.goalsForPerMatch(),
					overallAway5.goalsAgainstPerMatch(),
					overallAway5.pointsPerMatch(),
					overallHome10.size(),
					overallHome10.goalsForPerMatch(),
					overallHome10.goalsAgainstPerMatch(),
					overallAway10.size(),
					overallAway10.goalsForPerMatch(),
					overallAway10.goalsAgainstPerMatch(),
					homeVenue.size(),
					homeVenue.goalsForPerMatch(),
					homeVenue.goalsAgainstPerMatch(),
					awayVenue.size(),
					awayVenue.goalsForPerMatch(),
					awayVenue.goalsAgainstPerMatch(),
					league.matchCount(),
					league.homeGoalsPerMatch(),
					league.awayGoalsPerMatch(),
					league.totalGoalsPerMatch());
		}

		private void apply(HistoricalMatchRecord match) {
			MatchScore score = match.score();
			teams.computeIfAbsent(new TeamKey(match.competition(), match.homeTeam()), ignored -> new TeamHistory())
					.add(score.homeGoals(), score.awayGoals(), true);
			teams.computeIfAbsent(new TeamKey(match.competition(), match.awayTeam()), ignored -> new TeamHistory())
					.add(score.awayGoals(), score.homeGoals(), false);
			leagues.computeIfAbsent(LeagueKey.of(match.competition(), match.season()), ignored -> new LeagueSeasonStats())
					.add(score.homeGoals(), score.awayGoals());
		}
	}

	private static final class TeamHistory {
		private static final TeamHistory EMPTY = new TeamHistory();

		private final List<Appearance> all = new ArrayList<>();
		private final List<Appearance> home = new ArrayList<>();
		private final List<Appearance> away = new ArrayList<>();

		private void add(int goalsFor, int goalsAgainst, boolean playedHome) {
			Appearance appearance = new Appearance(goalsFor, goalsAgainst, points(goalsFor, goalsAgainst));
			all.add(appearance);
			if (playedHome) {
				home.add(appearance);
			}
			else {
				away.add(appearance);
			}
		}

		private int size() {
			return all.size();
		}

		private Window overall(int n) {
			return Window.of(all, n);
		}

		private Window homeOnly(int n) {
			return Window.of(home, n);
		}

		private Window awayOnly(int n) {
			return Window.of(away, n);
		}
	}

	private record Appearance(int goalsFor, int goalsAgainst, int points) {
	}

	private record Window(int size, BigDecimal goalsForPerMatch, BigDecimal goalsAgainstPerMatch, BigDecimal pointsPerMatch) {
		private static Window of(List<Appearance> history, int n) {
			if (history.isEmpty()) {
				return new Window(0, null, null, null);
			}
			int from = Math.max(0, history.size() - n);
			List<Appearance> slice = history.subList(from, history.size());
			BigDecimal goalsFor = BigDecimal.ZERO;
			BigDecimal goalsAgainst = BigDecimal.ZERO;
			BigDecimal points = BigDecimal.ZERO;
			for (Appearance appearance : slice) {
				goalsFor = goalsFor.add(BigDecimal.valueOf(appearance.goalsFor()));
				goalsAgainst = goalsAgainst.add(BigDecimal.valueOf(appearance.goalsAgainst()));
				points = points.add(BigDecimal.valueOf(appearance.points()));
			}
			BigDecimal count = BigDecimal.valueOf(slice.size());
			return new Window(
					slice.size(),
					goalsFor.divide(count, MATH),
					goalsAgainst.divide(count, MATH),
					points.divide(count, MATH));
		}
	}

	private static final class LeagueSeasonStats {
		private static final LeagueSeasonStats EMPTY = new LeagueSeasonStats();

		private int matchCount;
		private BigDecimal homeGoals = BigDecimal.ZERO;
		private BigDecimal awayGoals = BigDecimal.ZERO;

		private void add(int home, int away) {
			matchCount++;
			homeGoals = homeGoals.add(BigDecimal.valueOf(home));
			awayGoals = awayGoals.add(BigDecimal.valueOf(away));
		}

		private int matchCount() {
			return matchCount;
		}

		private BigDecimal homeGoalsPerMatch() {
			return average(homeGoals);
		}

		private BigDecimal awayGoalsPerMatch() {
			return average(awayGoals);
		}

		private BigDecimal totalGoalsPerMatch() {
			return average(homeGoals.add(awayGoals));
		}

		private BigDecimal average(BigDecimal sum) {
			if (matchCount == 0) {
				return null;
			}
			return sum.divide(BigDecimal.valueOf(matchCount), MATH);
		}
	}

	private static int points(int goalsFor, int goalsAgainst) {
		if (goalsFor > goalsAgainst) {
			return 3;
		}
		if (goalsFor == goalsAgainst) {
			return 1;
		}
		return 0;
	}
}
