package com.safeedge.historical.evaluation;

import com.safeedge.bankroll.OwnerId;
import com.safeedge.event.domain.SelectionType;
import com.safeedge.historical.domain.HistoricalQuoteSource;
import com.safeedge.historical.domain.HistoricalSource;
import com.safeedge.historical.features.HistoricalMatchRecord;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic historical market/selection identity. Not Tippmix IDs and not
 * random UUIDs.
 */
public final class HistoricalWalkForwardIdentities {

	public static final String PROVIDER = HistoricalSource.FOOTBALL_DATA_UK.name();

	public static final OwnerId SIMULATION_OWNER =
			new OwnerId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

	private HistoricalWalkForwardIdentities() {
	}

	public static String eventId(HistoricalMatchRecord match) {
		if (match == null) {
			throw new IllegalArgumentException("match is required");
		}
		return match.source().name()
				+ ":"
				+ match.competition().name()
				+ ":"
				+ match.matchDate()
				+ ":"
				+ match.homeTeam()
				+ ":"
				+ match.awayTeam()
				+ ":"
				+ match.sourceRowNumber();
	}

	public static String marketId(String eventId, HistoricalQuoteSource quoteSource, BigDecimal homeHandicapLine) {
		if (eventId == null || eventId.isBlank()) {
			throw new IllegalArgumentException("eventId is required");
		}
		if (quoteSource == null) {
			throw new IllegalArgumentException("quoteSource is required");
		}
		if (homeHandicapLine == null) {
			throw new IllegalArgumentException("homeHandicapLine is required");
		}
		return eventId + ":" + quoteSource.name() + ":AH:" + homeHandicapLine.stripTrailingZeros().toPlainString();
	}

	public static String opportunityId(String marketId, SelectionType side) {
		if (marketId == null || marketId.isBlank()) {
			throw new IllegalArgumentException("marketId is required");
		}
		if (side == null) {
			throw new IllegalArgumentException("side is required");
		}
		return marketId + ":" + side.name();
	}
}
