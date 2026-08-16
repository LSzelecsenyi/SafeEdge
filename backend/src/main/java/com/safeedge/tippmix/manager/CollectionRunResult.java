package com.safeedge.tippmix.manager;

import java.time.Instant;
import java.util.List;

public record CollectionRunResult(
		Instant startedAt,
		Instant completedAt,
		int pagesFetched,
		int eventsDiscovered,
		int eventsEligible,
		int eventsIngested,
		int eventsSkipped,
		int eventsFailed,
		int marketsPersisted,
		int selectionsPersisted,
		int snapshotsCreated,
		List<EventFailure> failures) {

	public record EventFailure(long eventId, String reason) {
	}
}
