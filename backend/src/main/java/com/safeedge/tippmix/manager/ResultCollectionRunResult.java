package com.safeedge.tippmix.manager;

import java.time.Instant;
import java.util.List;

public record ResultCollectionRunResult(
		int eventsReceived,
		int finishedEvents,
		int knownEvents,
		int resultsInserted,
		int resultsUpdated,
		int resultsUnchanged,
		int eventsSkipped,
		int eventsFailed,
		Instant observedAt,
		List<EventFailure> failures) {

	public record EventFailure(Long eventId, String reason) {
	}

}
