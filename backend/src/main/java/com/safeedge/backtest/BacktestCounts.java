package com.safeedge.backtest;

public record BacktestCounts(
		int opportunitiesProcessed,
		int betsAccepted,
		int opportunitiesRejected,
		int opportunitiesSkippedByBetLimit,
		int opportunitiesSkippedByDrawdownPause,
		int wins,
		int halfWins,
		int pushes,
		int halfLosses,
		int losses) {

	public BacktestCounts {
		requireNonNegative(opportunitiesProcessed, "opportunitiesProcessed");
		requireNonNegative(betsAccepted, "betsAccepted");
		requireNonNegative(opportunitiesRejected, "opportunitiesRejected");
		requireNonNegative(opportunitiesSkippedByBetLimit, "opportunitiesSkippedByBetLimit");
		requireNonNegative(opportunitiesSkippedByDrawdownPause, "opportunitiesSkippedByDrawdownPause");
		requireNonNegative(wins, "wins");
		requireNonNegative(halfWins, "halfWins");
		requireNonNegative(pushes, "pushes");
		requireNonNegative(halfLosses, "halfLosses");
		requireNonNegative(losses, "losses");
	}

	private static void requireNonNegative(int value, String name) {
		if (value < 0) {
			throw new BacktestException(name + " cannot be negative");
		}
	}

}
