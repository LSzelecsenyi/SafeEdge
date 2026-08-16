package com.safeedge.historical.evaluation;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.features.HistoricalMatchRecord;
import com.safeedge.probability.ProbabilityModelConfig;
import com.safeedge.settlement.MatchScore;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class HistoricalWalkForwardFixtures {

	static final FootballSeason S22 = new FootballSeason(2022, 2023);
	static final FootballSeason S23 = new FootballSeason(2023, 2024);
	static final ProbabilityModelConfig MIN1 = new ProbabilityModelConfig(180, 10, 1);
	static final BigDecimal LINE_ZERO = BigDecimal.ZERO;
	static final BigDecimal HOME_ODDS = new BigDecimal("10.00");
	static final BigDecimal AWAY_ODDS = new BigDecimal("1.05");

	private HistoricalWalkForwardFixtures() {
	}

	static WalkForwardEvaluationRequest eval2023(HistoricalQuoteSource quoteSource) {
		return new WalkForwardEvaluationRequest(
				CanonicalCompetition.PREMIER_LEAGUE, 2022, 2023, 2023, quoteSource, MIN1);
	}

	static WalkForwardEvaluationRequest eval2023NoWarmup(HistoricalQuoteSource quoteSource) {
		return new WalkForwardEvaluationRequest(
				CanonicalCompetition.PREMIER_LEAGUE, 2023, 2023, 2023, quoteSource, MIN1);
	}

	static HistoricalMatchRecord match(
			FootballSeason season,
			String home,
			String away,
			LocalDate date,
			int homeGoals,
			int awayGoals,
			int sourceRowNumber) {
		return new HistoricalMatchRecord(
				HistoricalSource.FOOTBALL_DATA_UK,
				CanonicalCompetition.PREMIER_LEAGUE,
				season,
				date,
				null,
				home,
				away,
				new MatchScore(homeGoals, awayGoals),
				sourceRowNumber,
				null);
	}

	static HistoricalAhQuoteSnapshot quote(
			HistoricalMatchRecord match,
			HistoricalQuoteSource source,
			BigDecimal line,
			BigDecimal homeOdds,
			BigDecimal awayOdds) {
		String eventId = HistoricalWalkForwardIdentities.eventId(match);
		return new HistoricalAhQuoteSnapshot(eventId, source, line, homeOdds, awayOdds);
	}

	static Map<String, HistoricalAhQuoteSnapshot> quotes(HistoricalAhQuoteSnapshot... snapshots) {
		Map<String, HistoricalAhQuoteSnapshot> map = new HashMap<>();
		for (HistoricalAhQuoteSnapshot snapshot : snapshots) {
			map.put(snapshot.eventId(), snapshot);
		}
		return map;
	}

	/**
	 * One prior home match for {@code homeTeam} and one prior away match for
	 * {@code awayTeam} on dates before {@code before}.
	 */
	static List<HistoricalMatchRecord> venueWarmup(
			FootballSeason season, String homeTeam, String awayTeam, LocalDate before, int rowStart) {
		List<HistoricalMatchRecord> matches = new ArrayList<>();
		matches.add(match(season, homeTeam, homeTeam + "Opp", before.minusDays(14), 2, 1, rowStart));
		matches.add(match(season, awayTeam + "Opp", awayTeam, before.minusDays(7), 1, 2, rowStart + 1));
		return matches;
	}

	static List<HistoricalMatchRecord> strongHomeWarmup(FootballSeason season, LocalDate before, int rowStart) {
		List<HistoricalMatchRecord> matches = new ArrayList<>();
		matches.add(match(season, "H", "X", before.minusDays(21), 3, 1, rowStart));
		matches.add(match(season, "Y", "A", before.minusDays(14), 3, 1, rowStart + 1));
		matches.add(match(season, "C", "Z", before.minusDays(10), 1, 1, rowStart + 2));
		return matches;
	}

	static StrategyConfig flatMinEdge(String minimumEdge) {
		return flat("0.01", minimumEdge);
	}

	static StrategyConfig flat(String flatStakeRate, String minimumEdge) {
		return new StrategyConfig(
				false,
				BigDecimal.ZERO,
				StakingMode.FLAT_STAKE,
				null,
				new BigDecimal(flatStakeRate),
				new BigDecimal("0.02"),
				new BigDecimal(minimumEdge),
				new BigDecimal("0.50"),
				new BigDecimal("0.50"),
				BigDecimal.ONE,
				new BigDecimal("0.05"),
				new BigDecimal("0.08"),
				new BigDecimal("0.50"),
				new BigDecimal("0.90"));
	}

	static HistoricalBettingOpportunity side(
			HistoricalWalkForwardDataset dataset, HistoricalMatchRecord match, SelectionType side) {
		String eventId = HistoricalWalkForwardIdentities.eventId(match);
		return dataset.opportunities().stream()
				.filter(opportunity -> opportunity.opportunity().eventId().equals(eventId)
						&& opportunity.selection().selectionType() == side)
				.findFirst()
				.orElseThrow();
	}
}
