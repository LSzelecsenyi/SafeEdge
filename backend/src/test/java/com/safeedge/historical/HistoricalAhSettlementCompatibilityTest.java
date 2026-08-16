package com.safeedge.historical;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.HistoricalAhQuoteDraft;
import com.safeedge.historical.domain.HistoricalObservationType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.settlement.MatchScore;
import com.safeedge.settlement.SettlementEngine;
import com.safeedge.settlement.SettlementResult;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoricalAhSettlementCompatibilityTest {

	private final SettlementEngine settlementEngine = new SettlementEngine();

	@Test
	void sourceMinusOneQuarterSettlesAsSafeEdgeHomeHalfLoss() {
		HistoricalAhQuoteDraft quote = new HistoricalAhQuoteDraft(
				HistoricalSource.FOOTBALL_DATA_UK,
				HistoricalQuoteSource.BET365,
				new BigDecimal("-1.25"),
				new BigDecimal("1.90"),
				new BigDecimal("2.00"),
				null,
				HistoricalObservationType.PRE_MATCH_SNAPSHOT,
				"B365AH",
				"B365AHH",
				"B365AHA",
				"-1.25",
				"1.90",
				"2.00");
		assertThat(quote.homeHandicapLine()).isEqualByComparingTo("-1.25");
		assertThat(quote.awayHandicapLine()).isEqualByComparingTo("1.25");
		BettingMarket market = market(quote);
		BettingSelection home = selection(market, SelectionType.HOME);
		BettingSelection away = selection(market, SelectionType.AWAY);
		MatchScore homeWinsByOne = new MatchScore(1, 0);
		assertThat(settlementEngine.settle(market, home, homeWinsByOne)).isEqualTo(SettlementResult.HALF_LOSS);
		assertThat(settlementEngine.settle(market, away, homeWinsByOne)).isEqualTo(SettlementResult.HALF_WIN);
	}

	private static BettingMarket market(HistoricalAhQuoteDraft quote) {
		return new BettingMarket(
				"FOOTBALL_DATA_UK",
				"hist-ah",
				null,
				"asian",
				null,
				null,
				null,
				MarketType.ASIAN_HANDICAP,
				quote.homeHandicapLine(),
				List.of(
						new BettingSelection(
								"FOOTBALL_DATA_UK",
								1,
								1,
								"home",
								SelectionType.HOME,
								quote.homeHandicapLine(),
								quote.homeOdds()),
						new BettingSelection(
								"FOOTBALL_DATA_UK",
								2,
								2,
								"away",
								SelectionType.AWAY,
								quote.awayHandicapLine(),
								quote.awayOdds())));
	}

	private static BettingSelection selection(BettingMarket market, SelectionType type) {
		return market.selections().stream()
				.filter(item -> item.selectionType() == type)
				.findFirst()
				.orElseThrow();
	}

}
