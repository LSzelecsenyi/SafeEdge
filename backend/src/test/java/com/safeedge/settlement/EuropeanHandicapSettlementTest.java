package com.safeedge.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.SelectionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EuropeanHandicapSettlementTest {

	private final SettlementEngine engine = new SettlementEngine();

	@ParameterizedTest(name = "EH {0}, score {1}-{2}, {3} → {4}")
	@CsvSource({
			"-1, 2, 0, HOME, WIN",
			"-1, 2, 0, DRAW, LOSS",
			"-1, 2, 0, AWAY, LOSS",
			"-1, 2, 1, HOME, LOSS",
			"-1, 2, 1, DRAW, WIN",
			"-1, 2, 1, AWAY, LOSS",
			"-1, 1, 1, HOME, LOSS",
			"-1, 1, 1, DRAW, LOSS",
			"-1, 1, 1, AWAY, WIN",
			"1, 0, 0, HOME, WIN",
			"1, 0, 0, DRAW, LOSS",
			"1, 0, 0, AWAY, LOSS",
			"1, 0, 1, HOME, LOSS",
			"1, 0, 1, DRAW, WIN",
			"1, 0, 1, AWAY, LOSS",
			"1, 0, 2, HOME, LOSS",
			"1, 0, 2, DRAW, LOSS",
			"1, 0, 2, AWAY, WIN",
			"0, 1, 0, HOME, WIN",
			"0, 0, 0, DRAW, WIN",
			"0, 0, 1, AWAY, WIN"
	})
	void settlesThreeWayOutcomes(
			String marketLine,
			int homeGoals,
			int awayGoals,
			SelectionType selectionType,
			SettlementResult expected) {
		BettingMarket market = SettlementFixtures.european(marketLine);
		assertThat(engine.settle(
						market,
						selection(market, selectionType),
						new MatchScore(homeGoals, awayGoals)))
				.isEqualTo(expected);
	}

	@Test
	void europeanMinusOneScoreTwoOneDrawIsWin() {
		BettingMarket market = SettlementFixtures.european("-1");
		assertThat(engine.settle(
						market,
						selection(market, SelectionType.DRAW),
						new MatchScore(2, 1)))
				.isEqualTo(SettlementResult.WIN);
	}

	private static com.safeedge.event.domain.BettingSelection selection(
			BettingMarket market, SelectionType type) {
		return market.selections().stream()
				.filter(selection -> selection.selectionType() == type)
				.findFirst()
				.orElseThrow();
	}

}
