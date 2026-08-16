package com.safeedge.historical.diagnostics;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DrawdownPauseDiagnostics(
		boolean paused,
		Integer opportunityIndex,
		LocalDate pauseBettingDate,
		Instant pauseDecisionAt,
		int acceptedBetsBeforePause,
		BigDecimal activeBankrollAtPause,
		BigDecimal activeDrawdownAtPause,
		BigDecimal totalEquityAtPause,
		List<SettledBetSnapshot> lastSettledBetsBeforePause) {

	public DrawdownPauseDiagnostics {
		if (acceptedBetsBeforePause < 0) {
			throw new IllegalArgumentException("acceptedBetsBeforePause must be >= 0");
		}
		activeBankrollAtPause = strip(activeBankrollAtPause);
		activeDrawdownAtPause = strip(activeDrawdownAtPause);
		totalEquityAtPause = strip(totalEquityAtPause);
		lastSettledBetsBeforePause =
				List.copyOf(lastSettledBetsBeforePause == null ? List.of() : lastSettledBetsBeforePause);
	}

	private static BigDecimal strip(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros();
	}
}
