package com.safeedge.settlement;

import com.safeedge.event.domain.BettingMarket;
import com.safeedge.event.domain.BettingSelection;
import com.safeedge.event.domain.MarketType;
import com.safeedge.event.domain.SelectionType;
import java.math.BigDecimal;

public final class SettlementEngine {

	private static final BigDecimal QUARTER = new BigDecimal("0.25");
	private static final BigDecimal HALF = new BigDecimal("0.5");

	public SettlementResult settle(BettingMarket market, BettingSelection selection, MatchScore finalScore) {
		if (market == null) {
			throw new SettlementException("Betting market is required");
		}
		if (selection == null) {
			throw new SettlementException("Betting selection is required");
		}
		if (finalScore == null) {
			throw new SettlementException("Match score is required");
		}
		if (market.marketType() == null) {
			throw new SettlementException("Market type is required");
		}
		return switch (market.marketType()) {
			case ASIAN_HANDICAP -> settleAsian(market, selection, finalScore);
			case EUROPEAN_HANDICAP -> settleEuropean(market, selection, finalScore);
			case DOUBLE_CHANCE -> settleDoubleChance(market, selection, finalScore);
		};
	}

	private SettlementResult settleAsian(BettingMarket market, BettingSelection selection, MatchScore score) {
		SelectionType type = requireSelectionType(selection);
		if (type != SelectionType.HOME && type != SelectionType.AWAY) {
			throw new SettlementException("Asian handicap selection must be HOME or AWAY, not " + type);
		}
		BigDecimal marketLine = requireLine(market.line(), "Asian handicap market line is required");
		BigDecimal handicap = requireLine(selection.line(), "Asian handicap selection line is required");
		requireSupportedAsianIncrement(handicap);
		requireAsianLineConsistency(type, marketLine, handicap);

		int selectedGoalDifference = type == SelectionType.HOME
				? score.homeGoals() - score.awayGoals()
				: score.awayGoals() - score.homeGoals();

		if (isQuarterLine(handicap)) {
			return settleQuarterLine(selectedGoalDifference, handicap);
		}
		return settleWholeOrHalfLeg(selectedGoalDifference, handicap);
	}

	private SettlementResult settleQuarterLine(int selectedGoalDifference, BigDecimal handicap) {
		BigDecimal lowerLeg = handicap.subtract(QUARTER);
		BigDecimal upperLeg = handicap.add(QUARTER);
		SettlementResult lower = settleWholeOrHalfLeg(selectedGoalDifference, lowerLeg);
		SettlementResult upper = settleWholeOrHalfLeg(selectedGoalDifference, upperLeg);
		return combineQuarterLegs(lower, upper, handicap);
	}

	private SettlementResult settleWholeOrHalfLeg(int selectedGoalDifference, BigDecimal handicap) {
		BigDecimal adjusted = BigDecimal.valueOf(selectedGoalDifference).add(handicap);
		int comparison = adjusted.compareTo(BigDecimal.ZERO);
		if (comparison > 0) {
			return SettlementResult.WIN;
		}
		if (comparison == 0) {
			return SettlementResult.PUSH;
		}
		return SettlementResult.LOSS;
	}

	private SettlementResult combineQuarterLegs(
			SettlementResult first, SettlementResult second, BigDecimal handicap) {
		if (first == second) {
			return first;
		}
		if (isPair(first, second, SettlementResult.WIN, SettlementResult.PUSH)) {
			return SettlementResult.HALF_WIN;
		}
		if (isPair(first, second, SettlementResult.LOSS, SettlementResult.PUSH)) {
			return SettlementResult.HALF_LOSS;
		}
		throw new SettlementException(
				"Unsupported Asian quarter-line combination " + first + " + " + second + " for handicap " + handicap);
	}

