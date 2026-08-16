package com.safeedge.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.SelectionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AsianHandicapSettlementTest {

	private final SettlementEngine engine = new SettlementEngine();

	@ParameterizedTest(name = "HOME {1} on {2}-{3} → {4}")
	@CsvSource({
			"HOME, -2, 3, 0, WIN",
			"HOME, -2, 2, 0, PUSH",
			"HOME, -2, 2, 1, LOSS",
			"HOME, -1, 2, 0, WIN",
			"HOME, -1, 2, 1, PUSH",
			"HOME, -1, 1, 1, LOSS",
			"HOME, -1, 0, 1, LOSS",
			"HOME,  0, 1, 0, WIN",
			"HOME,  0, 0, 0, PUSH",
			"HOME,  0, 0, 1, LOSS",
			"HOME,  1, 0, 0, WIN",
			"HOME,  1, 0, 1, PUSH",
			"HOME,  1, 0, 2, LOSS",
			"HOME,  2, 0, 1, WIN",
			"HOME,  2, 0, 2, PUSH",
			"HOME,  2, 0, 3, LOSS"
	})
	void integerLines(
			SelectionType selectionType,
			String line,
			int homeGoals,
			int awayGoals,
			SettlementResult expected) {
		assertSettles(selectionType, line, homeGoals, awayGoals, expected);
	}

	@ParameterizedTest(name = "AWAY {1} on {2}-{3} → {4}")
	@CsvSource({
			"AWAY,  2, 0, 1, WIN",
			"AWAY,  2, 2, 0, PUSH",
			"AWAY,  2, 3, 0, LOSS",
			"AWAY,  1, 0, 1, WIN",
			"AWAY,  1, 1, 1, WIN",
			"AWAY,  1, 2, 1, PUSH",
			"AWAY,  1, 3, 1, LOSS",
			"AWAY,  0, 0, 1, WIN",
			"AWAY,  0, 0, 0, PUSH",
			"AWAY,  0, 1, 0, LOSS",
			"AWAY, -1, 0, 2, WIN",
			"AWAY, -1, 1, 2, PUSH",
			"AWAY, -1, 1, 1, LOSS",
			"AWAY, -2, 0, 3, WIN",
			"AWAY, -2, 0, 2, PUSH",
			"AWAY, -2, 1, 2, LOSS"
	})
	void integerAwayLines(
			SelectionType selectionType,
			String line,
			int homeGoals,
			int awayGoals,
			SettlementResult expected) {
		assertSettles(selectionType, line, homeGoals, awayGoals, expected);
	}

	@ParameterizedTest(name = "{0} {1} on {2}-{3} → {4}")
	@CsvSource({
			"HOME, -1.5, 2, 0, WIN",
			"HOME, -1.5, 2, 1, LOSS",
			"HOME, -0.5, 1, 0, WIN",
			"HOME, -0.5, 0, 0, LOSS",
			"HOME,  0.5, 0, 0, WIN",
			"HOME,  0.5, 0, 1, LOSS",
			"HOME,  1.5, 0, 1, WIN",
			"HOME,  1.5, 0, 2, LOSS",
			"AWAY,  1.5, 2, 1, WIN",
			"AWAY,  1.5, 3, 1, LOSS",
			"AWAY,  0.5, 0, 0, WIN",
			"AWAY,  0.5, 1, 0, LOSS",
			"AWAY, -0.5, 0, 1, WIN",
			"AWAY, -0.5, 0, 0, LOSS",
			"AWAY, -1.5, 1, 3, WIN",
			"AWAY, -1.5, 1, 2, LOSS"
	})
	void halfLines(
			SelectionType selectionType,
			String line,
			int homeGoals,
			int awayGoals,
			SettlementResult expected) {
		assertSettles(selectionType, line, homeGoals, awayGoals, expected);
	}

	@ParameterizedTest(name = "{0} {1} on {2}-{3} → {4}")
	@CsvSource({
			"AWAY,  1.25, 2, 1, HALF_WIN",
			"AWAY,  1.25, 3, 1, LOSS",
			"AWAY,  1.25, 1, 1, WIN",
			"AWAY,  0.75, 2, 1, HALF_LOSS",
			"AWAY,  0.75, 1, 1, WIN",
			"HOME, -1.25, 2, 1, HALF_LOSS",
			"HOME, -1.25, 2, 0, WIN",
			"HOME, -0.75, 2, 1, HALF_WIN",
			"HOME, -0.75, 1, 1, LOSS",
			"AWAY,  0.25, 0, 0, HALF_WIN",
			"HOME, -0.25, 0, 0, HALF_LOSS",
			"HOME, -1.75, 2, 0, HALF_WIN",
			"HOME, -1.75, 2, 1, LOSS",
			"HOME, -1.75, 3, 0, WIN",
			"AWAY,  1.75, 2, 0, HALF_LOSS",
			"AWAY,  1.75, 3, 1, HALF_LOSS",
			"AWAY,  1.75, 4, 1, LOSS",
			"AWAY,  1.75, 1, 1, WIN",
			"HOME,  0.25, 0, 0, HALF_WIN",
			"AWAY, -0.25, 0, 0, HALF_LOSS",
			"HOME,  0.75, 0, 0, WIN",
			"HOME,  0.75, 0, 1, HALF_LOSS",
			"AWAY, -0.75, 1, 2, HALF_WIN",
			"AWAY, -0.75, 1, 1, LOSS",
			"HOME,  1.25, 0, 1, HALF_WIN",
			"HOME,  1.25, 0, 2, LOSS",
			"AWAY, -1.25, 1, 2, HALF_LOSS",
			"AWAY, -1.25, 0, 2, WIN"
	})
	void quarterLines(
			SelectionType selectionType,
			String line,
			int homeGoals,
			int awayGoals,
			SettlementResult expected) {
		assertSettles(selectionType, line, homeGoals, awayGoals, expected);
	}

	@Test
	void awayPlusOneLosingByOneIsPush() {
		assertSettles(SelectionType.AWAY, "1", 2, 1, SettlementResult.PUSH);
	}

	@Test
	void awayPlusOneQuarterLosingByOneIsHalfWin() {
		assertSettles(SelectionType.AWAY, "1.25", 2, 1, SettlementResult.HALF_WIN);
	}

	@Test
	void awayPlusThreeQuartersLosingByOneIsHalfLoss() {
		assertSettles(SelectionType.AWAY, "0.75", 2, 1, SettlementResult.HALF_LOSS);
	}

	@Test
	void homeMinusOneQuarterWinningByOneIsHalfLoss() {
		assertSettles(SelectionType.HOME, "-1.25", 2, 1, SettlementResult.HALF_LOSS);
	}

	@Test
	void homeMinusThreeQuartersWinningByOneIsHalfWin() {
		assertSettles(SelectionType.HOME, "-0.75", 2, 1, SettlementResult.HALF_WIN);
	}

	@ParameterizedTest(name = "HOME {0} and AWAY opposite are complementary on {1}-{2}")
	@CsvSource({
			"-2, 3, 0",
			"-2, 2, 0",
			"-1, 2, 1",
			"-1, 1, 1",
			"0, 1, 0",
			"0, 0, 0",
			"1, 0, 1",
			"-1.5, 2, 0",
			"-0.5, 0, 0",
			"0.5, 0, 0",
			"1.5, 0, 2",
			"-1.25, 2, 1",
			"-0.75, 2, 1",
			"-0.25, 0, 0",
			"0.25, 0, 0",
			"0.75, 0, 1",
			"1.25, 2, 1",
			"-1.75, 2, 0",
			"1.75, 3, 1"
	})
	void homeAndAwayLinesAreComplementary(String homeLine, int homeGoals, int awayGoals) {
		BettingMarket market = SettlementFixtures.asian(homeLine);
		MatchScore score = new MatchScore(homeGoals, awayGoals);
		SettlementResult homeResult = engine.settle(
				market, SettlementFixtures.asianSelection(market, SelectionType.HOME), score);
		SettlementResult awayResult = engine.settle(
				market, SettlementFixtures.asianSelection(market, SelectionType.AWAY), score);
		assertThat(awayResult).isEqualTo(complement(homeResult));
	}

	private void assertSettles(
			SelectionType selectionType,
			String line,
			int homeGoals,
			int awayGoals,
			SettlementResult expected) {
		BettingMarket market = selectionType == SelectionType.HOME
				? SettlementFixtures.asian(line)
				: SettlementFixtures.asian(new java.math.BigDecimal(line).negate().toPlainString());
		assertThat(engine.settle(
						market,
						SettlementFixtures.asianSelection(market, selectionType),
						new MatchScore(homeGoals, awayGoals)))
				.isEqualTo(expected);
	}

	private static SettlementResult complement(SettlementResult result) {
		return switch (result) {
			case WIN -> SettlementResult.LOSS;
			case HALF_WIN -> SettlementResult.HALF_LOSS;
			case PUSH -> SettlementResult.PUSH;
			case HALF_LOSS -> SettlementResult.HALF_WIN;
			case LOSS -> SettlementResult.WIN;
		};
	}

}
