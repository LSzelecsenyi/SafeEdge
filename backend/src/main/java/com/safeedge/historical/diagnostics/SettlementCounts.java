package com.safeedge.historical.diagnostics;

import com.safeedge.settlement.SettlementResult;

public record SettlementCounts(int win, int halfWin, int push, int halfLoss, int loss) {

	public SettlementCounts {
		if (win < 0 || halfWin < 0 || push < 0 || halfLoss < 0 || loss < 0) {
			throw new IllegalArgumentException("settlement counts must be >= 0");
		}
	}

	public static SettlementCounts empty() {
		return new SettlementCounts(0, 0, 0, 0, 0);
	}

	public int total() {
		return win + halfWin + push + halfLoss + loss;
	}

	SettlementCounts plus(SettlementResult result) {
		if (result == null) {
			throw new IllegalArgumentException("settlement result is required");
		}
		return switch (result) {
			case WIN -> new SettlementCounts(win + 1, halfWin, push, halfLoss, loss);
			case HALF_WIN -> new SettlementCounts(win, halfWin + 1, push, halfLoss, loss);
			case PUSH -> new SettlementCounts(win, halfWin, push + 1, halfLoss, loss);
			case HALF_LOSS -> new SettlementCounts(win, halfWin, push, halfLoss + 1, loss);
			case LOSS -> new SettlementCounts(win, halfWin, push, halfLoss, loss + 1);
		};
	}
}
