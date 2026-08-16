package com.safeedge.historical.evaluation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Synthetic chronological instants for football-data.co.uk rows that have a
 * match date but no verified kickoff or odds {@code observedAt}.
 *
 * <p>These are <strong>not</strong> kickoff times and <strong>not</strong> real
 * observation timestamps. They only preserve date order for
 * {@code BacktestEngine}: decision at 00:00 UTC on {@code matchDate}, settlement
 * at 00:00 UTC the following day. Same-date matches share {@code decisionAt} and
 * must not train each other.
 */
public final class HistoricalSyntheticChronology {

	private HistoricalSyntheticChronology() {
	}

	public static Instant decisionAt(LocalDate matchDate) {
		if (matchDate == null) {
			throw new IllegalArgumentException("matchDate is required");
		}
		return matchDate.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	public static Instant settlementAt(LocalDate matchDate) {
		if (matchDate == null) {
			throw new IllegalArgumentException("matchDate is required");
		}
		return matchDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
	}
}
