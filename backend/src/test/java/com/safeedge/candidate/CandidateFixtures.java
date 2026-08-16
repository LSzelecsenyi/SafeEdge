package com.safeedge.candidate;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.settlement.MatchScore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class CandidateFixtures {

	static final LocalDate DATE = LocalDate.of(2026, 8, 16);
	static final CandidateContext CONTEXT = new CandidateContext("opp-1", "event-1", "league-1", DATE);
	static final BigDecimal ODDS_125 = new BigDecimal("1.25");
	static final BigDecimal ODDS_200 = new BigDecimal("2.00");

	private CandidateFixtures() {
	}

	static ScoreProbability score(int home, int away, String probability) {
		return new ScoreProbability(new MatchScore(home, away), new BigDecimal(probability));
	}

	static ScoreProbabilityDistribution distribution(ScoreProbability... entries) {
		return ScoreProbabilityDistribution.of(entries);
	}

	/**
	 * Home-by-2+ 0.15, home-by-1 0.20, draw-or-away 0.65.
	 */
	static ScoreProbabilityDistribution awayPlusOneShape() {
		return distribution(
				score(2, 0, "0.10"),
				score(3, 1, "0.05"),
				score(1, 0, "0.20"),
				score(0, 0, "0.25"),
				score(1, 1, "0.15"),
				score(0, 1, "0.25"));
	}

	static BettingMarket asianHomeLine(String homeLine, BigDecimal odds) {
		BigDecimal line = new BigDecimal(homeLine);
		BettingSelection home = selection(SelectionType.HOME, line, odds);
		BettingSelection away = selection(SelectionType.AWAY, line.negate(), odds);
		return market(MarketType.ASIAN_HANDICAP, line, home, away);
	}

	static BettingMarket european(String homeLine, BigDecimal odds) {
		return market(
				MarketType.EUROPEAN_HANDICAP,
				new BigDecimal(homeLine),
				selection(SelectionType.HOME, null, odds),
				selection(SelectionType.DRAW, null, odds),
				selection(SelectionType.AWAY, null, odds));
	}

	static BettingMarket doubleChance(BigDecimal odds) {
		return market(
				MarketType.DOUBLE_CHANCE,
				null,
				selection(SelectionType.HOME_OR_DRAW, null, odds),
				selection(SelectionType.HOME_OR_AWAY, null, odds),
				selection(SelectionType.DRAW_OR_AWAY, null, odds));
	}

	static BettingSelection selectionOf(BettingMarket market, SelectionType type) {
		return market.selections().stream()
				.filter(selection -> selection.selectionType() == type)
				.findFirst()
				.orElseThrow();
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

	static BettingSelection selection(SelectionType type, BigDecimal line, BigDecimal odds) {
		return new BettingSelection("TEST", 1, 1, "test-selection", type, line, odds);
	}

	static ScoreProbabilityDistribution binaryHomeWin(String winProbability) {
		BigDecimal win = new BigDecimal(winProbability);
		return distribution(
				score(1, 0, win.toPlainString()),
				score(0, 1, BigDecimal.ONE.subtract(win).toPlainString()));
	}

}
