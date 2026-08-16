package com.safeedge.historical.diagnostics;

import com.safeedge.backtest.HistoricalBettingOpportunity;
import com.safeedge.backtest.HistoricalEventResult;
import com.safeedge.candidate.ScoreProbability;
import com.safeedge.candidate.ScoreProbabilityDistribution;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.CanonicalCompetition;
import com.safeedge.historical.domain.FootballSeason;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.evaluation.HistoricalPredictionSnapshot;
import com.safeedge.historical.evaluation.HistoricalWalkForwardDataset;
import com.safeedge.historical.evaluation.WalkForwardBuildStats;
import com.safeedge.settlement.MatchScore;
import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.GeneralizedKellyCalculator;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

final class BaselineDiagnosticsFixtures {

	static final FootballSeason S19 = new FootballSeason(2019, 2020);
	static final FootballSeason S20 = new FootballSeason(2020, 2021);
	static final LocalDate D19 = LocalDate.of(2019, 8, 10);
	static final LocalDate D20 = LocalDate.of(2020, 8, 15);
	static final SettlementProbabilityDistribution BINARY_60 =
			SettlementProbabilityDistribution.binary(new BigDecimal("0.60"));
	private static final GeneralizedKellyCalculator EXPECTED_RETURN = new GeneralizedKellyCalculator();

	private BaselineDiagnosticsFixtures() {
	}

	static HistoricalBettingOpportunity opportunity(
			String opportunityId,
			String eventId,
			LocalDate date,
			SelectionType side,
			String line,
			String odds,
			String edge) {
		return opportunity(opportunityId, eventId, date, side, line, odds, edge, BINARY_60);
	}

