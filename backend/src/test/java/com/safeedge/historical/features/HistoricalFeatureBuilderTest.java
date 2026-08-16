package com.safeedge.historical.features;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoricalFeatureBuilderTest {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final FootballSeason S23 = new FootballSeason(2023, 2024);
	private static final FootballSeason S24 = new FootballSeason(2024, 2025);

	private final HistoricalFeatureBuilder builder = new HistoricalFeatureBuilder();
	private int nextRow;

	@BeforeEach
	void resetRowNumbers() {
		nextRow = 1;
	}

	@Test
	void currentMatchResultIsNotInItsOwnFeatures() {
		HistoricalMatchRecord first = pl("A", "B", date(1, 1), 4, 0);
		HistoricalMatchRecord second = pl("A", "C", date(1, 8), 1, 1);
		List<HistoricalModelRow> rows = builder.build(List.of(first, second)).rows();
		PreMatchFeatures jan1 = rows.getFirst().features();
		assertThat(jan1.homeTeamMatchesPlayed()).isZero();
		assertThat(jan1.homeLast5GoalsForPerMatch()).isNull();
		assertThat(jan1.homeLast5GoalsAgainstPerMatch()).isNull();
		assertThat(rows.getFirst().target()).isEqualTo(new MatchScore(4, 0));
		PreMatchFeatures jan8 = rows.get(1).features();
		assertThat(jan8.homeTeamMatchesPlayed()).isEqualTo(1);
		assertThat(jan8.homeLast5GoalsForPerMatch()).isEqualByComparingTo("4");
		assertThat(jan8.homeLast5GoalsAgainstPerMatch()).isEqualByComparingTo("0");
		assertThat(jan8.homeLast5PointsPerMatch()).isEqualByComparingTo("3");
	}

	@Test
	void futureMatchDoesNotChangeEarlierFeatureRows() {
		HistoricalMatchRecord jan1 = pl("A", "B", date(1, 1), 1, 0);
		HistoricalMatchRecord jan8 = pl("A", "C", date(1, 8), 2, 2);
		HistoricalMatchRecord jan15 = pl("A", "D", date(1, 15), 9, 0);
		PreMatchFeatures withoutFuture = features(builder.build(List.of(jan1, jan8)), date(1, 8), "A");
		PreMatchFeatures withFuture = features(builder.build(List.of(jan1, jan8, jan15)), date(1, 8), "A");
		assertThat(withFuture).isEqualTo(withoutFuture);
		assertThat(withFuture.homeLast5GoalsForPerMatch()).isEqualByComparingTo("1");
	}

	@Test
	void sameDateMatchesDoNotSeeEachOthersResults() {
		HistoricalMatchRecord ab = pl("A", "B", date(1, 1), 5, 0);
		HistoricalMatchRecord cd = pl("C", "D", date(1, 1), 1, 0);
		HistoricalMatchRecord ef = pl("E", "F", date(1, 2), 0, 0);
		List<HistoricalModelRow> rows = builder.build(List.of(ab, cd, ef)).rows();
		assertThat(features(rows, date(1, 1), "A").leagueMatchesObserved()).isZero();
		assertThat(features(rows, date(1, 1), "A").leagueHomeGoalsPerMatch()).isNull();
		assertThat(features(rows, date(1, 1), "C").leagueMatchesObserved()).isZero();
		assertThat(features(rows, date(1, 1), "C").leagueTotalGoalsPerMatch()).isNull();
		PreMatchFeatures jan2 = features(rows, date(1, 2), "E");
		assertThat(jan2.leagueMatchesObserved()).isEqualTo(2);
		assertThat(jan2.leagueHomeGoalsPerMatch()).isEqualByComparingTo("3");
		assertThat(jan2.leagueAwayGoalsPerMatch()).isEqualByComparingTo("0");
		assertThat(jan2.leagueTotalGoalsPerMatch()).isEqualByComparingTo("3");
	}

	@Test
	void last5AndLast10UseMostRecentPriorMatches() {
		List<HistoricalMatchRecord> matches = new ArrayList<>();
		for (int i = 1; i <= 12; i++) {
			matches.add(pl("A", "Opp" + i, date(1, i), i, 0));
		}
		matches.add(pl("A", "Final", date(1, 20), 0, 0));
		PreMatchFeatures features = features(builder.build(matches), date(1, 20), "A");
		assertThat(features.homeLast5Matches()).isEqualTo(5);
		assertThat(features.homeLast5GoalsForPerMatch()).isEqualByComparingTo("10");
		assertThat(features.homeLast10Matches()).isEqualTo(10);
		assertThat(features.homeLast10GoalsForPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(75).divide(BigDecimal.TEN, MATH));
		assertThat(features.homeTeamMatchesPlayed()).isEqualTo(12);
	}

	@Test
	void venueSpecificWindowsIgnoreOppositeVenue() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("A", "B", date(1, 1), 2, 0),
				pl("C", "A", date(1, 8), 3, 0),
				pl("A", "D", date(1, 15), 1, 1),
				pl("A", "E", date(1, 22), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 22), "A");
		assertThat(features.homeLast5Matches()).isEqualTo(3);
		assertThat(features.homeLast5GoalsForPerMatch()).isEqualByComparingTo("1");
		assertThat(features.homeLast5GoalsAgainstPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), MATH));
		assertThat(features.homeTeamLast5HomeMatches()).isEqualTo(2);
		assertThat(features.homeTeamLast5HomeGoalsForPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(3).divide(BigDecimal.valueOf(2), MATH));
		assertThat(features.homeTeamLast5HomeGoalsAgainstPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(1).divide(BigDecimal.valueOf(2), MATH));
	}

	@Test
	void awayVenueWindowUsesOnlyPriorAwayFixtures() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("X", "E", date(1, 1), 1, 0),
				pl("E", "Y", date(1, 8), 2, 2),
				pl("A", "E", date(1, 15), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 15), "A");
		assertThat(features.awayTeamLast5AwayMatches()).isEqualTo(1);
		assertThat(features.awayLast5Matches()).isEqualTo(2);
		assertThat(features.awayTeamLast5AwayGoalsForPerMatch()).isEqualByComparingTo("0");
		assertThat(features.awayTeamLast5AwayGoalsAgainstPerMatch()).isEqualByComparingTo("1");
		assertThat(features.awayLast5GoalsForPerMatch()).isEqualByComparingTo("1");
	}

	@Test
	void pointsAverageUsesWinDrawLoss() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("A", "B", date(1, 1), 1, 0),
				pl("A", "C", date(1, 8), 1, 1),
				pl("A", "D", date(1, 15), 0, 2),
				pl("A", "E", date(1, 22), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 22), "A");
		assertThat(features.homeLast5Matches()).isEqualTo(3);
		assertThat(features.homeLast5PointsPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(4).divide(BigDecimal.valueOf(3), MATH));
	}

	@Test
	void teamHistoryCarriesAcrossSeasonsWhileLeagueAveragesReset() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				match(CanonicalCompetition.PREMIER_LEAGUE, S23, "A", "B", date(5, 19), 2, 0),
				match(CanonicalCompetition.PREMIER_LEAGUE, S24, "A", "C", date(8, 17), 1, 1),
				match(CanonicalCompetition.PREMIER_LEAGUE, S24, "A", "D", date(8, 24), 0, 0)))
				.rows();
		PreMatchFeatures firstNewSeason = features(rows, date(8, 17), "A");
		assertThat(firstNewSeason.homeTeamMatchesPlayed()).isEqualTo(1);
		assertThat(firstNewSeason.homeLast5GoalsForPerMatch()).isEqualByComparingTo("2");
		assertThat(firstNewSeason.leagueMatchesObserved()).isZero();
		assertThat(firstNewSeason.leagueHomeGoalsPerMatch()).isNull();
		assertThat(firstNewSeason.leagueAwayGoalsPerMatch()).isNull();
		assertThat(firstNewSeason.leagueTotalGoalsPerMatch()).isNull();
		PreMatchFeatures secondNewSeason = features(rows, date(8, 24), "A");
		assertThat(secondNewSeason.homeTeamMatchesPlayed()).isEqualTo(2);
		assertThat(secondNewSeason.homeLast5GoalsForPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(3).divide(BigDecimal.valueOf(2), MATH));
		assertThat(secondNewSeason.leagueMatchesObserved()).isEqualTo(1);
		assertThat(secondNewSeason.leagueHomeGoalsPerMatch()).isEqualByComparingTo("1");
		assertThat(secondNewSeason.leagueAwayGoalsPerMatch()).isEqualByComparingTo("1");
	}

	@Test
	void unseenTeamHasNullAveragesAndZeroCount() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("A", "B", date(1, 1), 2, 0),
				pl("A", "C", date(1, 8), 1, 0),
				pl("A", "NewTown", date(1, 15), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 15), "A");
		assertThat(features.homeTeamMatchesPlayed()).isEqualTo(2);
		assertThat(features.homeLast5GoalsForPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(3).divide(BigDecimal.valueOf(2), MATH));
		assertThat(features.awayTeamMatchesPlayed()).isZero();
		assertThat(features.awayLast5GoalsForPerMatch()).isNull();
		assertThat(features.awayLast5PointsPerMatch()).isNull();
		assertThat(features.awayTeamLast5AwayGoalsForPerMatch()).isNull();
	}

	@Test
	void sameTeamNameIsIsolatedByCompetition() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				match(CanonicalCompetition.PREMIER_LEAGUE, S23, "United", "X", date(1, 1), 4, 0),
				match(CanonicalCompetition.BUNDESLIGA, S23, "United", "Y", date(1, 8), 0, 0),
				match(CanonicalCompetition.PREMIER_LEAGUE, S23, "Z", "W", date(1, 8), 1, 0)))
				.rows();
		PreMatchFeatures bundesliga = features(rows, date(1, 8), "United");
		assertThat(bundesliga.homeTeamMatchesPlayed()).isZero();
		assertThat(bundesliga.homeLast5GoalsForPerMatch()).isNull();
		assertThat(bundesliga.leagueMatchesObserved()).isZero();
		PreMatchFeatures premierSameDay = features(rows, date(1, 8), "Z");
		assertThat(premierSameDay.leagueMatchesObserved()).isEqualTo(1);
		assertThat(premierSameDay.leagueHomeGoalsPerMatch()).isEqualByComparingTo("4");
	}

	@Test
	void partialLast5AveragesOverAvailableMatchesOnly() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("A", "B", date(1, 1), 2, 0),
				pl("A", "C", date(1, 8), 4, 2),
				pl("A", "D", date(1, 15), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 15), "A");
		assertThat(features.homeLast5Matches()).isEqualTo(2);
		assertThat(features.homeLast5GoalsForPerMatch()).isEqualByComparingTo("3");
		assertThat(features.homeLast5GoalsAgainstPerMatch()).isEqualByComparingTo("1");
	}

	@Test
	void leagueAveragesUseDecimal128AndPriorMatchesOnly() {
		List<HistoricalModelRow> rows = builder.build(List.of(
				pl("A", "B", date(1, 1), 2, 1),
				pl("C", "D", date(1, 2), 1, 1),
				pl("E", "F", date(1, 3), 3, 0),
				pl("G", "H", date(1, 4), 0, 0)))
				.rows();
		PreMatchFeatures features = features(rows, date(1, 4), "G");
		assertThat(features.leagueMatchesObserved()).isEqualTo(3);
		assertThat(features.leagueHomeGoalsPerMatch()).isEqualByComparingTo("2");
		assertThat(features.leagueAwayGoalsPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(2).divide(BigDecimal.valueOf(3), MATH));
		assertThat(features.leagueTotalGoalsPerMatch())
				.isEqualByComparingTo(BigDecimal.valueOf(8).divide(BigDecimal.valueOf(3), MATH));
	}

	@Test
	void identicalInputProducesIdenticalRows() {
		List<HistoricalMatchRecord> matches = List.of(
				pl("A", "B", date(1, 1), 1, 0),
				pl("C", "D", date(1, 1), 2, 2),
				pl("A", "C", date(1, 8), 0, 1));
		assertThat(builder.build(matches)).isEqualTo(builder.build(matches));
	}

	@Test
	void emptyInputIsEmptyDataset() {
		HistoricalFeatureDataset dataset = builder.build(List.of());
		assertThat(dataset.totalRows()).isZero();
		assertThat(dataset.rowsWithMissingTeamHistory()).isZero();
	}

	@Test
	void nullInputIsRejected() {
		assertThatThrownBy(() -> builder.build(null)).isInstanceOf(HistoricalDataException.class);
	}

	@Test
	void datasetSummaryCountsMissingAndFullHistory() {
		List<HistoricalMatchRecord> matches = new ArrayList<>();
		matches.add(pl("A", "B", date(1, 1), 1, 0));
		HistoricalFeatureDataset opening = builder.build(matches);
		assertThat(opening.rowsWithMissingTeamHistory()).isEqualTo(1);
		assertThat(opening.rowsWithFullLast5History()).isZero();
	}

	private HistoricalMatchRecord pl(String home, String away, LocalDate date, int homeGoals, int awayGoals) {
		return match(CanonicalCompetition.PREMIER_LEAGUE, S23, home, away, date, homeGoals, awayGoals);
	}

	private HistoricalMatchRecord match(
			CanonicalCompetition competition,
			FootballSeason season,
			String home,
			String away,
			LocalDate date,
			int homeGoals,
			int awayGoals) {
		return new HistoricalMatchRecord(
				HistoricalSource.FOOTBALL_DATA_UK,
				competition,
				season,
				date,
				null,
				home,
				away,
				new MatchScore(homeGoals, awayGoals),
				nextRow++,
				null);
	}

	private static LocalDate date(int month, int day) {
		return LocalDate.of(2024, month, day);
	}

	private static PreMatchFeatures features(HistoricalFeatureDataset dataset, LocalDate date, String homeTeam) {
		return features(dataset.rows(), date, homeTeam);
	}

	private static PreMatchFeatures features(List<HistoricalModelRow> rows, LocalDate date, String homeTeam) {
		return rows.stream()
				.filter(row -> row.matchDate().equals(date) && row.homeTeam().equals(homeTeam))
				.findFirst()
				.orElseThrow()
				.features();
	}
}
