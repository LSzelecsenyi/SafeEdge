package com.safeedge.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.SelectionType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DoubleChanceSettlementTest {

	private final SettlementEngine engine = new SettlementEngine();

	@ParameterizedTest(name = "{0} on {1}-{2} → {3}")
	@CsvSource({
			"HOME_OR_DRAW, 1, 0, WIN",
			"HOME_OR_DRAW, 0, 0, WIN",
			"HOME_OR_DRAW, 0, 1, LOSS",
			"HOME_OR_AWAY, 1, 0, WIN",
			"HOME_OR_AWAY, 0, 0, LOSS",
			"HOME_OR_AWAY, 0, 1, WIN",
			"DRAW_OR_AWAY, 1, 0, LOSS",
			"DRAW_OR_AWAY, 0, 0, WIN",
			"DRAW_OR_AWAY, 0, 1, WIN"
	})
	void settlesUnadjustedMatchResult(
			SelectionType selectionType, int homeGoals, int awayGoals, SettlementResult expected) {
		BettingMarket market = SettlementFixtures.doubleChance();
		assertThat(engine.settle(
						market,
						selection(market, selectionType),
						new MatchScore(homeGoals, awayGoals)))
				.isEqualTo(expected);
	}

	private static com.safeedge.event.domain.BettingSelection selection(
			BettingMarket market, SelectionType type) {
		return market.selections().stream()
				.filter(selection -> selection.selectionType() == type)
				.findFirst()
				.orElseThrow();
	}

}