	static HistoricalBettingOpportunity opportunity(
			String opportunityId,
			String eventId,
			LocalDate date,
			SelectionType side,
			String line,
			String odds,
			String edge,
			SettlementProbabilityDistribution probabilities) {
		BigDecimal homeLine = side == SelectionType.HOME ? new BigDecimal(line) : new BigDecimal(line).negate();
		BettingMarket market = asianMarket(homeLine.toPlainString(), odds);
		BettingSelection selection = market.selections().stream()
				.filter(item -> item.selectionType() == side)
				.findFirst()
				.orElseThrow();
		if (side == SelectionType.AWAY && selection.line().compareTo(new BigDecimal(line)) != 0) {
			throw new IllegalStateException("away line mismatch");
		}
		BettingOpportunity bettingOpportunity = new BettingOpportunity(
				opportunityId,
				eventId,
				"PREMIER_LEAGUE",
				date,
				new BigDecimal(odds),
				new BigDecimal(edge),
				probabilities);
		return new HistoricalBettingOpportunity(
				bettingOpportunity, market, selection, date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
	}

	static HistoricalBettingOpportunity priced(
			String opportunityId,
			String eventId,
			LocalDate date,
			SelectionType side,
			String line,
			String odds,
			SettlementProbabilityDistribution probabilities) {
		BigDecimal edge = EXPECTED_RETURN.expectedReturnRate(new BigDecimal(odds), probabilities);
		return opportunity(opportunityId, eventId, date, side, line, odds, edge.toPlainString(), probabilities);
	}

	static HistoricalBettingOpportunity twoWay(
			String opportunityId,
			String eventId,
			LocalDate date,
			SelectionType side,
			String homeLine,
			String homeOdds,
			String awayOdds,
			SettlementProbabilityDistribution probabilities) {
		BigDecimal line = new BigDecimal(homeLine);
		BigDecimal homePrice = new BigDecimal(homeOdds);
		BigDecimal awayPrice = new BigDecimal(awayOdds);
		BettingSelection homeSelection =
				new BettingSelection("TEST", 1, 1, "home", SelectionType.HOME, line, homePrice);
		BettingSelection awaySelection =
				new BettingSelection("TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), awayPrice);
		BettingMarket market = new BettingMarket(
				"TEST",
				"market-" + homeLine + "-" + eventId,
				null,
				"asian",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				line,
				List.of(homeSelection, awaySelection));
		BettingSelection selection = side == SelectionType.HOME ? homeSelection : awaySelection;
		BigDecimal odds = selection.odds();
		BigDecimal edge = EXPECTED_RETURN.expectedReturnRate(odds, probabilities);
		BettingOpportunity bettingOpportunity = new BettingOpportunity(
				opportunityId, eventId, "PREMIER_LEAGUE", date, odds, edge, probabilities);
		return new HistoricalBettingOpportunity(
				bettingOpportunity, market, selection, date.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
	}

	static HistoricalBettingOpportunity home(
			String opportunityId, String eventId, LocalDate date, String line, String odds, String edge) {
		return opportunity(opportunityId, eventId, date, SelectionType.HOME, line, odds, edge);
	}

	static HistoricalBettingOpportunity away(
			String opportunityId, String eventId, LocalDate date, String line, String odds, String edge) {
		return opportunity(opportunityId, eventId, date, SelectionType.AWAY, line, odds, edge);
	}

	static BettingMarket asianMarket(String homeLine, String odds) {
		BigDecimal line = new BigDecimal(homeLine);
		BigDecimal price = new BigDecimal(odds);
		BettingSelection homeSelection =
				new BettingSelection("TEST", 1, 1, "home", SelectionType.HOME, line, price);
		BettingSelection awaySelection =
				new BettingSelection("TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), price);
		return new BettingMarket(
				"TEST",
				"market-" + homeLine,
				null,
				"asian",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				line,
				List.of(homeSelection, awaySelection));
	}

	static HistoricalEventResult result(String eventId, LocalDate matchDate, int homeGoals, int awayGoals) {
		Instant settlementAt = matchDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
		return new HistoricalEventResult(eventId, settlementAt, new MatchScore(homeGoals, awayGoals));
	}

	static HistoricalWalkForwardDataset dataset(
			List<HistoricalBettingOpportunity> opportunities, List<HistoricalEventResult> results) {
		return dataset(CanonicalCompetition.PREMIER_LEAGUE, opportunities, results);
	}

	static HistoricalWalkForwardDataset dataset(
			CanonicalCompetition competition,
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> results) {
		int home = 0;
		int away = 0;
		int pos = 0;
		int zero = 0;
		int neg = 0;
		for (HistoricalBettingOpportunity opportunity : opportunities) {
			if (opportunity.selection().selectionType() == SelectionType.HOME) {
				home++;
			}
			else {
				away++;
			}
			int sign = opportunity.opportunity().edge().compareTo(BigDecimal.ZERO);
			if (sign > 0) {
				pos++;
			}
			else if (sign < 0) {
				neg++;
			}
			else {
				zero++;
			}
		}
		WalkForwardBuildStats stats = new WalkForwardBuildStats(
				competition,
				2014,
				2019,
				2023,
				HistoricalQuoteSource.MARKET_AVERAGE,
				opportunities.size(),
				results.size(),
				0,
				0,
				0,
				0,
				results.size(),
				results.size(),
				opportunities.size(),
				home,
				away,
				pos,
				zero,
				neg,
				0,
				0,
				null,
				null);
		return new HistoricalWalkForwardDataset(stats, opportunities, results);
	}

	static HistoricalPredictionSnapshot prediction(
			String eventId,
			FootballSeason season,
			LocalDate date,
			MatchScore actual,
			ScoreProbabilityDistribution distribution) {
		BigDecimal homeXg = BigDecimal.ZERO;
		BigDecimal awayXg = BigDecimal.ZERO;
		for (ScoreProbability entry : distribution.entries()) {
			homeXg = homeXg.add(entry.probability().multiply(BigDecimal.valueOf(entry.score().homeGoals())));
			awayXg = awayXg.add(entry.probability().multiply(BigDecimal.valueOf(entry.score().awayGoals())));
		}
		return new HistoricalPredictionSnapshot(eventId, season, date, homeXg, awayXg, distribution, actual);
	}

	static ScoreProbabilityDistribution oneNilShape() {
		return ScoreProbabilityDistribution.of(
				new ScoreProbability(new MatchScore(1, 0), new BigDecimal("0.40")),
				new ScoreProbability(new MatchScore(0, 0), new BigDecimal("0.30")),
				new ScoreProbability(new MatchScore(0, 1), new BigDecimal("0.30")));
	}

	static ScoreProbabilityDistribution blowoutShape() {
		return ScoreProbabilityDistribution.of(
				new ScoreProbability(new MatchScore(3, 0), new BigDecimal("0.20")),
				new ScoreProbability(new MatchScore(2, 0), new BigDecimal("0.20")),
				new ScoreProbability(new MatchScore(1, 0), new BigDecimal("0.20")),
				new ScoreProbability(new MatchScore(0, 0), new BigDecimal("0.20")),
				new ScoreProbability(new MatchScore(0, 2), new BigDecimal("0.20")));
	}
}
