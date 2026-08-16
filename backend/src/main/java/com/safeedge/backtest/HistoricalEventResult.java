package com.safeedge.backtest;

import com.safeedge.settlement.MatchScore;
import java.time.Instant;

/**
 * Final score for one event, available to the backtest only at
 * {@code settlementAt}. One result may settle every accepted bet on that event.
 */
public record HistoricalEventResult(String eventId, Instant settlementAt, MatchScore finalScore) {

	public HistoricalEventResult {
		eventId = requireText(eventId);
		if (settlementAt == null) {
			throw new BacktestException("settlementAt is required");
		}
		if (finalScore == null) {
			throw new BacktestException("finalScore is required");
		}
	}

	private static String requireText(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			throw new BacktestException("eventId is required");
		}
		return eventId;
	}

}
