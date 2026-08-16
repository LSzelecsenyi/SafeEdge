package com.safeedge.backtest;

import com.safeedge.bankroll.OwnerId;
import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.MatchScore;
import com.safeedge.strategy.BettingOpportunity;
import com.safeedge.strategy.SettlementProbabilityDistribution;
import com.safeedge.strategy.StakingMode;
import com.safeedge.strategy.StrategyConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

final class BacktestFixtures {

	static final OwnerId OWNER = new OwnerId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
	static final LocalDate DAY = LocalDate.of(2026, 8, 16);
	static final LocalDate NEXT_DAY = DAY.plusDays(1);
	static final BigDecimal STARTING = new BigDecimal("100000");
	static final SettlementProbabilityDistribution BINARY_60 =
			SettlementProbabilityDistribution.binary(new BigDecimal("0.60"));

	static final Instant T10 = Instant.parse("2026-08-16T10:00:00Z");
	static final Instant T1030 = Instant.parse("2026-08-16T10:30:00Z");
	static final Instant T11 = Instant.parse("2026-08-16T11:00:00Z");
	static final Instant T1130 = Instant.parse("2026-08-16T11:30:00Z");
	static final Instant T12 = Instant.parse("2026-08-16T12:00:00Z");
	static final Instant T1201 = Instant.parse("2026-08-16T12:01:00Z");
	static final Instant T1205 = Instant.parse("2026-08-16T12:05:00Z");
	static final Instant T1230 = Instant.parse("2026-08-16T12:30:00Z");
	static final Instant T13 = Instant.parse("2026-08-16T13:00:00Z");
	static final Instant T14 = Instant.parse("2026-08-16T14:00:00Z");
	static final Instant T18 = Instant.parse("2026-08-16T18:00:00Z");
	static final Instant T1830 = Instant.parse("2026-08-16T18:30:00Z");

	private BacktestFixtures() {
	}

	static StrategyConfig flatTwoPercent() {
		return flat(
				false,
				"0",
				"0.02",
				"0.02",
				"0.03",
				"0.03",
				"0.05",
				"0.10",
				"0.10",
				"0.15",
				"0.50",
				"0.20");
	}

	static StrategyConfig leagueCapConfig() {
		return flat(
				false,
				"0",
				"0.02",
				"0.02",
				"0.03",
				"0.025",
				"0.03",
				"0.10",
				"0.10",
				"0.15",
				"0.50",
				"0.20");
	}

	static StrategyConfig highCapacityFlat(String flatRate, String maxStake, String stop) {
		return flat(
				false,
				"0",
				flatRate,
				maxStake,
				"0.03",
				"0.50",
				"0.50",
				"1",
				"0.05",
				"0.08",
				"0.50",
				stop);
	}

	static StrategyConfig reductionConfig() {
		return flat(
				false,
				"0",
				"0.10",
				"0.10",
				"0.03",
				"0.50",
				"0.50",
				"1",
				"0.05",
				"0.10",
				"0.50",
				"0.50");
	}

	static StrategyConfig vaultOnFlat() {
		return flat(
				true,
				"0.30",
				"0.02",
				"0.02",
				"0.03",
				"0.03",
				"0.05",
				"0.10",
				"0.10",
				"0.15",
				"0.50",
				"0.20");
	}

	static StrategyConfig flat(
			boolean vaultEnabled,
			String vaultSweepRate,
			String flatStakeRate,
			String maxStakeRate,
			String minimumEdge,
			String maxMatchExposure,
			String maxLeagueExposure,
			String maxDailyExposure,
			String drawdownWarning,
			String drawdownReduction,
			String drawdownMultiplier,
			String drawdownStop) {
		return new StrategyConfig(
				vaultEnabled,
				money(vaultSweepRate),
				StakingMode.FLAT_STAKE,
				null,
				money(flatStakeRate),
				money(maxStakeRate),
				money(minimumEdge),
				money(maxMatchExposure),
				money(maxLeagueExposure),
				money(maxDailyExposure),
				money(drawdownWarning),
				money(drawdownReduction),
				money(drawdownMultiplier),
				money(drawdownStop));
	}

	static HistoricalBettingOpportunity homeZero(
			String opportunityId,
			String eventId,
			String leagueId,
			Instant decisionAt,
			String odds,
			String edge) {
		return homeZero(opportunityId, eventId, leagueId, DAY, decisionAt, odds, edge);
	}

	static HistoricalBettingOpportunity homeZero(
			String opportunityId,
			String eventId,
			String leagueId,
			LocalDate bettingDate,
			Instant decisionAt,
			String odds,
			String edge) {
		return asianHome(opportunityId, eventId, leagueId, bettingDate, decisionAt, "0", odds, edge);
	}

	static HistoricalBettingOpportunity asianHome(
			String opportunityId,
			String eventId,
			String leagueId,
			LocalDate bettingDate,
			Instant decisionAt,
			String line,
			String odds,
			String edge) {
		BettingMarket market = asianMarket(line, odds);
		BettingSelection selection = market.selections().getFirst();
		BettingOpportunity opportunity = new BettingOpportunity(
				opportunityId,
				eventId,
				leagueId,
				bettingDate,
				new BigDecimal(odds),
				money(edge),
				BINARY_60);
		return new HistoricalBettingOpportunity(opportunity, market, selection, decisionAt);
	}

	static BettingMarket asianMarket(String homeLine, String odds) {
		BigDecimal line = new BigDecimal(homeLine);
		BigDecimal price = new BigDecimal(odds);
		BettingSelection home = new BettingSelection(
				"TEST", 1, 1, "home", SelectionType.HOME, line, price);
		BettingSelection away = new BettingSelection(
				"TEST", 2, 2, "away", SelectionType.AWAY, line.negate(), price);
		return new BettingMarket(
				"TEST",
				"market-1",
				null,
				"asian",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				line,
				List.of(home, away));
	}

	static HistoricalEventResult win(String eventId, Instant settlementAt) {
		return result(eventId, settlementAt, 1, 0);
	}

	static HistoricalEventResult loss(String eventId, Instant settlementAt) {
		return result(eventId, settlementAt, 0, 1);
	}

	static HistoricalEventResult draw(String eventId, Instant settlementAt) {
		return result(eventId, settlementAt, 0, 0);
	}

	static HistoricalEventResult result(String eventId, Instant settlementAt, int homeGoals, int awayGoals) {
		return new HistoricalEventResult(eventId, settlementAt, new MatchScore(homeGoals, awayGoals));
	}

	static BacktestRequest request(
			StrategyConfig config,
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> eventResults) {
		return request(config, opportunities, eventResults, null);
	}

	static BacktestRequest request(
			StrategyConfig config,
			List<HistoricalBettingOpportunity> opportunities,
			List<HistoricalEventResult> eventResults,
			Integer maxAcceptedBets) {
		return new BacktestRequest(OWNER, STARTING, config, opportunities, eventResults, maxAcceptedBets);
	}

	static BigDecimal money(String value) {
		return new BigDecimal(value);
	}

}
