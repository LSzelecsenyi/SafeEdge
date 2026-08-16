package com.safeedge.settlement;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import java.math.BigDecimal;
import java.util.List;

final class SettlementFixtures {

	private SettlementFixtures() {
	}

	static BettingMarket asian(String homeLine) {
		BigDecimal line = decimal(homeLine);
		return market(
				MarketType.ASIAN_HANDICAP,
				line,
				selection(SelectionType.HOME, line),
				selection(SelectionType.AWAY, line.negate()));
	}

	static BettingMarket european(String homeLine) {
		return market(
				MarketType.EUROPEAN_HANDICAP,
				decimal(homeLine),
				selection(SelectionType.HOME, null),
				selection(SelectionType.DRAW, null),
				selection(SelectionType.AWAY, null));
	}

	static BettingMarket doubleChance() {
		return market(
				MarketType.DOUBLE_CHANCE,
				null,
				selection(SelectionType.HOME_OR_DRAW, null),
				selection(SelectionType.HOME_OR_AWAY, null),
				selection(SelectionType.DRAW_OR_AWAY, null));
	}

	static BettingMarket market(MarketType type, BigDecimal line, BettingSelection... selections) {
		return new BettingMarket(
				"TEST",
				"market-1",
				null,
				"test-market",
				null,
				null,
				null,
				type,
				line,
				List.of(selections));
	}

	static BettingSelection selection(SelectionType type, BigDecimal line) {
		return new BettingSelection("TEST", 1, 1, "test-selection", type, line, BigDecimal.ONE);
	}

	static BettingSelection asianSelection(BettingMarket market, SelectionType type) {
		return market.selections().stream()
				.filter(selection -> selection.selectionType() == type)
				.findFirst()
				.orElseThrow();
	}

	static BigDecimal decimal(String value) {
		return value == null ? null : new BigDecimal(value);
	}

}
