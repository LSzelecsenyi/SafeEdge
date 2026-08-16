package com.safeedge.historical.evaluation;

import com.safeedge.historical.domain.HistoricalDataException;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import java.math.BigDecimal;

/**
 * Selected-source historical AH quote. Missing configured sources are skipped,
 * never substituted.
 */
public record HistoricalAhQuoteSnapshot(
		String eventId,
		HistoricalQuoteSource quoteSource,
		BigDecimal homeHandicapLine,
		BigDecimal homeOdds,
		BigDecimal awayOdds) {

	public HistoricalAhQuoteSnapshot {
		if (eventId == null || eventId.isBlank()) {
			throw new HistoricalDataException("eventId is required");
		}
		if (quoteSource == null) {
			throw new HistoricalDataException("quoteSource is required");
		}
		if (homeHandicapLine == null || homeOdds == null || awayOdds == null) {
			throw new HistoricalDataException("AH line and both odds are required");
		}
	}
}
