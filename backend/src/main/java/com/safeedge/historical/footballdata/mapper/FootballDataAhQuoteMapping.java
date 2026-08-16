package com.safeedge.historical.footballdata.mapper;

import com.safeedge.historical.domain.HistoricalQuoteSource;

/**
 * Verified football-data.co.uk AH column pairings. Pinnacle line column {@code PAH}
 * is parsed when present; notes.txt lists {@code PAHH}/{@code PAHA} but not {@code PAH},
 * so a Pinnacle quote is skipped when the line column is absent (no fallback to {@code AHh}).
 */
public enum FootballDataAhQuoteMapping {
	BET365("B365AH", "B365AHH", "B365AHA", HistoricalQuoteSource.BET365),
	PINNACLE("PAH", "PAHH", "PAHA", HistoricalQuoteSource.PINNACLE),
	MARKET_MAX("AHh", "MaxAHH", "MaxAHA", HistoricalQuoteSource.MARKET_MAX),
	MARKET_AVERAGE("AHh", "AvgAHH", "AvgAHA", HistoricalQuoteSource.MARKET_AVERAGE);

	private final String lineColumn;
	private final String homeOddsColumn;
	private final String awayOddsColumn;
	private final HistoricalQuoteSource quoteSource;

	FootballDataAhQuoteMapping(
			String lineColumn,
			String homeOddsColumn,
			String awayOddsColumn,
			HistoricalQuoteSource quoteSource) {
		this.lineColumn = lineColumn;
		this.homeOddsColumn = homeOddsColumn;
		this.awayOddsColumn = awayOddsColumn;
		this.quoteSource = quoteSource;
	}

	public String lineColumn() {
		return lineColumn;
	}

	public String homeOddsColumn() {
		return homeOddsColumn;
	}

	public String awayOddsColumn() {
		return awayOddsColumn;
	}

	public HistoricalQuoteSource quoteSource() {
		return quoteSource;
	}

}
