package com.safeedge.event.domain;

import java.time.Instant;

public record OfferSaveResult(
		Long eventId,
		int supportedMarketCount,
		int selectionCount,
		int snapshotCount,
		Instant capturedAt) {
}
