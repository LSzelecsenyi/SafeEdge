package com.safeedge.settlement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SettlementEngineValidationTest {

	private final SettlementEngine engine = new SettlementEngine();

	@Test
	void negativeScoreIsRejected() {
		assertThatThrownBy(() -> new MatchScore(-1, 0))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("non-negative");
		assertThatThrownBy(() -> new MatchScore(0, -1)).isInstanceOf(SettlementException.class);
	}

	@Test
	void asianHandicapRequiresMarketLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.ASIAN_HANDICAP,
				null,
				SettlementFixtures.selection(SelectionType.HOME, new BigDecimal("-1")));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("market line");
	}

	@Test
	void asianHandicapRequiresSelectionLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.ASIAN_HANDICAP,
				new BigDecimal("-1"),
				SettlementFixtures.selection(SelectionType.HOME, null));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("selection line");
	}

	@Test
	void asianHandicapRejectsUnsupportedIncrement() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.ASIAN_HANDICAP,
				new BigDecimal("0.3"),
				SettlementFixtures.selection(SelectionType.HOME, new BigDecimal("0.3")),
				SettlementFixtures.selection(SelectionType.AWAY, new BigDecimal("-0.3")));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("0.25");
	}

	@Test
	void asianHandicapRejectsDrawSelection() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.ASIAN_HANDICAP,
				new BigDecimal("-1"),
				SettlementFixtures.selection(SelectionType.DRAW, new BigDecimal("-1")));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("HOME or AWAY");
	}

	@Test
	void asianHandicapRejectsInconsistentAwayLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.ASIAN_HANDICAP,
				new BigDecimal("-1.25"),
				SettlementFixtures.selection(SelectionType.HOME, new BigDecimal("-1.25")),
				SettlementFixtures.selection(SelectionType.AWAY, new BigDecimal("1.00")));
		BettingSelection away = market.selections().get(1);
		assertThatThrownBy(() -> engine.settle(market, away, new MatchScore(2, 1)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("inconsistent");
	}

	@Test
	void europeanHandicapRequiresMarketLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.EUROPEAN_HANDICAP,
				null,
				SettlementFixtures.selection(SelectionType.HOME, null));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("European handicap market line");
	}

	@Test
	void europeanHandicapRejectsFractionalLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.EUROPEAN_HANDICAP,
				new BigDecimal("-0.5"),
				SettlementFixtures.selection(SelectionType.HOME, null));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("whole number");
	}

	@Test
	void doubleChanceRejectsMarketLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.DOUBLE_CHANCE,
				new BigDecimal("-1"),
				SettlementFixtures.selection(SelectionType.HOME_OR_DRAW, null));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("Double chance market line");
	}

	@Test
	void doubleChanceRejectsHomeSelection() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.DOUBLE_CHANCE,
				null,
				SettlementFixtures.selection(SelectionType.HOME, null));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("two-way combination");
	}

	@Test
	void doubleChanceRejectsSelectionLine() {
		BettingMarket market = SettlementFixtures.market(
				MarketType.DOUBLE_CHANCE,
				null,
				SettlementFixtures.selection(SelectionType.HOME_OR_DRAW, new BigDecimal("0")));
		assertThatThrownBy(() -> engine.settle(market, market.selections().getFirst(), new MatchScore(1, 0)))
				.isInstanceOf(SettlementException.class)
				.hasMessageContaining("selection line");
	}

	@Test
	void nullInputsAreRejected() {
		BettingMarket market = SettlementFixtures.asian("-1");
		BettingSelection selection = market.selections().getFirst();
		MatchScore score = new MatchScore(1, 0);
		assertThatThrownBy(() -> engine.settle(null, selection, score)).isInstanceOf(SettlementException.class);
		assertThatThrownBy(() -> engine.settle(market, null, score)).isInstanceOf(SettlementException.class);
		assertThatThrownBy(() -> engine.settle(market, selection, null)).isInstanceOf(SettlementException.class);
	}

}
