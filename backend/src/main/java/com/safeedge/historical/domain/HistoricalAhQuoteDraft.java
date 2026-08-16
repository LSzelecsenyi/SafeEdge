package com.safeedge.historical.domain;

import com.safeedge.settlement.AsianHandicapLines;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Provider-independent pre-match Asian Handicap quote. {@code homeHandicapLine}
 * uses the SafeEdge home-side convention ({@code BettingMarket.line}).
 */
public record HistoricalAhQuoteDraft(
		HistoricalSource source,
		HistoricalQuoteSource quoteSource,
		BigDecimal homeHandicapLine,
		BigDecimal homeOdds,
		BigDecimal awayOdds,
		Instant observedAt,
		HistoricalObservationType observationType,
		String sourceLineColumn,
		String sourceHomeOddsColumn,
		String sourceAwayOddsColumn,
		String rawLineValue,
		String rawHomeOddsValue,
		String rawAwayOddsValue) {

	public HistoricalAhQuoteDraft {
		if (source == null) {
			throw new HistoricalDataException("source is required");
		}
		if (quoteSource == null) {
			throw new HistoricalDataException("quoteSource is required");
		}
		if (homeHandicapLine == null) {
			throw new HistoricalDataException("homeHandicapLine is required");
		}
		AsianHandicapLines.requireSupportedIncrement(homeHandicapLine);
		if (homeOdds == null || homeOdds.compareTo(BigDecimal.ONE) <= 0) {
			throw new HistoricalDataException("homeOdds must be greater than 1");
		}
		if (awayOdds == null || awayOdds.compareTo(BigDecimal.ONE) <= 0) {
			throw new HistoricalDataException("awayOdds must be greater than 1");
		}
		if (observationType == null) {
			throw new HistoricalDataException("observationType is required");
		}
		sourceLineColumn = requireText(sourceLineColumn, "sourceLineColumn");
		sourceHomeOddsColumn = requireText(sourceHomeOddsColumn, "sourceHomeOddsColumn");
		sourceAwayOddsColumn = requireText(sourceAwayOddsColumn, "sourceAwayOddsColumn");
		homeHandicapLine = homeHandicapLine.stripTrailingZeros();
		homeOdds = homeOdds.stripTrailingZeros();
		awayOdds = awayOdds.stripTrailingZeros();
	}

	public BigDecimal awayHandicapLine() {
		return AsianHandicapLines.awayLine(homeHandicapLine);
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new HistoricalDataException(name + " is required");
		}
		return value;
	}

}