	private SettlementResult settleEuropean(BettingMarket market, BettingSelection selection, MatchScore score) {
		SelectionType type = requireSelectionType(selection);
		if (type != SelectionType.HOME && type != SelectionType.DRAW && type != SelectionType.AWAY) {
			throw new SettlementException("European handicap selection must be HOME, DRAW or AWAY, not " + type);
		}
		BigDecimal marketLine = requireLine(market.line(), "European handicap market line is required");
		if (!isWholeNumber(marketLine)) {
			throw new SettlementException(
					"European handicap line must be a whole number, not " + marketLine.toPlainString());
		}
		BigDecimal adjustedDifference = BigDecimal.valueOf(score.homeGoals() - score.awayGoals()).add(marketLine);
		int comparison = adjustedDifference.compareTo(BigDecimal.ZERO);
		SelectionType winner;
		if (comparison > 0) {
			winner = SelectionType.HOME;
		}
		else if (comparison == 0) {
			winner = SelectionType.DRAW;
		}
		else {
			winner = SelectionType.AWAY;
		}
		return type == winner ? SettlementResult.WIN : SettlementResult.LOSS;
	}

	private SettlementResult settleDoubleChance(BettingMarket market, BettingSelection selection, MatchScore score) {
		if (market.line() != null) {
			throw new SettlementException("Double chance market line must be absent");
		}
		if (selection.line() != null) {
			throw new SettlementException("Double chance selection line must be absent");
		}
		SelectionType type = requireSelectionType(selection);
		if (type != SelectionType.HOME_OR_DRAW
				&& type != SelectionType.HOME_OR_AWAY
				&& type != SelectionType.DRAW_OR_AWAY) {
			throw new SettlementException("Double chance selection must be a two-way combination, not " + type);
		}
		SelectionType matchResult = matchResult(score);
		boolean wins = switch (type) {
			case HOME_OR_DRAW -> matchResult == SelectionType.HOME || matchResult == SelectionType.DRAW;
			case HOME_OR_AWAY -> matchResult == SelectionType.HOME || matchResult == SelectionType.AWAY;
			case DRAW_OR_AWAY -> matchResult == SelectionType.DRAW || matchResult == SelectionType.AWAY;
			case HOME, DRAW, AWAY -> throw new SettlementException(
					"Double chance selection must be a two-way combination, not " + type);
		};
		return wins ? SettlementResult.WIN : SettlementResult.LOSS;
	}

	private static SelectionType matchResult(MatchScore score) {
		int comparison = Integer.compare(score.homeGoals(), score.awayGoals());
		if (comparison > 0) {
			return SelectionType.HOME;
		}
		if (comparison == 0) {
			return SelectionType.DRAW;
		}
		return SelectionType.AWAY;
	}

	private static void requireAsianLineConsistency(
			SelectionType type, BigDecimal marketLine, BigDecimal selectionLine) {
		BigDecimal expected = type == SelectionType.HOME ? marketLine : marketLine.negate();
		if (selectionLine.compareTo(expected) != 0) {
			throw new SettlementException(
					"Asian handicap " + type + " line " + selectionLine.toPlainString()
							+ " is inconsistent with market line " + marketLine.toPlainString());
		}
	}

	private static void requireSupportedAsianIncrement(BigDecimal line) {
		if (!isMultipleOf(line, QUARTER)) {
			throw new SettlementException(
					"Asian handicap line must be a multiple of 0.25, not " + line.toPlainString());
		}
	}

	private static boolean isQuarterLine(BigDecimal line) {
		return !isMultipleOf(line, HALF);
	}

	private static boolean isWholeNumber(BigDecimal line) {
		return isMultipleOf(line, BigDecimal.ONE);
	}

	private static boolean isMultipleOf(BigDecimal value, BigDecimal increment) {
		return value.remainder(increment).compareTo(BigDecimal.ZERO) == 0;
	}

	private static boolean isPair(
			SettlementResult first, SettlementResult second, SettlementResult a, SettlementResult b) {
		return (first == a && second == b) || (first == b && second == a);
	}

	private static SelectionType requireSelectionType(BettingSelection selection) {
		if (selection.selectionType() == null) {
			throw new SettlementException("Selection type is required");
		}
		return selection.selectionType();
	}

	private static BigDecimal requireLine(BigDecimal line, String message) {
		if (line == null) {
			throw new SettlementException(message);
		}
		return line;
	}

}
