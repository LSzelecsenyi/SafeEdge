package com.safeedge.historical.domain;

import java.util.List;

public record MappedHistoricalMatch(
		HistoricalMatchDraft match,
		List<HistoricalAhQuoteDraft> quotes,
		int quotesSkippedIncomplete,
		int quotesSkippedInvalidOdds,
		int quotesSkippedInvalidLine) {

	public MappedHistoricalMatch {
		if (match == null) {
			throw new HistoricalDataException("match is required");
		}
		if (quotes == null) {
			throw new HistoricalDataException("quotes are required");
		}
		quotes = List.copyOf(quotes);
		if (quotesSkippedIncomplete < 0 || quotesSkippedInvalidOdds < 0 || quotesSkippedInvalidLine < 0) {
			throw new HistoricalDataException("skip counts cannot be negative");
		}
	}

}
