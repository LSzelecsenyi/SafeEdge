package com.safeedge.tippmix.manager;

import com.safeedge.event.domain.OfferSaveResult;
import java.time.Instant;

public record IngestionResult(
		Long eventId,
		int supportedMarketCount,
		int selectionCount,
		int snapshotCount,
		Instant capturedAt) {

	static IngestionResult from(OfferSaveResult saved) {
		return new IngestionResult(
				saved.eventId(),
				saved.supportedMarketCount(),
				saved.selectionCount(),
				saved.snapshotCount(),
				saved.capturedAt());
	}

}
